/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package repositories

import java.time.LocalDateTime

import models.setup.messages.{CustomMessages, Summary}
import models.setup.{JourneyData, JourneySetup}
import org.scalatest.{Assertion, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{Json, OWrites}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.{DefaultWriteResult, WriteResult}
import reactivemongo.play.json.collection.JSONCollection
import uk.gov.hmrc.mongo.Awaiting
import uk.gov.hmrc.play.test.WithFakeApplication

class JourneyDataRepositoryISpec extends PlaySpec with WithFakeApplication with Awaiting with BeforeAndAfterEach {

  val repository: JourneyDataMongoRepository = fakeApplication.injector.instanceOf[JourneyDataRepo].store
  val mongo = fakeApplication.injector.instanceOf[ReactiveMongoComponent]

  class Setup {
    await(repository.drop)
    await(repository.ensureIndexes)

    def count: Int = await(repository.count)
    def insert(journeyData: JourneyData): WriteResult = await(repository.insert(journeyData))
    def fetchAll: List[JourneyData] = await(repository.findAll())
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    await(repository.drop)
  }

  def dateHasAdvanced(futureTime: LocalDateTime): Assertion = assert(futureTime.isAfter(now))

  val now = LocalDateTime.now

  "initialiseJourney" should {
    "successfully insert JourneyData into collection" in new Setup {
      val data = JourneyData(
        journeyId = "testId",
        sessionId = "testSessionId",
        redirectUrl = "test/url",
        customMessages = None,
        journeySetupDetails = JourneySetup(),
        lastUpdated = now
      )

      await(repository.initialiseJourney(data)) mustBe DefaultWriteResult(true, 1, Seq.empty, None, None, None)
      count mustBe 1
      fetchAll mustBe List(data)
    }
  }

  "getRedirectUrl" should {
    "successfully return a redirect url from an existing journey" in new Setup {
      val data = JourneyData(
        journeyId = "testId",
        sessionId = "testSessionId",
        redirectUrl = "test/url",
        customMessages = None,
        journeySetupDetails = JourneySetup(),
        lastUpdated = now
      )

      insert(data)
      await(repository.getRedirectUrl(data.journeyId, data.sessionId)) mustBe "test/url"
    }

    "throw an exception if the redirectUrl isn't in the journey data" in new Setup {
      case class JID(journeyId: String)
      implicit val writes: OWrites[JID] = Json.writes[JID]

      await(mongo.mongoConnector.db().collection[JSONCollection]("journey-data").insert(JID("testId")))

      a[NoSuchElementException] mustBe thrownBy(await(repository.getRedirectUrl("testId", "testSessionId")))
    }

    "return an exception if journey does not exist" in new Setup {
      a[NoSuchElementException] mustBe thrownBy(await(repository.getRedirectUrl("testWrongId", "testSessionId")))
    }
  }

  "getMessagesFor" should {
    "successfully return the messages for a given key" in new Setup {
      val data = JourneyData(
        journeyId = "testId",
        sessionId = "testSessionId",
        redirectUrl = "test/url",
        customMessages = Some(CustomMessages(
          summary = Some(Summary(
            heading = Some("testMessage1"),
            lead = Some("testMessage2")
          ))
        )),
        journeySetupDetails = JourneySetup(),
        lastUpdated = now
      )

      insert(data)
      await(repository.getMessagesFor[Summary](data.journeyId, data.sessionId, "summary")) mustBe Some(Summary(heading = Some("testMessage1"), lead = Some("testMessage2")))
    }

    "return None" when {
      "custom messages isn't defined" in new Setup {
        val data = JourneyData(
          journeyId = "testId",
          sessionId = "testSessionId",
          redirectUrl = "test/url",
          customMessages = None,
          journeySetupDetails = JourneySetup(),
          lastUpdated = now
        )

        insert(data)
        await(repository.getMessagesFor[Summary](data.journeyId, data.sessionId, "summary")) mustBe None
      }

      "custom messages is defined but summary isn't" in new Setup {
        val data = JourneyData(
          journeyId = "testId",
          sessionId = "testSessionId",
          redirectUrl = "test/url",
          customMessages = Some(CustomMessages(
            summary = None
          )),
          journeySetupDetails = JourneySetup(),
          lastUpdated = now
        )

        insert(data)
        await(repository.getMessagesFor[Summary](data.journeyId, data.sessionId, "summary")) mustBe None
      }
    }
  }

  "getSetupDetails" should {
    "successfully return a JourneySetupDetails" in new Setup {
      val data = JourneyData(
        journeyId = "testId",
        sessionId = "testSessionId",
        redirectUrl = "test/url",
        customMessages = Some(CustomMessages(
          summary = Some(Summary(
            heading = Some("testMessage1"),
            lead = Some("testMessage2")
          ))
        )),
        journeySetupDetails = JourneySetup(),
        lastUpdated = now
      )

      insert(data)
      await(repository.getSetupDetails(data.journeyId, data.sessionId)(JourneyData.journeySetupFormat)) mustBe JourneySetup()
    }

    "throw a IllegalStateException when a journey exists but journey setup details isn't defined" in new Setup {
      case class JID(journeyId: String)
      implicit val writes: OWrites[JID] = Json.writes[JID]

      await(mongo.mongoConnector.db().collection[JSONCollection]("journey-data").insert(JID("testId")))

      a[NoSuchElementException] mustBe thrownBy(await(repository.getSetupDetails("testId", "testSessionId")(JourneyData.journeySetupFormat)))
    }
  }
}
