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
import org.joda.time.DateTime
import org.scalatest.concurrent.Eventually
import play.api.libs.json.{JsValue, Json}
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
  val journey: String = Journey.QUERY_BUILDER
  val dataSet: String = Journey.HMRC_SIC_8

  val sicCodeCode = "12345678"
  val sicCode = SicCode(sicCodeCode, "Test sic code description")
  val sicCode2 = SicCode("87654321", "Another test sic code description")

  val searchResults = SearchResults("testQuery", 1, List(sicCode), List(Sector("A", "Example", 1)))
  val searchResults2 = SearchResults("testQuery", 1, List(sicCode2), List(Sector("B", "Alternative", 1)))

  val sicStoreNoChoices = SicStore(sessionId, journey, dataSet, Some(searchResults), None, dateTime)
  val sicStore1Choice = SicStore(sessionId, journey, dataSet, Some(searchResults), Some(List(sicCode)), dateTime)
  val sicStore2Choices = SicStore(sessionId, journey, dataSet, Some(searchResults2), Some(List(sicCode, sicCode2)), dateTime)

  "retrieveSicStore" should {

    "return a sic store when it is present" in new Setup {
      await(repository.insert(sicStoreNoChoices))
      await(repository.retrieveSicStore(sessionId)) shouldBe Some(sicStoreNoChoices)
    }

    "return nothing when the reg id is not present" in new Setup {
      await(repository.retrieveSicStore(sessionId)) shouldBe None
    }
  }

  "updateSearchResults" should {

    "not insert a new document when the journey has not been initialised" in new Setup {

      val updateSuccess: Boolean = repository.updateSearchResults(sessionId, searchResults)

      updateSuccess shouldBe true
      count shouldBe 0
    }

    "update a document with the new sic code if the document already exists for a given session id" in new Setup {

      val otherSearchResults = SearchResults("other query", 1, List(SicCode("87654321", "Another test sic code description")), List(Sector("A", "Fake", 1)))

      insert(sicStoreNoChoices)

      count shouldBe 1

      val updateSuccess: Boolean = repository.updateSearchResults(sessionId, otherSearchResults)

      updateSuccess shouldBe true
      count shouldBe 1

      val fetchedDocument: SicStore = fetchAll.head

      fetchedDocument.searchResults shouldBe Some(otherSearchResults)
      fetchedDocument.lastUpdated isAfter sicStoreNoChoices.lastUpdated shouldBe true
    }
  }

  "insertChoice" should {

    "insert a new sic code into a choices list with no other choices" in new Setup {
      insert(sicStoreNoChoices)
      count shouldBe 1

      val insertSuccess: Boolean = repository.insertChoice(sessionId, sicCodeCode)

      insertSuccess shouldBe true
      count shouldBe 1

      val fetchedDocument: SicStore = fetchAll.head

      fetchedDocument.choices shouldBe sicStore1Choice.choices
      fetchedDocument.lastUpdated isAfter sicStore1Choice.lastUpdated shouldBe true
    }

    "insert a new sic code choice into a choices list with another choice already there" in new Setup {

      val sicCodeToAdd = SicCode("67891234", "some description")

      val searchResults = SearchResults("testQuery", 1, List(sicCodeToAdd), List(Sector("A", "Fake", 1)))
      val sicStoreWithExistingChoice = SicStore(sessionId, journey, dataSet, Some(searchResults), Some(List(sicCode)), dateTime)

      insert(sicStoreWithExistingChoice)

      await(repository.insertChoice(sessionId, sicCodeToAdd.sicCode))

      val fetchedDocument: SicStore = fetchAll.head
      val sicStoreWith2Choices = SicStore(sessionId, journey, dataSet, Some(searchResults), Some(List(sicCode, sicCodeToAdd)), dateTime)

      fetchedDocument.choices shouldBe sicStoreWith2Choices.choices
      fetchedDocument.lastUpdated isAfter sicStoreWith2Choices.lastUpdated shouldBe true
    }

    "inserting the same choice twice will not duplicate it in the documents choices" in new Setup {
      insert(sicStore1Choice)

      await(repository.insertChoice(sessionId, sicCodeCode))

      val fetchedDocument: SicStore = fetchAll.head

      fetchedDocument.choices shouldBe sicStore1Choice.choices
      fetchedDocument.lastUpdated isAfter sicStore1Choice.lastUpdated shouldBe true
    }

    "return None if the document wasn't found" in new Setup {
      await(repository.insertChoice(sessionId, sicCode.sicCode)) shouldBe false
    }
  }

  "removeChoice" should {

    "remove a choice from the list of choices held in the document" in new Setup {
      await(repository.insert(sicStore2Choices))
      await(repository.removeChoice(sessionId, sicCode.sicCode))

      val fetchedDocument: SicStore = await(repository.retrieveSicStore(sessionId)).get

      fetchedDocument.choices shouldBe Some(List(sicCode2))
      fetchedDocument.lastUpdated isAfter sicStore2Choices.lastUpdated shouldBe true
    }

    "remove a choice from the choice list leaving no choices" in new Setup {
      await(repository.insert(sicStore1Choice))
      await(repository.removeChoice(sessionId, sicCode.sicCode))

      val fetchedDocument: SicStore = await(repository.retrieveSicStore(sessionId)).get

      fetchedDocument.choices shouldBe Some(List())
      fetchedDocument.lastUpdated isAfter sicStore1Choice.lastUpdated shouldBe true
    }

    "return None if unable to remove the choice from the list" in new Setup {
      await(repository.removeChoice(sessionId, sicCode.sicCode))
      await(repository.retrieveSicStore(sessionId)) shouldBe None
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

  "upsertJourney" should {
    "Update a journey is it already exists" in new Setup {
      await(repository.insert(sicStore2Choices))
      await(repository.upsertJourney(Journey(sessionId, Journey.QUERY_PARSER, dataSet)))

      val fetchedDocument: SicStore = await(repository.retrieveSicStore(sessionId)).get

      fetchedDocument.journey shouldBe Journey.QUERY_PARSER
    }


    "Create a journey if one doesn't exist" in new Setup {
      count shouldBe 0
      await(repository.upsertJourney(Journey(sessionId, Journey.QUERY_PARSER, dataSet)))

      val fetchedDocument: SicStore = await(repository.retrieveSicStore(sessionId)).get
      fetchedDocument.journey shouldBe Journey.QUERY_PARSER

    }
  }
}
