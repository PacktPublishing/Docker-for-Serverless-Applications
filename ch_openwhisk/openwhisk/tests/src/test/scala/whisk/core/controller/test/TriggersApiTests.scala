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

package whisk.core.controller.test

import java.time.Instant

import scala.language.postfixOps

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.StatusCodes._

import spray.json._
import spray.json.DefaultJsonProtocol._

import whisk.core.controller.WhiskTriggersApi
import whisk.core.entity._
import whisk.core.entity.WhiskRule
import whisk.core.entity.size._
import whisk.core.entity.test.OldWhiskTrigger
import whisk.http.ErrorResponse
import whisk.http.Messages

/**
 * Tests Trigger API.
 *
 * Unit tests of the controller service as a standalone component.
 * These tests exercise a fresh instance of the service object in memory -- these
 * tests do NOT communication with a whisk deployment.
 *
 *
 * @Idioglossia
 * "using Specification DSL to write unit tests, as in should, must, not, be"
 * "using Specs2RouteTest DSL to chain HTTP requests for unit testing, as in ~>"
 */
@RunWith(classOf[JUnitRunner])
class TriggersApiTests extends ControllerTestCommon with WhiskTriggersApi {

  /** Triggers API tests */
  behavior of "Triggers API"

  val creds = WhiskAuthHelpers.newIdentity()
  val namespace = EntityPath(creds.subject.asString)
  val collectionPath = s"/${EntityPath.DEFAULT}/${collection.path}"
  def aname() = MakeName.next("triggers_tests")
  val parametersLimit = Parameters.sizeLimit

  //// GET /triggers
  it should "list triggers by default/explicit namespace" in {
    implicit val tid = transid()
    val triggers = (1 to 2).map { i =>
      WhiskTrigger(namespace, aname(), Parameters("x", "b"))
    }.toList
    triggers foreach { put(entityStore, _) }
    waitOnView(entityStore, WhiskTrigger, namespace, 2)
    Get(s"$collectionPath") ~> Route.seal(routes(creds)) ~> check {
      status should be(OK)
      val response = responseAs[List[JsObject]]
      triggers.length should be(response.length)
      triggers forall { a =>
        response contains a.summaryAsJson
      } should be(true)
    }

    // it should "list triggers with explicit namespace owned by subject" in {
    Get(s"/$namespace/${collection.path}") ~> Route.seal(routes(creds)) ~> check {
      status should be(OK)
      val response = responseAs[List[JsObject]]
      triggers.length should be(response.length)
      triggers forall { a =>
        response contains a.summaryAsJson
      } should be(true)
    }

    // it should "reject list triggers with explicit namespace not owned by subject" in {
    val auser = WhiskAuthHelpers.newIdentity()
    Get(s"/$namespace/${collection.path}") ~> Route.seal(routes(auser)) ~> check {
      status should be(Forbidden)
    }
  }

  // ?docs disabled
  ignore should "list triggers by default namespace with full docs" in {
    implicit val tid = transid()
    val triggers = (1 to 2).map { i =>
      WhiskTrigger(namespace, aname(), Parameters("x", "b"))
    }.toList
    triggers foreach { put(entityStore, _) }
    waitOnView(entityStore, WhiskTrigger, namespace, 2)
    Get(s"$collectionPath?docs=true") ~> Route.seal(routes(creds)) ~> check {
      status should be(OK)
      val response = responseAs[List[WhiskTrigger]]
      triggers.length should be(response.length)
      triggers forall { a =>
        response contains a
      } should be(true)
    }
  }

  //// GET /triggers/name
  it should "get trigger by name in default/explicit namespace" in {
    implicit val tid = transid()
    val trigger = WhiskTrigger(namespace, aname(), Parameters("x", "b"))
    put(entityStore, trigger)
    Get(s"$collectionPath/${trigger.name}") ~> Route.seal(routes(creds)) ~> check {
      status should be(OK)
      val response = responseAs[WhiskTrigger]
      response should be(trigger.withoutRules)
    }

    // it should "get trigger by name in explicit namespace owned by subject" in
    Get(s"/$namespace/${collection.path}/${trigger.name}") ~> Route.seal(routes(creds)) ~> check {
      status should be(OK)
      val response = responseAs[WhiskTrigger]
      response should be(trigger.withoutRules)
    }

    // it should "reject get trigger by name in explicit namespace not owned by subject" in
    val auser = WhiskAuthHelpers.newIdentity()
    Get(s"/$namespace/${collection.path}/${trigger.name}") ~> Route.seal(routes(auser)) ~> check {
      status should be(Forbidden)
    }
  }

  it should "report Conflict if the name was of a different type" in {
    implicit val tid = transid()
    val rule = WhiskRule(
      namespace,
      aname(),
      FullyQualifiedEntityName(namespace, aname()),
      FullyQualifiedEntityName(namespace, aname()))
    put(entityStore, rule)
    Get(s"/$namespace/${collection.path}/${rule.name}") ~> Route.seal(routes(creds)) ~> check {
      status should be(Conflict)
    }
  }

  //// DEL /triggers/name
  it should "delete trigger by name" in {
    implicit val tid = transid()
    val trigger = WhiskTrigger(namespace, aname(), Parameters("x", "b"))
    put(entityStore, trigger)
    Delete(s"$collectionPath/${trigger.name}") ~> Route.seal(routes(creds)) ~> check {
      status should be(OK)
      val response = responseAs[WhiskTrigger]
      response should be(trigger.withoutRules)
    }
  }

  //// PUT /triggers/name
  it should "put should accept request with missing optional properties" in {
    implicit val tid = transid()
    val trigger = WhiskTrigger(namespace, aname())
    val content = WhiskTriggerPut()
    Put(s"$collectionPath/${trigger.name}", content) ~> Route.seal(routes(creds)) ~> check {
      deleteTrigger(trigger.docid)
      status should be(OK)
      val response = responseAs[WhiskTrigger]
      response should be(trigger.withoutRules)
    }
  }

  it should "put should accept request with valid feed parameter" in {
    implicit val tid = transid()
    val trigger = WhiskTrigger(namespace, aname(), annotations = Parameters(Parameters.Feed, "xyz"))
    val content = WhiskTriggerPut(annotations = Some(trigger.annotations))
    Put(s"$collectionPath/${trigger.name}", content) ~> Route.seal(routes(creds)) ~> check {
      deleteTrigger(trigger.docid)
      status should be(OK)
      val response = responseAs[WhiskTrigger]
      response should be(trigger.withoutRules)
    }
  }

  it should "put should reject request with undefined feed parameter" in {
    implicit val tid = transid()
    val trigger = WhiskTrigger(namespace, aname(), annotations = Parameters(Parameters.Feed, ""))
    val content = WhiskTriggerPut(annotations = Some(trigger.annotations))
    Put(s"$collectionPath/${trigger.name}", content) ~> Route.seal(routes(creds)) ~> check {
      status should be(BadRequest)
    }
  }

  it should "put should reject request with bad feed parameters" in {
    implicit val tid = transid()
    val trigger = WhiskTrigger(namespace, aname(), annotations = Parameters(Parameters.Feed, "a,b"))
    val content = WhiskTriggerPut(annotations = Some(trigger.annotations))
    Put(s"$collectionPath/${trigger.name}", content) ~> Route.seal(routes(creds)) ~> check {
      status should be(BadRequest)
    }
  }

  it should "reject activation with entity which is too big" in {
    implicit val tid = transid()
    val code = "a" * (allowedActivationEntitySize.toInt + 1)
    val content = s"""{"a":"$code"}""".stripMargin
    Post(s"$collectionPath/${aname()}", content.parseJson.asJsObject) ~> Route.seal(routes(creds)) ~> check {
      status should be(RequestEntityTooLarge)
      responseAs[String] should include {
        Messages.entityTooBig(
          SizeError(fieldDescriptionForSizeError, (content.length).B, allowedActivationEntitySize.B))
      }
    }
  }

  it should "reject create with parameters which are too big" in {
    implicit val tid = transid()
    val keys: List[Long] =
      List.range(Math.pow(10, 9) toLong, (parametersLimit.toBytes / 20 + Math.pow(10, 9) + 2) toLong)
    val parameters = keys map { key =>
      Parameters(key.toString, "a" * 10)
    } reduce (_ ++ _)
    val content = s"""{"parameters":$parameters}""".parseJson.asJsObject
    Put(s"$collectionPath/${aname()}", content) ~> Route.seal(routes(creds)) ~> check {
      status should be(RequestEntityTooLarge)
      responseAs[String] should include {
        Messages.entityTooBig(SizeError(WhiskEntity.paramsFieldName, parameters.size, Parameters.sizeLimit))
      }
    }
  }

  it should "reject create with annotations which are too big" in {
    implicit val tid = transid()
    val keys: List[Long] =
      List.range(Math.pow(10, 9) toLong, (parametersLimit.toBytes / 20 + Math.pow(10, 9) + 2) toLong)
    val annotations = keys map { key =>
      Parameters(key.toString, "a" * 10)
    } reduce (_ ++ _)
    val content = s"""{"annotations":$annotations}""".parseJson.asJsObject
    Put(s"$collectionPath/${aname()}", content) ~> Route.seal(routes(creds)) ~> check {
      status should be(RequestEntityTooLarge)
      responseAs[String] should include {
        Messages.entityTooBig(SizeError(WhiskEntity.annotationsFieldName, annotations.size, Parameters.sizeLimit))
      }
    }
  }

  it should "reject update with parameters which are too big" in {
    implicit val tid = transid()
    val trigger = WhiskTrigger(namespace, aname())
    val keys: List[Long] =
      List.range(Math.pow(10, 9) toLong, (parametersLimit.toBytes / 20 + Math.pow(10, 9) + 2) toLong)
    val parameters = keys map { key =>
      Parameters(key.toString, "a" * 10)
    } reduce (_ ++ _)
    val content = s"""{"parameters":$parameters}""".parseJson.asJsObject
    put(entityStore, trigger)
    Put(s"$collectionPath/${trigger.name}?overwrite=true", content) ~> Route.seal(routes(creds)) ~> check {
      status should be(RequestEntityTooLarge)
      responseAs[String] should include {
        Messages.entityTooBig(SizeError(WhiskEntity.paramsFieldName, parameters.size, Parameters.sizeLimit))
      }
    }
  }

  it should "put should accept update request with missing optional properties" in {
    implicit val tid = transid()
    val trigger = WhiskTrigger(namespace, aname(), Parameters("x", "b"))
    val content = WhiskTriggerPut()
    put(entityStore, trigger)
    Put(s"$collectionPath/${trigger.name}?overwrite=true", content) ~> Route.seal(routes(creds)) ~> check {
      deleteTrigger(trigger.docid)
      status should be(OK)
      val response = responseAs[WhiskTrigger]
      response should be(WhiskTrigger(
        trigger.namespace,
        trigger.name,
        trigger.parameters,
        version = trigger.version.upPatch).withoutRules)
    }
  }

  it should "put should reject update request for trigger with existing feed" in {
    implicit val tid = transid()
    val trigger = WhiskTrigger(namespace, aname(), annotations = Parameters(Parameters.Feed, "xyz"))
    val content = WhiskTriggerPut(annotations = Some(trigger.annotations))
    put(entityStore, trigger)
    Put(s"$collectionPath/${trigger.name}?overwrite=true", content) ~> Route.seal(routes(creds)) ~> check {
      status should be(BadRequest)
    }
  }

  it should "put should reject update request for trigger with new feed" in {
    implicit val tid = transid()
    val trigger = WhiskTrigger(namespace, aname())
    val content = WhiskTriggerPut(annotations = Some(Parameters(Parameters.Feed, "xyz")))
    put(entityStore, trigger)
    Put(s"$collectionPath/${trigger.name}?overwrite=true", content) ~> Route.seal(routes(creds)) ~> check {
      status should be(BadRequest)
    }
  }

  //// POST /triggers/name
  it should "fire a trigger" in {
    implicit val tid = transid()
    val trigger = WhiskTrigger(namespace, aname(), Parameters("x", "b"))
    val content = JsObject("xxx" -> "yyy".toJson)
    put(entityStore, trigger)
    Post(s"$collectionPath/${trigger.name}", content) ~> Route.seal(routes(creds)) ~> check {
      status should be(OK)
      val response = responseAs[JsObject]
      val JsString(id) = response.fields("activationId")
      val activationId = ActivationId(id)
      response.fields("activationId") should not be None

      val activationDoc = DocId(WhiskEntity.qualifiedName(namespace, activationId))
      val activation = get(activationStore, activationDoc, WhiskActivation, garbageCollect = false)
      del(activationStore, DocId(WhiskEntity.qualifiedName(namespace, activationId)), WhiskActivation)
      activation.end should be(Instant.EPOCH)
      activation.response.result should be(Some(content))
    }
  }

  it should "fire a trigger without args" in {
    implicit val tid = transid()
    val trigger = WhiskTrigger(namespace, aname(), Parameters("x", "b"))
    put(entityStore, trigger)
    Post(s"$collectionPath/${trigger.name}") ~> Route.seal(routes(creds)) ~> check {
      val response = responseAs[JsObject]
      val JsString(id) = response.fields("activationId")
      val activationId = ActivationId(id)
      del(activationStore, DocId(WhiskEntity.qualifiedName(namespace, activationId)), WhiskActivation)
      response.fields("activationId") should not be None
    }
  }

  //// invalid resource
  it should "reject invalid resource" in {
    implicit val tid = transid()
    val trigger = WhiskTrigger(namespace, aname())
    put(entityStore, trigger)
    Get(s"$collectionPath/${trigger.name}/bar") ~> Route.seal(routes(creds)) ~> check {
      status should be(NotFound)
    }
  }

  // migration path
  it should "be able to handle a trigger as of the old schema" in {
    implicit val tid = transid()
    val trigger = OldWhiskTrigger(namespace, aname())
    put(entityStore, trigger)
    Get(s"$collectionPath/${trigger.name}") ~> Route.seal(routes(creds)) ~> check {
      val response = responseAs[WhiskTrigger]
      status should be(OK)

      response should be(trigger.toWhiskTrigger)
    }
  }

  it should "report proper error when record is corrupted on delete" in {
    implicit val tid = transid()
    val entity = BadEntity(namespace, aname())
    put(entityStore, entity)

    Delete(s"$collectionPath/${entity.name}") ~> Route.seal(routes(creds)) ~> check {
      status should be(InternalServerError)
      responseAs[ErrorResponse].error shouldBe Messages.corruptedEntity
    }
  }

  it should "report proper error when record is corrupted on get" in {
    implicit val tid = transid()
    val entity = BadEntity(namespace, aname())
    put(entityStore, entity)

    Get(s"$collectionPath/${entity.name}") ~> Route.seal(routes(creds)) ~> check {
      status should be(InternalServerError)
      responseAs[ErrorResponse].error shouldBe Messages.corruptedEntity
    }
  }

  it should "report proper error when record is corrupted on put" in {
    implicit val tid = transid()
    val entity = BadEntity(namespace, aname())
    put(entityStore, entity)

    val content = WhiskTriggerPut()
    Put(s"$collectionPath/${entity.name}", content) ~> Route.seal(routes(creds)) ~> check {
      status should be(InternalServerError)
      responseAs[ErrorResponse].error shouldBe Messages.corruptedEntity
    }
  }
}
