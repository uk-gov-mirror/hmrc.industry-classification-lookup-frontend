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

import models.{SicStore, SicCode}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global

class SicStoreRepoISpec
  extends UnitSpec with MongoSpecSupport with BeforeAndAfterEach with ScalaFutures with Eventually with WithFakeApplication {

  class Setup {
    val repository = new SicStoreMongoRepository(mongo)
    await(repository.drop)
    await(repository.ensureIndexes)
  }

  val testRegID = "testRegID"
  val testSicCode = SicCode("12345678", "Test sic code description")
  val testSicCode2 = SicCode("87654321", "Another test sic code description")
  val testSicStore = SicStore(testRegID, testSicCode, None)
  val testSicStoreWithChoice = SicStore(testRegID, testSicCode, Some(List(testSicCode)))
  val testSicStoreWithChoices = SicStore(testRegID, testSicCode2, Some(List(testSicCode, testSicCode2)))

  "retrieveSicStore" should {
    "return a sic store when it is present" in new Setup {
      await(repository.insert(testSicStore))
      await(repository.retrieveSicStore(testRegID)) shouldBe Some(testSicStore)
    }

    "return nothing when the reg id is not present" in new Setup {
      await(repository.retrieveSicStore(testRegID)) shouldBe None
    }
  }

  "upsertSearchCode" should {

    "insert a new document when the reg id does not already exist in the collection" in new Setup {

      repository.upsertSearchCode(testRegID, testSicCode).get shouldBe testSicCode
      repository.retrieveSicStore(testRegID).get shouldBe testSicStore

    }

    "update a document with the new sic code if the document already exists" in new Setup {

      val otherTestCode = SicCode("87654321", "Another test sic code description")

      await(repository.insert(testSicStore))

      await(repository.upsertSearchCode(testRegID, otherTestCode))
      val retrievedSicStore : SicStore = await(repository.retrieveSicStore(testRegID)).get

      retrievedSicStore.search.sicCode shouldBe otherTestCode.sicCode
      retrievedSicStore.search.description shouldBe otherTestCode.description
    }

  }

  "insertChoice" should {

    "insert a new sic code subdocument into a choices list with no other choices" in new Setup {
      await(repository.insert(testSicStore))
      await(repository.insertChoice(testRegID)).get shouldBe testSicCode
      await(repository.retrieveSicStore(testRegID)).get shouldBe testSicStoreWithChoice
    }

    "insert a new sic code choice into a choices list with another choice already there" in new Setup {
      await(repository.insert(testSicStoreWithChoice))
      await(repository.upsertSearchCode(testRegID, testSicCode2))
      await(repository.insertChoice(testRegID)).get shouldBe testSicCode2
      await(repository.retrieveSicStore(testRegID)).get shouldBe testSicStoreWithChoices
    }

    "inserting the same choice twice will not duplicate it in the documents choices" in new Setup {
      await(repository.insert(testSicStoreWithChoice))
      await(repository.insertChoice(testRegID)).get shouldBe testSicCode
      await(repository.retrieveSicStore(testRegID)).get shouldBe testSicStoreWithChoice
    }

    "return None if the document wasn't found" in new Setup {
      await(repository.insertChoice(testRegID)) shouldBe None
    }
  }

  "removeChoice" should {

    "remove a choice from the list of choices held in the document" in new Setup {
      await(repository.insert(testSicStoreWithChoices))
      await(repository.removeChoice(testRegID, testSicCode.sicCode))
      await(repository.retrieveSicStore(testRegID)).get shouldBe SicStore(testRegID, testSicCode2, Some(List(testSicCode2)))
    }

    "return None if unable to remove the choice from the list" in new Setup {
      await(repository.insert(testSicStoreWithChoice))
      await(repository.removeChoice(testRegID, testSicCode.sicCode))
      await(repository.retrieveSicStore(testRegID)).get shouldBe SicStore(testRegID, testSicCode, Some(List()))
    }
  }

}
