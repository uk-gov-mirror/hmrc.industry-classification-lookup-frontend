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

import models.{SearchResults, SicCode, SicStore}
import reactivemongo.api.commands.WriteResult
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global

class SicStoreRepoISpec extends UnitSpec with MongoSpecSupport with WithFakeApplication {

  class Setup {
    val repository: SicStoreMongoRepository = fakeApplication.injector.instanceOf[SicStoreRepo].repo

    await(repository.drop)
    await(repository.ensureIndexes)

    def count: Int = await(repository.count)
    def insert(sicStore: SicStore): WriteResult = await(repository.insert(sicStore))
    def fetchAll: List[SicStore] = await(repository.findAll())
  }

  val sessionId = "session-id-12345"

  val sicCodeCode = "12345678"
  val sicCode = SicCode(sicCodeCode, "Test sic code description")
  val sicCode2 = SicCode("87654321", "Another test sic code description")

  val searchResults = SearchResults("testQuery", 1, List(sicCode))
  val searchResults2 = SearchResults("testQuery", 1, List(sicCode2))

  val sicStoreNoChoices = SicStore(sessionId, searchResults, None)
  val sicStore1Choice = SicStore(sessionId, searchResults, Some(List(sicCode)))
  val sicStore2Choices = SicStore(sessionId, searchResults2, Some(List(sicCode, sicCode2)))

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

    "insert a new document when one for a given session id does not already exist in the collection" in new Setup {

      val updateSuccess: Boolean = repository.updateSearchResults(sessionId, searchResults)

      updateSuccess shouldBe true
      count shouldBe 1

      val fetchedDocument: SicStore = fetchAll.head

      fetchedDocument shouldBe sicStoreNoChoices
    }

    "update a document with the new sic code if the document already exists for a given session id" in new Setup {

      val otherSearchResults = SearchResults("other query", 1, List(SicCode("87654321", "Another test sic code description")))

      insert(sicStoreNoChoices)

      count shouldBe 1

      val updateSuccess: Boolean = repository.updateSearchResults(sessionId, otherSearchResults)

      updateSuccess shouldBe true
      count shouldBe 1

      val fetchedDocument: SicStore = fetchAll.head

      fetchedDocument.searchResults shouldBe otherSearchResults
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

      fetchedDocument shouldBe sicStore1Choice
    }

    "insert a new sic code choice into a choices list with another choice already there" in new Setup {

      val sicCodeToAdd = SicCode("67891234", "some description")

      val searchResults = SearchResults("testQuery", 1, List(sicCodeToAdd))
      val sicStoreWithExistingChoice = SicStore(sessionId, searchResults, Some(List(sicCode)))

      insert(sicStoreWithExistingChoice)

      await(repository.insertChoice(sessionId, sicCodeToAdd.sicCode))

      val fetchedDocument: SicStore = fetchAll.head
      val sicStoreWith2Choices = SicStore(sessionId, searchResults, Some(List(sicCode, sicCodeToAdd)))

      fetchedDocument shouldBe sicStoreWith2Choices
    }

    "inserting the same choice twice will not duplicate it in the documents choices" in new Setup {
      insert(sicStore1Choice)

      await(repository.insertChoice(sessionId, sicCodeCode))

      val fetchedDocument: SicStore = fetchAll.head

      fetchedDocument shouldBe sicStore1Choice
    }

    "return None if the document wasn't found" in new Setup {
      await(repository.insertChoice(sessionId, sicCode.sicCode)) shouldBe false
    }
  }

  "removeChoice" should {

    "remove a choice from the list of choices held in the document" in new Setup {
      await(repository.insert(sicStore2Choices))
      await(repository.removeChoice(sessionId, sicCode.sicCode))
      await(repository.retrieveSicStore(sessionId)).get shouldBe SicStore(sessionId, searchResults2, Some(List(sicCode2)))
    }

    "remove a choice from the choice list leaving no choices" in new Setup {
      await(repository.insert(sicStore1Choice))
      await(repository.removeChoice(sessionId, sicCode.sicCode))
      await(repository.retrieveSicStore(sessionId)).get shouldBe SicStore(sessionId, searchResults, Some(List()))
    }

    "return None if unable to remove the choice from the list" in new Setup {
      await(repository.removeChoice(sessionId, sicCode.sicCode))
      await(repository.retrieveSicStore(sessionId)) shouldBe None
    }
  }

}
