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

package helpers

import play.api.libs.json.{JsObject, JsValue, Json}
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.Index
import repositories.TTLIndexing
import uk.gov.hmrc.mongo.{MongoSpecSupport, ReactiveRepository}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext
import scala.util.Random

trait MongoSpec extends UnitSpec with MongoSpecSupport with WithFakeApplication with RichReactiveRepository {
  implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global

  def generateOID: String = {
    val alpha = "abcdef123456789"
    (1 to 24).map(x => alpha(Random.nextInt.abs % alpha.length)).mkString
  }
}

trait RichReactiveRepository {
  self: UnitSpec =>

  import scala.language.implicitConversions

  implicit class MongoTTLOps[T](repo: ReactiveRepository[T, _] with TTLIndexing[T, _])(implicit ec: ExecutionContext) {
    def awaitCount: Int = repo.count
    def awaitInsert(e: T): WriteResult = repo.insert(e)
    def awaitDrop: Boolean = repo.drop
    def awaitEnsureIndexes: Seq[Boolean] = repo.ensureIndexes

    def createIndex(index: Index): WriteResult = repo.collection.indexesManager.create(index)
    def listIndexes: List[Index] = repo.collection.indexesManager.list()
    def dropIndexes: Int = repo.collection.indexesManager.dropAll()
    def findIndex(indexName: String): Option[Index] = listIndexes.find(_.eventualName == indexName)
  }

  class JsObjectHelpers(o: JsObject) {
    def pretty: String = Json.prettyPrint(o)
  }

  implicit def impJsObjectHelpers(o: JsObject): JsObjectHelpers = new JsObjectHelpers(o)
  implicit def toJsObject(v: JsValue): JsObject = v.as[JsObject]
}
