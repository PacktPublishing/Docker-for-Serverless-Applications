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

package system.basic

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import common.ActivationResult
import common.JsHelpers
import common.TestHelpers
import common.TestUtils
import common.BaseWsk
import common.Wsk
import common.WskProps
import common.WskTestHelpers
import spray.json._
import spray.json.DefaultJsonProtocol._
import spray.json.JsObject
import spray.json.pimpAny

@RunWith(classOf[JUnitRunner])
abstract class WskActionTests extends TestHelpers with WskTestHelpers with JsHelpers {

  implicit val wskprops = WskProps()
  val wsk: BaseWsk

  val testString = "this is a test"
  val testResult = JsObject("count" -> testString.split(" ").length.toJson)
  val guestNamespace = wskprops.namespace

  behavior of "Whisk actions"

  it should "invoke an action returning a promise" in withAssetCleaner(wskprops) { (wp, assetHelper) =>
    val name = "hello promise"
    assetHelper.withCleaner(wsk.action, name) { (action, _) =>
      action.create(name, Some(TestUtils.getTestActionFilename("helloPromise.js")))
    }

    val run = wsk.action.invoke(name)
    withActivation(wsk.activation, run) { activation =>
      activation.response.status shouldBe "success"
      activation.response.result shouldBe Some(JsObject("done" -> true.toJson))
      activation.logs.get.mkString(" ") shouldBe empty
    }
  }

  it should "invoke an action with a space in the name" in withAssetCleaner(wskprops) { (wp, assetHelper) =>
    val name = "hello Async"
    assetHelper.withCleaner(wsk.action, name) { (action, _) =>
      action.create(name, Some(TestUtils.getTestActionFilename("helloAsync.js")))
    }

    val run = wsk.action.invoke(name, Map("payload" -> testString.toJson))
    withActivation(wsk.activation, run) { activation =>
      activation.response.status shouldBe "success"
      activation.response.result shouldBe Some(testResult)
      activation.logs.get.mkString(" ") should include(testString)
    }
  }

  it should "pass parameters bound on creation-time to the action" in withAssetCleaner(wskprops) { (wp, assetHelper) =>
    val name = "printParams"
    val params = Map("param1" -> "test1", "param2" -> "test2")

    assetHelper.withCleaner(wsk.action, name) { (action, _) =>
      action.create(
        name,
        Some(TestUtils.getTestActionFilename("printParams.js")),
        parameters = params.mapValues(_.toJson))
    }

    val invokeParams = Map("payload" -> testString)
    val run = wsk.action.invoke(name, invokeParams.mapValues(_.toJson))
    withActivation(wsk.activation, run) { activation =>
      val logs = activation.logs.get.mkString(" ")

      (params ++ invokeParams).foreach {
        case (key, value) =>
          logs should include(s"params.$key: $value")
      }
    }
  }

  it should "copy an action and invoke it successfully" in withAssetCleaner(wskprops) { (wp, assetHelper) =>
    val name = "copied"
    val packageName = "samples"
    val actionName = "wordcount"
    val fullQualifiedName = s"/$guestNamespace/$packageName/$actionName"

    assetHelper.withCleaner(wsk.pkg, packageName) { (pkg, _) =>
      pkg.create(packageName, shared = Some(true))
    }

    assetHelper.withCleaner(wsk.action, fullQualifiedName) {
      val file = Some(TestUtils.getTestActionFilename("wc.js"))
      (action, _) =>
        action.create(fullQualifiedName, file)
    }

    assetHelper.withCleaner(wsk.action, name) { (action, _) =>
      action.create(name, Some(fullQualifiedName), Some("copy"))
    }

    val run = wsk.action.invoke(name, Map("payload" -> testString.toJson))
    withActivation(wsk.activation, run) { activation =>
      activation.response.status shouldBe "success"
      activation.response.result shouldBe Some(testResult)
      activation.logs.get.mkString(" ") should include(testString)
    }
  }

  it should "copy an action and ensure exec, parameters, and annotations copied" in withAssetCleaner(wskprops) {
    (wp, assetHelper) =>
      val origActionName = "origAction"
      val copiedActionName = "copiedAction"
      val params = Map("a" -> "A".toJson)
      val annots = Map("b" -> "B".toJson)

      assetHelper.withCleaner(wsk.action, origActionName) {
        val file = Some(TestUtils.getTestActionFilename("wc.js"))
        (action, _) =>
          action.create(origActionName, file, parameters = params, annotations = annots)
      }

      assetHelper.withCleaner(wsk.action, copiedActionName) { (action, _) =>
        action.create(copiedActionName, Some(origActionName), Some("copy"))
      }

      val copiedAction = getJSONFromResponse(wsk.action.get(copiedActionName).stdout, wsk.isInstanceOf[Wsk])
      val origAction = getJSONFromResponse(wsk.action.get(copiedActionName).stdout, wsk.isInstanceOf[Wsk])

      copiedAction.fields("annotations") shouldBe origAction.fields("annotations")
      copiedAction.fields("parameters") shouldBe origAction.fields("parameters")
      copiedAction.fields("exec") shouldBe origAction.fields("exec")
      copiedAction.fields("version") shouldBe JsString("0.0.1")
  }

  it should "add new parameters and annotations while copying an action" in withAssetCleaner(wskprops) {
    (wp, assetHelper) =>
      val origName = "origAction"
      val copiedName = "copiedAction"
      val origParams = Map("origParam1" -> "origParamValue1".toJson, "origParam2" -> 999.toJson)
      val copiedParams = Map("copiedParam1" -> "copiedParamValue1".toJson, "copiedParam2" -> 123.toJson)
      val origAnnots = Map("origAnnot1" -> "origAnnotValue1".toJson, "origAnnot2" -> true.toJson)
      val copiedAnnots = Map("copiedAnnot1" -> "copiedAnnotValue1".toJson, "copiedAnnot2" -> false.toJson)
      val resParams = Seq(
        JsObject("key" -> JsString("copiedParam1"), "value" -> JsString("copiedParamValue1")),
        JsObject("key" -> JsString("copiedParam2"), "value" -> JsNumber(123)),
        JsObject("key" -> JsString("origParam1"), "value" -> JsString("origParamValue1")),
        JsObject("key" -> JsString("origParam2"), "value" -> JsNumber(999)))
      val resAnnots = Seq(
        JsObject("key" -> JsString("origAnnot1"), "value" -> JsString("origAnnotValue1")),
        JsObject("key" -> JsString("copiedAnnot2"), "value" -> JsBoolean(false)),
        JsObject("key" -> JsString("copiedAnnot1"), "value" -> JsString("copiedAnnotValue1")),
        JsObject("key" -> JsString("origAnnot2"), "value" -> JsBoolean(true)),
        JsObject("key" -> JsString("exec"), "value" -> JsString("nodejs:6")))

      assetHelper.withCleaner(wsk.action, origName) {
        val file = Some(TestUtils.getTestActionFilename("echo.js"))
        (action, _) =>
          action.create(origName, file, parameters = origParams, annotations = origAnnots)
      }

      assetHelper.withCleaner(wsk.action, copiedName) { (action, _) =>
        println("created copied ")
        action.create(copiedName, Some(origName), Some("copy"), parameters = copiedParams, annotations = copiedAnnots)
      }

      val copiedAction = getJSONFromResponse(wsk.action.get(copiedName).stdout, wsk.isInstanceOf[Wsk])

      // CLI does not guarantee order of annotations and parameters so do a diff to compare the values
      copiedAction.fields("parameters").convertTo[Seq[JsObject]] diff resParams shouldBe List()
      copiedAction.fields("annotations").convertTo[Seq[JsObject]] diff resAnnots shouldBe List()
  }

  it should "recreate and invoke a new action with different code" in withAssetCleaner(wskprops) { (wp, assetHelper) =>
    val name = "recreatedAction"
    assetHelper.withCleaner(wsk.action, name, false) { (action, _) =>
      action.create(name, Some(TestUtils.getTestActionFilename("wc.js")))
    }

    val run1 = wsk.action.invoke(name, Map("payload" -> testString.toJson))
    withActivation(wsk.activation, run1) { activation =>
      activation.response.status shouldBe "success"
      activation.logs.get.mkString(" ") should include(s"The message '$testString' has")
    }

    wsk.action.delete(name)
    assetHelper.withCleaner(wsk.action, name) { (action, _) =>
      action.create(name, Some(TestUtils.getTestActionFilename("hello.js")))
    }

    val run2 = wsk.action.invoke(name, Map("payload" -> testString.toJson))
    withActivation(wsk.activation, run2) { activation =>
      activation.response.status shouldBe "success"
      activation.logs.get.mkString(" ") should include(s"hello, $testString")
    }
  }

  it should "fail to invoke an action with an empty file" in withAssetCleaner(wskprops) { (wp, assetHelper) =>
    val name = "empty"
    assetHelper.withCleaner(wsk.action, name) { (action, _) =>
      action.create(name, Some(TestUtils.getTestActionFilename("empty.js")))
    }
    val run = wsk.action.invoke(name)
    withActivation(wsk.activation, run) { activation =>
      activation.response.status shouldBe "action developer error"
      activation.response.result shouldBe Some(JsObject("error" -> "Missing main/no code to execute.".toJson))
    }
  }

  it should "create an action with an empty file" in withAssetCleaner(wskprops) { (wp, assetHelper) =>
    val name = "empty"
    assetHelper.withCleaner(wsk.action, name) { (action, _) =>
      action.create(name, Some(TestUtils.getTestActionFilename("empty.js")))
    }
    val rr = wsk.action.get(name)
    wsk.parseJsonString(rr.stdout).getFieldPath("exec", "code") shouldBe Some(JsString(""))
  }

  it should "blocking invoke of nested blocking actions" in withAssetCleaner(wskprops) { (wp, assetHelper) =>
    val name = "nestedBlockingAction"
    val child = "wc"

    assetHelper.withCleaner(wsk.action, name) { (action, _) =>
      action.create(name, Some(TestUtils.getTestActionFilename("wcbin.js")))
    }
    assetHelper.withCleaner(wsk.action, child) { (action, _) =>
      action.create(child, Some(TestUtils.getTestActionFilename("wc.js")))
    }

    val run = wsk.action.invoke(name, Map("payload" -> testString.toJson), blocking = true)
    val activation = wsk.parseJsonString(run.stdout).convertTo[ActivationResult]

    withClue(s"check failed for activation: $activation") {
      val wordCount = testString.split(" ").length
      activation.response.result.get shouldBe JsObject("binaryCount" -> s"${wordCount.toBinaryString} (base 2)".toJson)
    }
  }

  it should "blocking invoke an asynchronous action" in withAssetCleaner(wskprops) { (wp, assetHelper) =>
    val name = "helloAsync"
    assetHelper.withCleaner(wsk.action, name) { (action, _) =>
      action.create(name, Some(TestUtils.getTestActionFilename("helloAsync.js")))
    }

    val run = wsk.action.invoke(name, Map("payload" -> testString.toJson), blocking = true)
    val activation = wsk.parseJsonString(run.stdout).convertTo[ActivationResult]

    withClue(s"check failed for activation: $activation") {
      activation.response.status shouldBe "success"
      activation.response.result shouldBe Some(testResult)
      activation.logs shouldBe Some(List())
    }
  }

  it should "not be able to use 'ping' in an action" in withAssetCleaner(wskprops) { (wp, assetHelper) =>
    val name = "ping"
    assetHelper.withCleaner(wsk.action, name) { (action, _) =>
      action.create(name, Some(TestUtils.getTestActionFilename("ping.js")))
    }

    val run = wsk.action.invoke(name, Map("payload" -> "google.com".toJson))
    withActivation(wsk.activation, run) { activation =>
      activation.response.result shouldBe Some(
        JsObject("stderr" -> "ping: icmp open socket: Operation not permitted\n".toJson, "stdout" -> "".toJson))
    }
  }

  ignore should "support UTF-8 as input and output format" in withAssetCleaner(wskprops) { (wp, assetHelper) =>
    val name = "utf8Test"
    assetHelper.withCleaner(wsk.action, name) { (action, _) =>
      action.create(name, Some(TestUtils.getTestActionFilename("hello.js")))
    }

    val utf8 = "«ταБЬℓσö»: 1<2 & 4+1>³, now 20%€§$ off!"
    val run = wsk.action.invoke(name, Map("payload" -> utf8.toJson))
    withActivation(wsk.activation, run) { activation =>
      activation.response.status shouldBe "success"
      activation.logs.get.mkString(" ") should include(s"hello $utf8")
    }
  }
}
