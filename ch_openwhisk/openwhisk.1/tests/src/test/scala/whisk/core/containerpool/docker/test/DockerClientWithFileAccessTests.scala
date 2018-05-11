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

package whisk.core.containerpool.docker.test

import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.nio.charset.StandardCharsets

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration
import scala.language.reflectiveCalls // Needed to invoke publicIpAddressFromFile() method of structural dockerClientForIp extension

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterEach
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.scalatest.Matchers
import org.scalatest.fixture.{FlatSpec => FixtureFlatSpec}

import common.StreamLogging
import spray.json._
import spray.json.DefaultJsonProtocol._
import whisk.common.TransactionId
import whisk.core.containerpool.ContainerId
import whisk.core.containerpool.ContainerAddress
import whisk.core.containerpool.docker.DockerClientWithFileAccess

@RunWith(classOf[JUnitRunner])
class DockerClientWithFileAccessTestsIp extends FlatSpec with Matchers with StreamLogging with BeforeAndAfterEach {

  override def beforeEach = stream.reset()

  implicit val transid = TransactionId.testing
  val id = ContainerId("Id")

  def await[A](f: Future[A], timeout: FiniteDuration = 500.milliseconds) = Await.result(f, timeout)

  val dockerCommand = "docker"
  val networkInConfigFile = "networkConfig"
  val networkInDockerInspect = "networkInspect"
  val ipInConfigFile = ContainerAddress("10.0.0.1")
  val ipInDockerInspect = ContainerAddress("10.0.0.2")
  val dockerConfig =
    JsObject(
      "NetworkSettings" ->
        JsObject(
          "Networks" ->
            JsObject(networkInConfigFile ->
              JsObject("IPAddress" -> JsString(ipInConfigFile.host)))))

  /** Returns a DockerClient with mocked results */
  def dockerClient(execResult: Future[String] = Future.successful(ipInDockerInspect.host),
                   readResult: Future[JsObject] = Future.successful(dockerConfig)) =
    new DockerClientWithFileAccess()(global) {
      override val dockerCmd = Seq(dockerCommand)
      override def executeProcess(args: String*)(implicit ec: ExecutionContext) = execResult
      override def configFileContents(configFile: File) = readResult
      // Make protected ipAddressFromFile available for testing - requires reflectiveCalls
      def publicIpAddressFromFile(id: ContainerId, network: String): Future[ContainerAddress] =
        ipAddressFromFile(id, network)
    }

  behavior of "DockerClientWithFileAccess - ipAddressFromFile"

  it should "throw NoSuchElementException if specified network is not in configuration file" in {
    val dc = dockerClient()

    a[NoSuchElementException] should be thrownBy await(dc.publicIpAddressFromFile(id, "foo network"))
  }

  behavior of "DockerClientWithFileAccess - inspectIPAddress"

  it should "read from config file" in {
    val dc = dockerClient()

    await(dc.inspectIPAddress(id, networkInConfigFile)) shouldBe ipInConfigFile
    logLines.foreach { _ should not include (s"${dockerCommand} inspect") }
  }

  it should "fall back to 'docker inspect' if config file cannot be read" in {
    val dc = dockerClient(readResult = Future.failed(new RuntimeException()))

    await(dc.inspectIPAddress(id, networkInDockerInspect)) shouldBe ipInDockerInspect
    logLines.head should include(s"${dockerCommand} inspect")
  }

  it should "throw NoSuchElementException if specified network does not exist" in {
    val dc = dockerClient(execResult = Future.successful("<no value>"))

    a[NoSuchElementException] should be thrownBy await(dc.inspectIPAddress(id, "foo network"))
  }
}

@RunWith(classOf[JUnitRunner])
class DockerClientWithFileAccessTestsOom extends FlatSpec with Matchers with StreamLogging with BeforeAndAfterEach {
  override def beforeEach = stream.reset()

  implicit val transid = TransactionId.testing
  val id = ContainerId("Id")

  def await[A](f: Future[A], timeout: FiniteDuration = 500.milliseconds) = Await.result(f, timeout)

  def dockerClient(readResult: Future[JsObject]) =
    new DockerClientWithFileAccess()(global) {
      override val dockerCmd = Seq("docker")
      override def configFileContents(configFile: File) = readResult
    }

  def stateObject(oom: Boolean) = JsObject("State" -> JsObject("OOMKilled" -> oom.toJson))

  behavior of "DockerClientWithFileAccess - isOomKilled"

  it should "return the state of the container respectively" in {
    val dcTrue = dockerClient(Future.successful(stateObject(true)))
    await(dcTrue.isOomKilled(id)) shouldBe true

    val dcFalse = dockerClient(Future.successful(stateObject(false)))
    await(dcFalse.isOomKilled(id)) shouldBe false
  }

  it should "default to 'false' if the json structure is unparseable" in {
    val dc = dockerClient(Future.successful(JsObject()))
    await(dc.isOomKilled(id)) shouldBe false
  }
}

/**
 * The file access tests use fixtures (org.scalatest.fixture.FlatSpec) in contrast to
 * the IP address related tests. For this reason, the file access tests are in a separate
 * test suite.
 *
 * This test uses fixtures because they provide a good way for setup and cleanup(!) in a thread-safe
 * way such that tests could be run in parallel. In particular, this test suite creates
 * a temporary file for each test and cleans it up afterwards.
 */
@RunWith(classOf[JUnitRunner])
class DockerClientWithFileAccessTestsLogs
    extends FixtureFlatSpec
    with Matchers
    with StreamLogging
    with BeforeAndAfterEach {

  override def beforeEach = stream.reset()

  implicit val transid = TransactionId.testing

  behavior of "DockerClientWithFileAccess - rawContainerLogs"

  /** Returns a DockerClient with mocked results */
  def dockerClient(logFile: File) = new DockerClientWithFileAccess()(global) {
    override def containerLogFile(containerId: ContainerId) = logFile
  }

  def await[A](f: Future[A], timeout: FiniteDuration = 500.milliseconds) = Await.result(f, timeout)

  /** The fixture parameter must be of type FixtureParam. This is hard-wired in fixture suits. */
  case class FixtureParam(file: File, writer: FileWriter, docker: DockerClientWithFileAccess) {
    def writeLogFile(content: String) = {
      writer.write(content)
      writer.flush()
    }
  }

  /** This overridden method gets control for each test and actually invokes the test. */
  override def withFixture(test: OneArgTest) = {
    val file = File.createTempFile(this.getClass.getName, test.name.replaceAll("[^a-zA-Z0-9.-]", "_"))
    val writer = new FileWriter(file)
    val docker = dockerClient(file)

    val fixture = FixtureParam(file, writer, docker)

    try {
      super.withFixture(test.toNoArgTest(fixture))
    } finally {
      writer.close()
      file.delete()
    }
  }

  val containerId = ContainerId("Id")

  it should "tolerate an empty log file" in { fixture =>
    val logText = ""
    fixture.writeLogFile(logText)

    val buffer = await(fixture.docker.rawContainerLogs(containerId, fromPos = 0))
    val logContent = new String(buffer.array, buffer.arrayOffset, buffer.position, StandardCharsets.UTF_8)

    logContent shouldBe logText
    stream should have size 0
  }

  it should "read a full log file" in { fixture =>
    val logText = "text"
    fixture.writeLogFile(logText)

    val buffer = await(fixture.docker.rawContainerLogs(containerId, fromPos = 0))
    val logContent = new String(buffer.array, buffer.arrayOffset, buffer.position, StandardCharsets.UTF_8)

    logContent shouldBe logText
    stream should have size 0
  }

  it should "read a log file portion" in { fixture =>
    val logText =
      """Hey, dude-it'z true not sad
              |Take a thrash song and make it better
              |Admit it! Beatallica'z under your skin!
              |So now begin to be a shredder""".stripMargin
    val from = 66 // start at third line...
    val expectedText = logText.substring(from)

    fixture.writeLogFile(logText)

    val buffer = await(fixture.docker.rawContainerLogs(containerId, fromPos = from))
    val logContent = new String(buffer.array, buffer.arrayOffset, buffer.position, StandardCharsets.UTF_8)

    logContent shouldBe expectedText
    stream should have size 0
  }

  it should "provide an empty result on failure" in { fixture =>
    fixture.writer.close()
    fixture.file.delete()

    an[IOException] should be thrownBy await(fixture.docker.rawContainerLogs(containerId, fromPos = 0))
    stream should have size 0
  }
}
