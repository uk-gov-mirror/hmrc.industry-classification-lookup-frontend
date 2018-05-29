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
import models.setup.{Identifiers, JourneyData, JourneySetup}
import org.scalatest.{Assertion, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsResultException, Json, OWrites}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.{DefaultWriteResult, WriteResult}
import reactivemongo.play.json.collection.JSONCollection
import uk.gov.hmrc.mongo.Awaiting
import uk.gov.hmrc.play.test.WithFakeApplication

class JourneyDataRepositoryISpec extends PlaySpec with WithFakeApplication with Awaiting with BeforeAndAfterEach {

  class Setup {

    val repository: JourneyDataMongoRepository = fakeApplication.injector.instanceOf[JourneyDataRepo].store
    val mongo: ReactiveMongoComponent = fakeApplication.injector.instanceOf[ReactiveMongoComponent]

    await(repository.drop)
    await(repository.ensureIndexes)

    def count: Int = await(repository.count)
    def insert(journeyData: JourneyData): WriteResult = await(repository.insert(journeyData))
    def fetchAll: List[JourneyData] = await(repository.findAll())
  }

  def dateHasAdvanced(futureTime: LocalDateTime): Assertion = assert(futureTime.isAfter(now))

  val now = LocalDateTime.now

  "initialiseJourney" should {
    "successfully insert JourneyData into collection" in new Setup {
      val data = JourneyData(
        identifiers = Identifiers(
          journeyId = "testJourneyId",
          sessionId = "testSessionId"
        ),
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

  "retrieveJourneyData" should {
    val data = JourneyData(
      identifiers = Identifiers(
        journeyId = "testJourneyId",
        sessionId = "testSessionId"
      ),
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

    "successfully return a JourneyData" in new Setup {
      insert(data)
      await(repository.retrieveJourneyData(data.identifiers)).journeySetupDetails mustBe JourneySetup()
    }

    "throw a RuntimeException when the journey does not exist in the repo" in new Setup {
      intercept[RuntimeException](await(repository.retrieveJourneyData(data.identifiers)))
    }

    "throw a JsResultException when a journey exists but journey data isn't defined" in new Setup {
      case class JID(identifiers: Identifiers)
      implicit val writes: OWrites[JID] = Json.writes[JID]

      await(mongo.mongoConnector.db().collection[JSONCollection]("journey-data").insert(JID(Identifiers("testJourneyId", "testSessionId"))))

      a[JsResultException] mustBe thrownBy(await(repository.retrieveJourneyData(Identifiers(journeyId = "testJourneyId", sessionId = "testSessionId"))))
    }
  }
}
