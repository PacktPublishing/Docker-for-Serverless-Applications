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

import scala.concurrent.duration.DurationInt

import common.TestHelpers
import common.TestUtils
import common.TestUtils.ANY_ERROR_EXIT
import common.WskTestHelpers
import common.WskProps
import common.BaseWsk

import org.junit.runner.RunWith
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner

import spray.json.JsString

@RunWith(classOf[JUnitRunner])
abstract class WskBasicJavaTests extends TestHelpers with WskTestHelpers with Matchers {

  implicit val wskprops = WskProps()
  val wsk: BaseWsk
  val expectedDuration = 120.seconds
  val activationPollDuration = 60.seconds

  behavior of "Java Actions"

  /**
   * Test the Java "hello world" demo sequence
   */
  it should "Invoke a java action" in withAssetCleaner(wskprops) { (wp, assetHelper) =>
    val name = "helloJava"
    val file = Some(TestUtils.getTestActionFilename("helloJava.jar"))

    assetHelper.withCleaner(wsk.action, name) { (action, _) =>
      action.create(name, file, main = Some("hello.HelloJava"))
    }

    val start = System.currentTimeMillis()
    withActivation(wsk.activation, wsk.action.invoke(name), totalWait = activationPollDuration) {
      _.response.result.get.toString should include("Hello stranger!")
    }

    withActivation(
      wsk.activation,
      wsk.action.invoke(name, Map("name" -> JsString("Sir"))),
      totalWait = activationPollDuration) {
      _.response.result.get.toString should include("Hello Sir!")
    }

    withClue("Test duration exceeds expectation (ms)") {
      val duration = System.currentTimeMillis() - start
      duration should be <= expectedDuration.toMillis
    }
  }

  /*
   * Example from the docs.
   */
  it should "Invoke a Java action where main is in the default package" in withAssetCleaner(wskprops) {
    (wp, assetHelper) =>
      val name = "helloJavaDefaultPkg"
      val file = Some(TestUtils.getTestActionFilename("helloJavaDefaultPackage.jar"))

      assetHelper.withCleaner(wsk.action, name) { (action, _) =>
        action.create(name, file, main = Some("Hello"))
      }

      withActivation(wsk.activation, wsk.action.invoke(name, Map()), totalWait = activationPollDuration) {
        _.response.result.get.toString should include("Hello stranger!")
      }
  }

  it should "Ensure that Java actions cannot be created without a specified main method" in withAssetCleaner(wskprops) {
    (wp, assetHelper) =>
      val name = "helloJavaWithNoMainSpecified"
      val file = Some(TestUtils.getTestActionFilename("helloJava.jar"))

      val createResult = assetHelper.withCleaner(wsk.action, name, confirmDelete = false) { (action, _) =>
        action.create(name, file, expectedExitCode = ANY_ERROR_EXIT)
      }

      val output = s"${createResult.stdout}\n${createResult.stderr}"

      output should include("main")
  }
}
