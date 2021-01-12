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

import helpers.MongoSpec
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{Format, Json}
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONLong, BSONObjectID}
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.{ExecutionContext, Future}

class TTLIndexingISpec extends MongoSpec with Eventually with GuiceOneServerPerSuite with IntegrationPatience {

  val ttl = 12345789

  case class TestCaseClass(x: String, y: Int)

  object TestCaseClass {
    implicit val format: Format[TestCaseClass] = Json.format[TestCaseClass]
  }

  class TestTTLRepository extends ReactiveRepository[TestCaseClass, BSONObjectID](collectionName = "test-collection", mongo, TestCaseClass.format)
    with TTLIndexing[TestCaseClass, BSONObjectID] {

    implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

    override def ensureIndexes(implicit ec: ExecutionContext): Future[Seq[Boolean]] = {
      for {
        ttl <- ensureTTLIndexes
        i <- super.ensureIndexes
      } yield ttl ++ i
    }

    override val ttl: Long = 12345789
  }

  class Setup {
    val repo = new TestTTLRepository

    repo.awaitDrop
  }

  class SetupWithIndex(index: Index) {
    val repo = new TestTTLRepository

    repo.awaitDrop
    await(repo.collection.indexesManager.create(index))
  }

  "A TTLIndex" should {

    val ttlIndexName = "lastUpdatedIndex"
    val otherTTLValue = 999

    def buildTTLIndex(ttl: Int) = Index(
      key = Seq("lastUpdated" -> IndexType.Ascending),
      name = Some(ttlIndexName),
      options = BSONDocument("expireAfterSeconds" -> BSONLong(ttl))
    )

    "be applied whenever ensureIndexes is called with the correct expiration value" ignore new Setup {

      repo.listIndexes.size mustBe 0

      await(repo.ensureIndexes)

      repo.listIndexes.size mustBe 2

      val ttlIndex: Index = repo.findIndex(ttlIndexName).get
      ttlIndex.eventualName mustBe ttlIndexName
      ttlIndex.options.elements.head mustBe BSONDocument("expireAfterSeconds" -> BSONLong(ttl)).elements.head
    }

    "not change when ensureIndexes is called when the expiration value hasn't changed" ignore new Setup {

      repo.listIndexes.size mustBe 0

      await(repo.ensureIndexes)

      repo.listIndexes.size mustBe 2

      val ttlIndex: Index = repo.findIndex(ttlIndexName).get
      ttlIndex.eventualName mustBe ttlIndexName
      ttlIndex.options.elements.head mustBe BSONDocument("expireAfterSeconds" -> BSONLong(ttl)).elements.head

      await(repo.ensureIndexes)

      repo.listIndexes.size mustBe 2

      val ttlIndex2: Index = repo.findIndex(ttlIndexName).get
      ttlIndex2.eventualName mustBe ttlIndexName
      ttlIndex2.options.elements.head mustBe BSONDocument("expireAfterSeconds" -> BSONLong(ttl)).elements.head
    }

    "update the existing ttl index if the expiration value has changed" ignore new SetupWithIndex(buildTTLIndex(otherTTLValue)) {

      repo.listIndexes.size mustBe 2

      val ttlIndex: Index = repo.findIndex(ttlIndexName).get
      ttlIndex.eventualName mustBe ttlIndexName
      ttlIndex.options.elements.head mustBe BSONDocument("expireAfterSeconds" -> BSONLong(otherTTLValue)).elements.head

      await(repo.ensureIndexes)

      repo.listIndexes.size mustBe 2

      val ttlIndex2: Index = repo.findIndex(ttlIndexName).get
      ttlIndex2.eventualName mustBe ttlIndexName
      ttlIndex2.options.elements.head mustBe BSONDocument("expireAfterSeconds" -> BSONLong(ttl)).elements.head
    }
  }
}
