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
import common.TestHelpers
import common.TestUtils
import common.TestUtils.RunResult
import common.BaseWsk
import common.WskProps
import common.WskTestHelpers
import spray.json._
import spray.json.DefaultJsonProtocol._
import java.time.Instant

@RunWith(classOf[JUnitRunner])
abstract class WskRuleTests extends TestHelpers with WskTestHelpers {

  implicit val wskprops = WskProps()
  val wsk: BaseWsk
  val defaultAction = TestUtils.getTestActionFilename("wc.js")
  val secondAction = TestUtils.getTestActionFilename("hello.js")
  val testString = "this is a test"
  val testResult = JsObject("count" -> testString.split(" ").length.toJson)

  /**
   * Invoker clock skew can sometimes make it appear as if an action was invoked
   * _before_ the trigger was fired. The "fudge factor" below allows the test to look
   * for action activations that occur starting at most this amount of time before
   * the trigger was fired.
   */
  val activationTimeSkewFactorMs = 500

  /**
   * Sets up trigger -> rule -> action triplets. Deduplicates triggers and rules
   * and links it all up.
   *
   * @param rules Tuple3s containing
   *   (rule, trigger, (action name for created action, action name for the rule binding, actionFile))
   *   where the action name for the created action is allowed to differ from that used by the rule binding
   *   for cases that reference actions in a package binding.
   */
  def ruleSetup(rules: Seq[(String, String, (String, String, String))], assetHelper: AssetCleaner) = {
    val triggers = rules.map(_._2).distinct
    val actions = rules.map(_._3).distinct

    triggers.foreach { trigger =>
      assetHelper.withCleaner(wsk.trigger, trigger) { (trigger, name) =>
        trigger.create(name)
      }
    }

    actions.foreach {
      case (actionName, _, file) =>
        assetHelper.withCleaner(wsk.action, actionName) { (action, name) =>
          action.create(name, Some(file))
        }
    }

    rules.foreach {
      case (ruleName, triggerName, action) =>
        assetHelper.withCleaner(wsk.rule, ruleName) { (rule, name) =>
          rule.create(name, triggerName, action._2)
        }
    }
  }

  /**
   * Append the current timestamp in ms
   */
  def withTimestamp(text: String) = s"${text}-${System.currentTimeMillis}"

  behavior of "Whisk rules"

  it should "invoke the action attached on trigger fire, creating an activation for each entity including the cause" in withAssetCleaner(
    wskprops) { (wp, assetHelper) =>
    val ruleName = withTimestamp("r1to1")
    val triggerName = withTimestamp("t1to1")
    val actionName = withTimestamp("a1 to 1") // spaces in name intended for greater test coverage

    ruleSetup(Seq((ruleName, triggerName, (actionName, actionName, defaultAction))), assetHelper)

    val run = wsk.trigger.fire(triggerName, Map("payload" -> testString.toJson))

    withActivation(wsk.activation, run) { triggerActivation =>
      triggerActivation.cause shouldBe None

      withActivationsFromEntity(
        wsk.activation,
        ruleName,
        since = Some(triggerActivation.start.minusMillis(activationTimeSkewFactorMs))) {
        _.head.cause shouldBe Some(triggerActivation.activationId)
      }

      withActivationsFromEntity(
        wsk.activation,
        actionName,
        since = Some(triggerActivation.start.minusMillis(activationTimeSkewFactorMs))) { activationList =>
        activationList.head.response.result shouldBe Some(testResult)
        activationList.head.cause shouldBe None
      }
    }
  }

  it should "invoke the action from a package attached on trigger fire, creating an activation for each entity including the cause" in withAssetCleaner(
    wskprops) { (wp, assetHelper) =>
    val ruleName = withTimestamp("pr1to1")
    val triggerName = withTimestamp("pt1to1")
    val pkgName = withTimestamp("rule pkg") // spaces in name intended to test uri path encoding
    val actionName = withTimestamp("a1 to 1")
    val pkgActionName = s"$pkgName/$actionName"

    assetHelper.withCleaner(wsk.pkg, pkgName) { (pkg, name) =>
      pkg.create(name)
    }

    ruleSetup(Seq((ruleName, triggerName, (pkgActionName, pkgActionName, defaultAction))), assetHelper)

    val now = Instant.now
    val run = wsk.trigger.fire(triggerName, Map("payload" -> testString.toJson))

    withActivation(wsk.activation, run) { triggerActivation =>
      triggerActivation.cause shouldBe None

      withActivationsFromEntity(
        wsk.activation,
        ruleName,
        since = Some(triggerActivation.start.minusMillis(activationTimeSkewFactorMs))) {
        _.head.cause shouldBe Some(triggerActivation.activationId)
      }

      withActivationsFromEntity(
        wsk.activation,
        pkgActionName,
        since = Some(triggerActivation.start.minusMillis(activationTimeSkewFactorMs))) {
        _.head.response.result shouldBe Some(testResult)
      }
    }
  }

  it should "invoke the action from a package binding attached on trigger fire, creating an activation for each entity including the cause" in withAssetCleaner(
    wskprops) { (wp, assetHelper) =>
    val ruleName = withTimestamp("pr1to1")
    val triggerName = withTimestamp("pt1to1")
    val pkgName = withTimestamp("rule pkg") // spaces in name intended to test uri path encoding
    val pkgBindingName = withTimestamp("rule pkg binding")
    val actionName = withTimestamp("a1 to 1")
    val pkgActionName = s"$pkgName/$actionName"

    assetHelper.withCleaner(wsk.pkg, pkgName) { (pkg, name) =>
      pkg.create(name)
    }

    assetHelper.withCleaner(wsk.pkg, pkgBindingName) { (pkg, name) =>
      pkg.bind(pkgName, pkgBindingName)
    }

    ruleSetup(Seq((ruleName, triggerName, (pkgActionName, s"$pkgBindingName/$actionName", defaultAction))), assetHelper)

    val run = wsk.trigger.fire(triggerName, Map("payload" -> testString.toJson))

    withActivation(wsk.activation, run) { triggerActivation =>
      triggerActivation.cause shouldBe None

      withActivationsFromEntity(
        wsk.activation,
        ruleName,
        since = Some(triggerActivation.start.minusMillis(activationTimeSkewFactorMs))) {
        _.head.cause shouldBe Some(triggerActivation.activationId)
      }

      withActivationsFromEntity(
        wsk.activation,
        pkgActionName,
        since = Some(triggerActivation.start.minusMillis(activationTimeSkewFactorMs))) {
        _.head.response.result shouldBe Some(testResult)
      }
    }
  }

  it should "not activate an action if the rule is deleted when the trigger is fired" in withAssetCleaner(wskprops) {
    (wp, assetHelper) =>
      val ruleName = withTimestamp("ruleDelete")
      val triggerName = withTimestamp("ruleDeleteTrigger")
      val actionName = withTimestamp("ruleDeleteAction")

      assetHelper.withCleaner(wsk.trigger, triggerName) { (trigger, name) =>
        trigger.create(name)
      }
      assetHelper.withCleaner(wsk.action, actionName) { (action, name) =>
        action.create(name, Some(defaultAction))
      }
      assetHelper.withCleaner(wsk.rule, ruleName, false) { (rule, name) =>
        rule.create(name, triggerName, actionName)
      }

      val first = wsk.trigger.fire(triggerName, Map("payload" -> "bogus".toJson))
      wsk.rule.delete(ruleName)
      wsk.trigger.fire(triggerName, Map("payload" -> "bogus2".toJson))

      withActivation(wsk.activation, first) { activation =>
        // tries to find 2 activations for the action, should only find 1
        val activations = wsk.activation.pollFor(
          2,
          Some(actionName),
          since = Some(activation.start.minusMillis(activationTimeSkewFactorMs)),
          retries = 30)

        activations.length shouldBe 1
      }
  }

  it should "enable and disable a rule and check action is activated only when rule is enabled" in withAssetCleaner(
    wskprops) { (wp, assetHelper) =>
    val ruleName = withTimestamp("ruleDisable")
    val triggerName = withTimestamp("ruleDisableTrigger")
    val actionName = withTimestamp("ruleDisableAction")

    ruleSetup(Seq((ruleName, triggerName, (actionName, actionName, defaultAction))), assetHelper)

    val first = wsk.trigger.fire(triggerName, Map("payload" -> testString.toJson))
    wsk.rule.disable(ruleName)
    wsk.trigger.fire(triggerName, Map("payload" -> s"$testString with added words".toJson))
    wsk.rule.enable(ruleName)
    wsk.trigger.fire(triggerName, Map("payload" -> testString.toJson))

    withActivation(wsk.activation, first) { triggerActivation =>
      withActivationsFromEntity(
        wsk.activation,
        actionName,
        N = 2,
        since = Some(triggerActivation.start.minusMillis(activationTimeSkewFactorMs))) { activations =>
        val results = activations.map(_.response.result)
        results should contain theSameElementsAs Seq(Some(testResult), Some(testResult))
      }
    }
  }

  it should "be able to recreate a rule with the same name and match it successfully" in withAssetCleaner(wskprops) {
    (wp, assetHelper) =>
      val ruleName = withTimestamp("ruleRecreate")
      val triggerName1 = withTimestamp("ruleRecreateTrigger1")
      val triggerName2 = withTimestamp("ruleRecreateTrigger2")
      val actionName = withTimestamp("ruleRecreateAction")

      assetHelper.withCleaner(wsk.trigger, triggerName1) { (trigger, name) =>
        trigger.create(name)
      }
      assetHelper.withCleaner(wsk.action, actionName) { (action, name) =>
        action.create(name, Some(defaultAction))
      }
      assetHelper.withCleaner(wsk.rule, ruleName, false) { (rule, name) =>
        rule.create(name, triggerName1, actionName)
      }

      wsk.rule.delete(ruleName)

      assetHelper.withCleaner(wsk.trigger, triggerName2) { (trigger, name) =>
        trigger.create(name)
      }
      assetHelper.withCleaner(wsk.rule, ruleName) { (rule, name) =>
        rule.create(name, triggerName2, actionName)
      }

      val first = wsk.trigger.fire(triggerName2, Map("payload" -> testString.toJson))
      withActivation(wsk.activation, first) { triggerActivation =>
        withActivationsFromEntity(
          wsk.activation,
          actionName,
          since = Some(triggerActivation.start.minusMillis(activationTimeSkewFactorMs))) {
          _.head.response.result shouldBe Some(testResult)
        }
      }
  }

  it should "connect two triggers via rules to one action and activate it accordingly" in withAssetCleaner(wskprops) {
    (wp, assetHelper) =>
      val triggerName1 = withTimestamp("t2to1a")
      val triggerName2 = withTimestamp("t2to1b")
      val actionName = withTimestamp("a2to1")

      ruleSetup(
        Seq(
          ("r2to1a", triggerName1, (actionName, actionName, defaultAction)),
          ("r2to1b", triggerName2, (actionName, actionName, defaultAction))),
        assetHelper)

      val testPayloads = Seq("got three words", "got four words, period")

      val run = wsk.trigger.fire(triggerName1, Map("payload" -> testPayloads(0).toJson))
      wsk.trigger.fire(triggerName2, Map("payload" -> testPayloads(1).toJson))

      withActivation(wsk.activation, run) { triggerActivation =>
        withActivationsFromEntity(
          wsk.activation,
          actionName,
          N = 2,
          since = Some(triggerActivation.start.minusMillis(activationTimeSkewFactorMs))) { activations =>
          val results = activations.map(_.response.result)
          val expectedResults = testPayloads.map { payload =>
            Some(JsObject("count" -> payload.split(" ").length.toJson))
          }

          results should contain theSameElementsAs expectedResults
        }
      }
  }

  it should "connect one trigger to two different actions, invoking them both eventually" in withAssetCleaner(wskprops) {
    (wp, assetHelper) =>
      val triggerName = withTimestamp("t1to2")
      val actionName1 = withTimestamp("a1to2a")
      val actionName2 = withTimestamp("a1to2b")

      ruleSetup(
        Seq(
          ("r1to2a", triggerName, (actionName1, actionName1, defaultAction)),
          ("r1to2b", triggerName, (actionName2, actionName2, secondAction))),
        assetHelper)

      val run = wsk.trigger.fire(triggerName, Map("payload" -> testString.toJson))

      withActivation(wsk.activation, run) { triggerActivation =>
        withActivationsFromEntity(
          wsk.activation,
          actionName1,
          since = Some(triggerActivation.start.minusMillis(activationTimeSkewFactorMs))) {
          _.head.response.result shouldBe Some(testResult)
        }
        withActivationsFromEntity(
          wsk.activation,
          actionName2,
          since = Some(triggerActivation.start.minusMillis(activationTimeSkewFactorMs))) {
          _.head.logs.get.mkString(" ") should include(s"hello, $testString")
        }
      }
  }

  it should "connect two triggers to two different actions, invoking them both eventually" in withAssetCleaner(wskprops) {
    (wp, assetHelper) =>
      val triggerName1 = withTimestamp("t1to1a")
      val triggerName2 = withTimestamp("t1to1b")
      val actionName1 = withTimestamp("a1to1a")
      val actionName2 = withTimestamp("a1to1b")

      ruleSetup(
        Seq(
          ("r2to2a", triggerName1, (actionName1, actionName1, defaultAction)),
          ("r2to2b", triggerName1, (actionName2, actionName2, secondAction)),
          ("r2to2c", triggerName2, (actionName1, actionName1, defaultAction)),
          ("r2to2d", triggerName2, (actionName2, actionName2, secondAction))),
        assetHelper)

      val testPayloads = Seq("got three words", "got four words, period")
      val run = wsk.trigger.fire(triggerName1, Map("payload" -> testPayloads(0).toJson))
      wsk.trigger.fire(triggerName2, Map("payload" -> testPayloads(1).toJson))

      withActivation(wsk.activation, run) { triggerActivation =>
        withActivationsFromEntity(
          wsk.activation,
          actionName1,
          N = 2,
          since = Some(triggerActivation.start.minusMillis(activationTimeSkewFactorMs))) { activations =>
          val results = activations.map(_.response.result)
          val expectedResults = testPayloads.map { payload =>
            Some(JsObject("count" -> payload.split(" ").length.toJson))
          }

          results should contain theSameElementsAs expectedResults
        }
        withActivationsFromEntity(
          wsk.activation,
          actionName2,
          N = 2,
          since = Some(triggerActivation.start.minusMillis(activationTimeSkewFactorMs))) { activations =>
          // drops the leftmost 39 characters (timestamp + streamname)
          val logs = activations.map(_.logs.get.map(_.drop(39))).flatten
          val expectedLogs = testPayloads.map { payload =>
            s"hello, $payload!"
          }

          logs should contain theSameElementsAs expectedLogs
        }
      }
  }

  it should "disable a rule and check its status is displayed when listed" in withAssetCleaner(wskprops) {
    (wp, assetHelper) =>
      val ruleName = withTimestamp("ruleDisable")
      val ruleName2 = withTimestamp("ruleEnable")
      val triggerName = withTimestamp("ruleDisableTrigger")
      val actionName = withTimestamp("ruleDisableAction")

      ruleSetup(
        Seq(
          (ruleName, triggerName, (actionName, actionName, defaultAction)),
          (ruleName2, triggerName, (actionName, actionName, defaultAction))),
        assetHelper)

      wsk.rule.disable(ruleName)
      val ruleListResult = wsk.rule.list()
      verifyRuleList(ruleListResult, ruleName2, ruleName)
  }

  def verifyRuleList(ruleListResult: RunResult, ruleNameEnable: String, ruleName: String) = {
    val ruleList = ruleListResult.stdout
    val listOutput = ruleList.lines
    listOutput.find(_.contains(ruleNameEnable)).get should (include(ruleNameEnable) and include("active"))
    listOutput.find(_.contains(ruleName)).get should (include(ruleName) and include("inactive"))
    ruleList should not include ("Unknown")
  }
}
