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

package whisk.core.containerpool.docker

import java.nio.charset.StandardCharsets
import java.time.Instant

import akka.actor.ActorSystem
import spray.json._

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Failure
import whisk.common.Logging
import whisk.common.TransactionId
import whisk.core.containerpool._
import whisk.core.entity.ActivationResponse.{ConnectionError, MemoryExhausted}
import whisk.core.entity.ByteSize
import whisk.core.entity.size._

object DockerContainer {

  /**
   * Creates a container running on a docker daemon.
   *
   * @param transid transaction creating the container
   * @param image image to create the container from
   * @param userProvidedImage whether the image is provided by the user
   *     or is an OpenWhisk provided image
   * @param memory memorylimit of the container
   * @param cpuShares sharefactor for the container
   * @param environment environment variables to set on the container
   * @param network network to launch the container in
   * @param dnsServers list of dns servers to use in the container
   * @param name optional name for the container
   * @param useRunc use docker-runc to pause/unpause container?
   * @return a Future which either completes with a DockerContainer or one of two specific failures
   */
  def create(transid: TransactionId,
             image: String,
             userProvidedImage: Boolean = false,
             memory: ByteSize = 256.MB,
             cpuShares: Int = 0,
             environment: Map[String, String] = Map(),
             network: String = "bridge",
             dnsServers: Seq[String] = Seq(),
             name: Option[String] = None,
             useRunc: Boolean = true,
             dockerRunParameters: Map[String, Set[String]])(implicit docker: DockerApiWithFileAccess,
                                                            runc: RuncApi,
                                                            as: ActorSystem,
                                                            ec: ExecutionContext,
                                                            log: Logging): Future[DockerContainer] = {
    implicit val tid = transid

    val environmentArgs = environment.flatMap {
      case (key, value) => Seq("-e", s"$key=$value")
    }

    val params = dockerRunParameters.flatMap {
      case (key, valueList) => valueList.toList.flatMap(Seq(key, _))
    }

    val args = Seq(
      "--cpu-shares",
      cpuShares.toString,
      "--memory",
      s"${memory.toMB}m",
      "--memory-swap",
      s"${memory.toMB}m",
      "--network",
      network) ++
      environmentArgs ++
      name.map(n => Seq("--name", n)).getOrElse(Seq.empty) ++
      params
    val pulled = if (userProvidedImage) {
      docker.pull(image).recoverWith {
        case _ => Future.failed(BlackboxStartupError(s"Failed to pull container image '${image}'."))
      }
    } else Future.successful(())

    for {
      _ <- pulled
      id <- docker.run(image, args).recoverWith {
        case BrokenDockerContainer(brokenId, message) =>
          // Remove the broken container - but don't wait or check for the result.
          // If the removal fails, there is nothing we could do to recover from the recovery.
          docker.rm(brokenId)
          Future.failed(
            WhiskContainerStartupError(s"Failed to run container with image '${image}'. Removing broken container."))
        case _ =>
          Future.failed(WhiskContainerStartupError(s"Failed to run container with image '${image}'."))
      }
      ip <- docker.inspectIPAddress(id, network).recoverWith {
        // remove the container immediately if inspect failed as
        // we cannot recover that case automatically
        case _ =>
          docker.rm(id)
          Future.failed(WhiskContainerStartupError(s"Failed to obtain IP address of container '${id.asString}'."))
      }
    } yield new DockerContainer(id, ip, useRunc)
  }
}

/**
 * Represents a container as run by docker.
 *
 * This class contains OpenWhisk specific behavior and as such does not necessarily
 * use docker commands to achieve the effects needed.
 *
 * @constructor
 * @param id the id of the container
 * @param addr the ip of the container
 */
class DockerContainer(protected val id: ContainerId,
                      protected val addr: ContainerAddress,
                      protected val useRunc: Boolean)(implicit docker: DockerApiWithFileAccess,
                                                      runc: RuncApi,
                                                      as: ActorSystem,
                                                      protected val ec: ExecutionContext,
                                                      protected val logging: Logging)
    extends Container
    with DockerActionLogDriver {

  /** The last read-position in the log file */
  private var logFileOffset = 0L

  protected val waitForLogs: FiniteDuration = 2.seconds
  protected val waitForOomState: FiniteDuration = 2.seconds
  protected val filePollInterval: FiniteDuration = 100.milliseconds

  def suspend()(implicit transid: TransactionId): Future[Unit] =
    if (useRunc) { runc.pause(id) } else { docker.pause(id) }
  def resume()(implicit transid: TransactionId): Future[Unit] =
    if (useRunc) { runc.resume(id) } else { docker.unpause(id) }
  override def destroy()(implicit transid: TransactionId): Future[Unit] = {
    super.destroy()
    docker.rm(id)
  }

  /**
   * Was the container killed due to memory exhaustion?
   *
   * Retries because as all docker state-relevant operations, they won't
   * be reflected by the respective commands immediately but will take
   * some time to be propagated.
   *
   * @param retries number of retries to make
   * @return a Future indicating a memory exhaustion situation
   */
  private def isOomKilled(retries: Int = (waitForOomState / filePollInterval).toInt)(
    implicit transid: TransactionId): Future[Boolean] = {
    docker.isOomKilled(id)(TransactionId.invoker).flatMap { killed =>
      if (killed) Future.successful(true)
      else if (retries > 0) akka.pattern.after(filePollInterval, as.scheduler)(isOomKilled(retries - 1))
      else Future.successful(false)
    }
  }

  override protected def callContainer(path: String, body: JsObject, timeout: FiniteDuration, retry: Boolean = false)(
    implicit transid: TransactionId): Future[RunResult] = {
    val started = Instant.now()
    val http = httpConnection.getOrElse {
      val conn = new HttpUtils(s"${addr.host}:${addr.port}", timeout, 1.MB)
      httpConnection = Some(conn)
      conn
    }
    Future {
      http.post(path, body, retry)
    }.flatMap { response =>
      val finished = Instant.now()

      response.left
        .map {
          // Only check for memory exhaustion if there was a
          // terminal connection error.
          case error: ConnectionError =>
            isOomKilled().map {
              case true  => MemoryExhausted()
              case false => error
            }
          case other => Future.successful(other)
        }
        .fold(_.map(Left(_)), right => Future.successful(Right(right)))
        .map(res => RunResult(Interval(started, finished), res))
    }
  }

  /**
   * Obtains the container's stdout and stderr output and converts it to our own JSON format.
   * At the moment, this is done by reading the internal Docker log file for the container.
   * Said file is written by Docker's JSON log driver and has a "well-known" location and name.
   *
   * For warm containers, the container log file already holds output from
   * previous activations that have to be skipped. For this reason, a starting position
   * is kept and updated upon each invocation.
   *
   * If asked, check for sentinel markers - but exclude the identified markers from
   * the result returned from this method.
   *
   * Only parses and returns as much logs as fit in the passed log limit.
   * Even if the log limit is exceeded, advance the starting position for the next invocation
   * behind the bytes most recently read - but don't actively read any more until sentinel
   * markers have been found.
   *
   * @param limit the limit to apply to the log size
   * @param waitForSentinel determines if the processor should wait for a sentinel to appear
   *
   * @return a vector of Strings with log lines in our own JSON format
   */
  def logs(limit: ByteSize, waitForSentinel: Boolean)(implicit transid: TransactionId): Future[Vector[String]] = {

    def readLogs(retries: Int): Future[Vector[String]] = {
      docker
        .rawContainerLogs(id, logFileOffset)
        .flatMap { rawLogBytes =>
          val rawLog =
            new String(rawLogBytes.array, rawLogBytes.arrayOffset, rawLogBytes.position, StandardCharsets.UTF_8)
          val (isComplete, isTruncated, formattedLogs) = processJsonDriverLogContents(rawLog, waitForSentinel, limit)

          if (retries > 0 && !isComplete && !isTruncated) {
            logging.info(this, s"log cursor advanced but missing sentinel, trying $retries more times")
            akka.pattern.after(filePollInterval, as.scheduler)(readLogs(retries - 1))
          } else {
            logFileOffset += rawLogBytes.position - rawLogBytes.arrayOffset
            Future.successful(formattedLogs)
          }
        }
        .andThen {
          case Failure(e) =>
            logging.error(this, s"Failed to obtain logs of ${id.asString}: ${e.getClass} - ${e.getMessage}")
        }
    }

    readLogs((waitForLogs / filePollInterval).toInt)
  }

}
