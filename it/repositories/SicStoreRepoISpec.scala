/*
 * Copyright 2017 HM Revenue & Customs
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

import models._
import models.setup.{JourneyData, JourneySetup}
import org.joda.time.DateTime
import org.scalatest.concurrent.Eventually
import play.api.libs.json.{JsValue, Json, Writes}
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.Index
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global

class SicStoreRepoISpec extends UnitSpec with MongoSpecSupport with WithFakeApplication with Eventually {

  class Setup {
    val repository: SicStoreMongoRepository = fakeApplication.injector.instanceOf[SicStoreRepo].repo

    await(repository.drop)
    await(repository.ensureIndexes)

    def count: Int = await(repository.count)
    def insert(sicStore: SicStore): WriteResult = await(repository.insert(sicStore))
    def fetchAll: List[SicStore] = await(repository.findAll())
  }

  val dateTime: DateTime = DateTime.parse("2017-06-15T10:06:28.434Z")
  val now: JsValue = Json.toJson(dateTime)(ReactiveMongoFormats.dateTimeWrite)

  val sessionId = "session-id-12345"
  val journeyId = "testJourneyId"
  val journey: String = JourneyData.QUERY_BUILDER
  val dataSet: String = JourneyData.ONS

  val sicCodeCode = "12345"
  val sicCode = SicCode(sicCodeCode, "Test sic code description")
  val sicCodeGroup = SicCodeChoice(sicCode, Nil)
  val sicCode2 = SicCode("87654", "Another test sic code description")
  val sicCodeGroup2 = SicCodeChoice(sicCode2, Nil)

  val searchResults = SearchResults("testQuery", 1, List(sicCode), List(Sector("A", "Example", 1)))
  val searchResults2 = SearchResults("testQuery", 1, List(sicCode2), List(Sector("B", "Alternative", 1)))
  def generateSicStoreWithIndexes(indexes: List[String]) =
    SicStore(sessionId, Some(searchResults), Some(List(sicCodeGroup.copy(indexes = indexes))), dateTime)
  val sicStoreNoChoices = SicStore(journeyId, Some(searchResults), None, dateTime)
  val sicStore1Choice = SicStore(journeyId, Some(searchResults), Some(List(sicCodeGroup)), dateTime)
  val sicStore2Choices = SicStore(journeyId, Some(searchResults2), Some(List(sicCodeGroup, sicCodeGroup2)), dateTime)
  val journeySetup     = JourneySetup(dataSet, Some(false), Some(true), 50, None)
  "retrieveSicStore" should {

    "return a sic store when it is present" in new Setup {
      await(repository.insert(sicStoreNoChoices))
      await(repository.retrieveSicStore(journeyId)) shouldBe Some(sicStoreNoChoices)
    }

    "return nothing when the reg id is not present" in new Setup {
      await(repository.retrieveSicStore(journeyId)) shouldBe None
    }
  }

  "updateSearchResults" should {

    "insert a new document when one does not exist" in new Setup {
      count shouldBe 0
      val updateSuccess: Boolean = repository.upsertSearchResults(journeyId, searchResults.copy(currentSector = Some(Sector("A", "Fake Sector", 1))))

      updateSuccess shouldBe true
      count shouldBe 1
    }

    "update a document with the new sic code if the document already exists for a given session id" in new Setup {

      val otherSearchResults = SearchResults("other query", 1, List(SicCode("87654", "Another test sic code description")), List(Sector("A", "Fake", 1)))

      insert(sicStoreNoChoices)

      count shouldBe 1

      val updateSuccess: Boolean = repository.upsertSearchResults(journeyId, otherSearchResults)

      updateSuccess shouldBe true
      count shouldBe 1

      val fetchedDocument: SicStore = fetchAll.head

      fetchedDocument.searchResults shouldBe Some(otherSearchResults)
      fetchedDocument.lastUpdated isAfter sicStoreNoChoices.lastUpdated shouldBe true
    }
  }

  "insertChoice" should {

    "insert new sic codes into empty sic store" in new Setup {
      count shouldBe 0

      val sicCode2 = SicCodeChoice(SicCode("67891", "some description"))
      val insertSuccess: Boolean = repository.insertChoices(journeyId, List(sicCodeGroup, sicCode2))

      insertSuccess shouldBe true
      count shouldBe 1

      val fetchedDocument: SicStore = fetchAll.head

      fetchedDocument.choices shouldBe Some(List(sicCodeGroup, sicCode2))
    }

    "insert a new sic code into a choices list with no other choices" in new Setup {
      insert(sicStoreNoChoices)
      count shouldBe 1

      val insertSuccess: Boolean = repository.insertChoices(journeyId, List(sicCodeGroup))

      insertSuccess shouldBe true
      count shouldBe 1

      val fetchedDocument: SicStore = fetchAll.head

      fetchedDocument.choices shouldBe sicStore1Choice.choices
      fetchedDocument.lastUpdated isAfter sicStore1Choice.lastUpdated shouldBe true
    }

    "insert a new sic code choice into a choices list with another choice already there" in new Setup {

      val sicCodeToAdd = SicCode("67891", "some description")

      val searchResults = SearchResults("testQuery", 1, List(sicCodeToAdd), List(Sector("A", "Fake", 1)))
      val sicStoreWithExistingChoice = SicStore(journeyId, Some(searchResults), Some(List(sicCodeGroup)), dateTime)

      insert(sicStoreWithExistingChoice)

      await(repository.insertChoices(journeyId, List(SicCodeChoice(sicCodeToAdd))))

      val fetchedDocument: SicStore = fetchAll.head
      val sicStoreWith2Choices = SicStore(journeyId, Some(searchResults), Some(List(sicCodeGroup, SicCodeChoice(sicCodeToAdd))), dateTime)

      fetchedDocument.choices shouldBe sicStoreWith2Choices.choices
      fetchedDocument.lastUpdated isAfter sicStoreWith2Choices.lastUpdated shouldBe true
    }

    "inserting the same choice twice will not duplicate it in the documents choices" in new Setup {
      insert(sicStore1Choice)

      await(repository.insertChoices(journeyId, List(sicCodeGroup)))

      val fetchedDocument: SicStore = fetchAll.head

      fetchedDocument.choices shouldBe sicStore1Choice.choices
      fetchedDocument.lastUpdated isAfter sicStore1Choice.lastUpdated shouldBe true
    }

    "inserting the same sicCode with different indexes will not duplicate it in the documents choices" in new Setup {
      insert(sicStore1Choice)

      await(repository.insertChoices(journeyId, List(SicCodeChoice(sicCode, List("some description")))))

      val fetchedDocument: SicStore = fetchAll.head

      fetchedDocument.choices shouldBe generateSicStoreWithIndexes(List("some description")).choices
      fetchedDocument.lastUpdated isAfter sicStore1Choice.lastUpdated shouldBe true
    }

    "inserting the same sicCode with different indexes will update the correct choice + add a new choice" in new Setup {
      val sicCode3 = SicCode("67891", "some other description")
      val sicCodeGroup3 = SicCodeChoice(sicCode3, List("some index 1"))
      val sicCode4 = SicCode("11122", "test desc")
      val sicCodeGroup4 = SicCodeChoice(sicCode4, List("index whatever test"))
      val sicStore3Choices = SicStore(journeyId, Some(searchResults2), Some(List(sicCodeGroup, sicCodeGroup2, sicCodeGroup3)), dateTime)
      insert(sicStore3Choices)

      val expected = Some(List(sicCodeGroup.copy(indexes = List("some description")), sicCodeGroup2, sicCodeGroup3.copy(indexes = List("some index 1", "new test other desc")), sicCodeGroup4))

      await(repository.insertChoices(journeyId, List(sicCodeGroup.copy(indexes = List("some description")), SicCodeChoice(sicCode3, List("new test other desc")), sicCodeGroup4)))

      val fetchedDocument: SicStore = fetchAll.head

      fetchedDocument.choices shouldBe expected
      fetchedDocument.lastUpdated isAfter sicStore3Choices.lastUpdated shouldBe true
    }
  }

  "removeChoice" should {

    "remove a choice from the list of choices held in the document" in new Setup {
      await(repository.insert(sicStore2Choices))
      await(repository.removeChoice(journeyId, sicCode.sicCode))

      val fetchedDocument: SicStore = await(repository.retrieveSicStore(journeyId)).get

      fetchedDocument.choices shouldBe Some(List(sicCodeGroup2))
      fetchedDocument.lastUpdated isAfter sicStore2Choices.lastUpdated shouldBe true
    }

    "remove a choice from the choice list leaving no choices" in new Setup {
      await(repository.insert(sicStore1Choice))
      await(repository.removeChoice(journeyId, sicCode.sicCode))

      val fetchedDocument: SicStore = await(repository.retrieveSicStore(journeyId)).get

      fetchedDocument.choices shouldBe Some(List())
      fetchedDocument.lastUpdated isAfter sicStore1Choice.lastUpdated shouldBe true
    }

    "return None if unable to remove the choice from the list" in new Setup {
      await(repository.removeChoice(journeyId, sicCode.sicCode))
      await(repository.retrieveSicStore(journeyId)) shouldBe None
    }
  }

  "Indexes" should {
    "should exist" in new Setup {
      val indexList: List[Index] = await(repository.collection.indexesManager.list())

      val containsIndexes: Boolean = eventually {
        indexList.map(_.name).filter(_.isDefined).contains(Some("lastUpdatedIndex")) &&
          indexList.map(_.name).filter(_.isDefined).contains(Some("_id_"))
      }

      containsIndexes shouldBe true
    }
  }
}
