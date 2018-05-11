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
import org.scalatest.Matchers
import scala.concurrent.duration.DurationInt
import spray.json._
import spray.json.DefaultJsonProtocol.StringJsonFormat
import common.JsHelpers
import common.TestHelpers
import common.TestUtils
import common.BaseWsk
import common.WskProps
import common.WskTestHelpers
import common.WhiskProperties

@RunWith(classOf[JUnitRunner])
abstract class WskBasicPythonTests extends TestHelpers with WskTestHelpers with Matchers with JsHelpers {

  implicit val wskprops = WskProps()
  val wsk: BaseWsk

  behavior of "Native Python Action"

  it should "invoke an action and get the result" in withAssetCleaner(wskprops) { (wp, assetHelper) =>
    val name = "basicInvoke"
    assetHelper.withCleaner(wsk.action, name) { (action, _) =>
      action.create(name, Some(TestUtils.getTestActionFilename("hello.py")))
    }

    withActivation(wsk.activation, wsk.action.invoke(name, Map("name" -> "Prince".toJson))) {
      _.response.result.get.toString should include("Prince")
    }
  }

  it should "invoke an action with a non-default entry point" in withAssetCleaner(wskprops) { (wp, assetHelper) =>
    val name = "nonDefaultEntryPoint"
    assetHelper.withCleaner(wsk.action, name) { (action, _) =>
      action.create(name, Some(TestUtils.getTestActionFilename("niam.py")), main = Some("niam"))
    }

    withActivation(wsk.activation, wsk.action.invoke(name, Map())) {
      _.response.result.get.fields.get("greetings") should be(Some(JsString("Hello from a non-standard entrypoint.")))
    }
  }

  it should "invoke an action from a zip file with a non-default entry point" in withAssetCleaner(wskprops) {
    (wp, assetHelper) =>
      val name = "pythonZipWithNonDefaultEntryPoint"
      assetHelper.withCleaner(wsk.action, name) { (action, _) =>
        action.create(
          name,
          Some(TestUtils.getTestActionFilename("python.zip")),
          main = Some("niam"),
          kind = Some("python:default"))
      }

      withActivation(wsk.activation, wsk.action.invoke(name, Map("name" -> "Prince".toJson))) {
        _.response.result.get shouldBe JsObject("greeting" -> JsString("Hello Prince!"))
      }
  }

  Seq("python:2", "python:3").foreach { kind =>
    it should s"invoke a $kind action" in withAssetCleaner(wskprops) { (wp, assetHelper) =>
      val name = s"${kind.replace(":", "")}-action"
      assetHelper.withCleaner(wsk.action, name) { (action, _) =>
        action.create(name, Some(TestUtils.getTestActionFilename("pythonVersion.py")), kind = Some(kind))
      }

      withActivation(wsk.activation, wsk.action.invoke(name)) {
        _.response.result.get shouldBe JsObject("version" -> JsNumber(kind.takeRight(1).toInt))
      }
    }
  }

  it should "invoke an action and confirm expected environment is defined" in withAssetCleaner(wskprops) {
    (wp, assetHelper) =>
      val name = "stdenv"
      assetHelper.withCleaner(wsk.action, name) { (action, _) =>
        action.create(name, Some(TestUtils.getTestActionFilename("stdenv.py")))
      }

      withActivation(wsk.activation, wsk.action.invoke(name)) { activation =>
        val result = activation.response.result.get
        result.fields.get("error") shouldBe empty
        result.fields.get("auth") shouldBe Some(
          JsString(WhiskProperties.readAuthKey(WhiskProperties.getAuthFileForTesting)))
        result.fields.get("edge").toString.trim should not be empty
      }
  }

  it should "invoke an invalid action and get error back" in withAssetCleaner(wskprops) { (wp, assetHelper) =>
    val name = "bad code"
    assetHelper.withCleaner(wsk.action, name) { (action, _) =>
      action.create(name, Some(TestUtils.getTestActionFilename("malformed.py")))
    }

    withActivation(wsk.activation, wsk.action.invoke(name)) { activation =>
      activation.response.result.get.fields.get("error") shouldBe Some(
        JsString("The action failed to generate or locate a binary. See logs for details."))
      activation.logs.get.mkString("\n") should { not include ("pythonaction.py") and not include ("flask") }
    }
  }

  behavior of "Python virtualenv"

  Seq(("python2_virtualenv.zip", "python:2"), ("python3_virtualenv.zip", "python:3")).foreach {
    case (filename, kind) =>
      it should s"invoke a zipped $kind action with virtualenv package" in withAssetCleaner(wskprops) {
        (wp, assetHelper) =>
          val name = filename
          val zippedPythonAction = Some(TestUtils.getTestActionFilename(filename))

          assetHelper.withCleaner(wsk.action, name) { (action, _) =>
            action.create(name, zippedPythonAction, kind = Some(kind))
          }

          withActivation(wsk.activation, wsk.action.invoke(name), totalWait = 120.seconds) { activation =>
            val response = activation.response
            response.result.get.fields.get("error") shouldBe empty
            response.result.get.fields.get("Networkinfo: ") shouldBe defined
          }
      }
  }
}
