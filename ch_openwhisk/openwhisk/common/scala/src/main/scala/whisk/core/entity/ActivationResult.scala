/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package whisk.core.entity

import scala.util.Try

import akka.http.scaladsl.model.StatusCodes.OK

import spray.json._
import spray.json.DefaultJsonProtocol

import whisk.common.Logging
import whisk.http.Messages._

protected[core] case class ActivationResponse private (val statusCode: Int, val result: Option[JsValue]) {

  def toJsonObject = ActivationResponse.serdes.write(this).asJsObject

  // Used when presenting to end-users, to hide the statusCode (which is an implementation detail),
  // and to provide a convenience boolean "success" field.
  def toExtendedJson: JsObject = {
    val baseFields = this.toJsonObject.fields
    JsObject(
      (baseFields - "statusCode") ++ Seq(
        "success" -> JsBoolean(this.isSuccess),
        "status" -> JsString(ActivationResponse.messageForCode(statusCode))))

  }

  def isSuccess = statusCode == ActivationResponse.Success
  def isApplicationError = statusCode == ActivationResponse.ApplicationError
  def isContainerError = statusCode == ActivationResponse.ContainerError
  def isWhiskError = statusCode == ActivationResponse.WhiskError
  def withoutResult = ActivationResponse(statusCode, None)

  override def toString = toJsonObject.compactPrint
}

protected[core] object ActivationResponse extends DefaultJsonProtocol {
  /* The field name that is universally recognized as the marker of an error, from the application or otherwise. */
  val ERROR_FIELD: String = "error"

  val Success = 0 // action ran successfully and produced a result
  val ApplicationError = 1 // action ran but there was an error and it was handled
  val ContainerError = 2 // action ran but failed to handle an error, or action did not run and failed to initialize
  val WhiskError = 3 // internal system error

  protected[core] def messageForCode(code: Int) = {
    require(code >= 0 && code <= 3)
    code match {
      case 0 => "success"
      case 1 => "application error"
      case 2 => "action developer error"
      case 3 => "whisk internal error"
    }
  }

  private def error(code: Int, errorValue: JsValue) = {
    require(code == ApplicationError || code == ContainerError || code == WhiskError)
    ActivationResponse(code, Some(JsObject(ERROR_FIELD -> errorValue)))
  }

  protected[core] def success(result: Option[JsValue] = None) = ActivationResponse(Success, result)

  protected[core] def applicationError(errorValue: JsValue) = error(ApplicationError, errorValue)
  protected[core] def applicationError(errorMsg: String) = error(ApplicationError, JsString(errorMsg))
  protected[core] def containerError(errorValue: JsValue) = error(ContainerError, errorValue)
  protected[core] def containerError(errorMsg: String) = error(ContainerError, JsString(errorMsg))
  protected[core] def whiskError(errorValue: JsValue) = error(WhiskError, errorValue)
  protected[core] def whiskError(errorMsg: String) = error(WhiskError, JsString(errorMsg))

  /**
   * Returns an ActivationResponse that is used as a placeholder for payload
   * Used as a feed for starting a sequence.
   * NOTE: the code is application error (since this response could be used as a response for the sequence
   * if the payload contains an error)
   */
  protected[core] def payloadPlaceholder(payload: Option[JsObject]) = ActivationResponse(ApplicationError, payload)

  /**
   * Class of errors for invoker-container communication.
   */
  protected[core] sealed trait ContainerConnectionError
  protected[core] sealed trait ContainerHttpError extends ContainerConnectionError
  protected[core] case class ConnectionError(t: Throwable) extends ContainerHttpError
  protected[core] case class NoResponseReceived() extends ContainerHttpError
  protected[core] case class Timeout() extends ContainerHttpError

  protected[core] case class MemoryExhausted() extends ContainerConnectionError

  /**
   * @param statusCode the container HTTP response code (e.g., 200 OK)
   * @param entity the entity response as string
   * @param truncated either None to indicate complete entity or Some(actual length, max allowed)
   */
  protected[core] case class ContainerResponse(statusCode: Int,
                                               entity: String,
                                               truncated: Option[(ByteSize, ByteSize)]) {

    /** true iff status code is OK (HTTP 200 status code), anything else is considered an error. **/
    val okStatus = statusCode == OK.intValue
    val ok = okStatus && truncated.isEmpty
    override def toString = {
      val base = if (okStatus) "ok" else "not ok"
      val rest = truncated.map(e => s", truncated ${e.toString}").getOrElse("")
      base + rest
    }
  }

  protected[core] object ContainerResponse {
    def apply(okStatus: Boolean, entity: String, truncated: Option[(ByteSize, ByteSize)] = None): ContainerResponse = {
      ContainerResponse(if (okStatus) OK.intValue else 500, entity, truncated)
    }
  }

  /**
   * Interprets response from container after initialization. This method is only called when the initialization failed.
   *
   * @param response an either a container error or container response (HTTP Status Code, HTTP response bytes as String)
   * @return appropriate ActivationResponse representing initialization error
   */
  protected[core] def processInitResponseContent(response: Either[ContainerConnectionError, ContainerResponse],
                                                 logger: Logging): ActivationResponse = {
    require(
      response.isLeft || !response.right.exists(_.ok),
      s"should not interpret init response when status code is OK")
    response match {
      case Right(ContainerResponse(code, str, truncated)) =>
        truncated match {
          case None =>
            Try { str.parseJson.asJsObject } match {
              case scala.util.Success(result @ JsObject(fields)) =>
                // If the response is a JSON object container an error field, accept it as the response error.
                val errorOpt = fields.get(ERROR_FIELD)
                val errorContent = errorOpt getOrElse invalidInitResponse(str).toJson
                containerError(errorContent)
              case _ =>
                containerError(invalidInitResponse(str))
            }

          case Some((length, maxlength)) =>
            containerError(truncatedResponse(str, length, maxlength))
        }

      case Left(_: MemoryExhausted) =>
        containerError(memoryExhausted)

      case Left(e) =>
        // This indicates a terminal failure in the container (it exited prematurely).
        containerError(abnormalInitialization)
    }
  }

  /**
   * Interprets response from container after running the action. This method is only called when the initialization succeeded.
   *
   * @param response an Option (HTTP Status Code, HTTP response bytes as String)
   * @return appropriate ActivationResponse representing run result
   */
  protected[core] def processRunResponseContent(response: Either[ContainerConnectionError, ContainerResponse],
                                                logger: Logging): ActivationResponse = {
    response match {
      case Right(res @ ContainerResponse(_, str, truncated)) =>
        truncated match {
          case None =>
            Try { str.parseJson.asJsObject } match {
              case scala.util.Success(result @ JsObject(fields)) =>
                // If the response is a JSON object container an error field, accept it as the response error.
                val errorOpt = fields.get(ERROR_FIELD)

                if (res.okStatus) {
                  errorOpt map { error =>
                    applicationError(error)
                  } getOrElse {
                    // The happy path.
                    success(Some(result))
                  }
                } else {
                  // Any non-200 code is treated as a container failure. We still need to check whether
                  // there was a useful error message in there.
                  val errorContent = errorOpt getOrElse invalidRunResponse(str).toJson
                  containerError(errorContent)
                }

              case scala.util.Success(notAnObj) =>
                // This should affect only blackbox containers, since our own containers should already test for that.
                containerError(invalidRunResponse(str))

              case scala.util.Failure(t) =>
                // This should affect only blackbox containers, since our own containers should already test for that.
                logger.warn(this, s"response did not json parse: '$str' led to $t")
                containerError(invalidRunResponse(str))
            }

          case Some((length, maxlength)) =>
            containerError(truncatedResponse(str, length, maxlength))
        }

      case Left(_: MemoryExhausted) =>
        containerError(memoryExhausted)

      case Left(e) =>
        // This indicates a terminal failure in the container (it exited prematurely).
        containerError(abnormalRun)
    }
  }

  protected[core] implicit val serdes = jsonFormat2(ActivationResponse.apply)
}
