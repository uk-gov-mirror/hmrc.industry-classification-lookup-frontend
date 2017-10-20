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

import javax.inject.{Inject, Singleton}

import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.DB
import reactivemongo.bson.{BSONDocument, BSONObjectID, BSONString}
import repositories.models.{SicCode, SicStore}
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


@Singleton
class SicStoreRepo @Inject()(mongo: ReactiveMongoComponent) {
  val repo = new SicStoreMongoRepository(mongo.mongoConnector.db)
}

trait SicStoreRepository {
  def upsertSearchCode(registrationID: String, searchResult: SicCode) : Future[Option[SicCode]]
  def retrieveSicStore(registrationID: String) : Future[Option[SicStore]]
  def insertChoice(registrationID: String) : Future[Option[SicCode]]
  def removeChoice(registrationID: String, choice: String) : Future[Option[SicCode]]
}

class SicStoreMongoRepository(mongo: () => DB)
  extends ReactiveRepository[SicStore, BSONObjectID]("sic-store", mongo, SicStore.format)
  with SicStoreRepository {

  private[repositories] def registrationIDSelector(registrationID: String): BSONDocument = BSONDocument(
    "registrationID" -> BSONString(registrationID)
  )

  override def retrieveSicStore(registrationID: String) : Future[Option[SicStore]] = {
    collection.find(registrationIDSelector(registrationID)).one[SicStore]
  }

  override def upsertSearchCode(registrationID: String, searchResult: SicCode) : Future[Option[SicCode]] = {
    val selector = registrationIDSelector(registrationID)
    val update = BSONDocument("$set" -> BSONDocument("search" -> BSONDocument("code" -> searchResult.sicCode, "desc" -> searchResult.description)))
    collection.findAndUpdate(selector, update, fetchNewObject = true, upsert = true).map(_ => Some(searchResult))
  }

  override def insertChoice(registrationID: String) : Future[Option[SicCode]] = {
    retrieveSicStore(registrationID) flatMap {
      case Some(store) =>
        val selector = registrationIDSelector(registrationID)
        val update = BSONDocument("$addToSet" -> BSONDocument("choices" -> BSONDocument("code" -> store.search.sicCode, "desc" -> store.search.description)))
        collection.findAndUpdate(selector, update, upsert = true).map(_ => Some(store.search))
      case None => Future(None)
    }
  }

  override def removeChoice(registrationID: String, sicCode: String) : Future[Option[SicCode]] = {
    retrieveSicStore(registrationID) flatMap {
      case Some(store) =>
        val selector = registrationIDSelector(registrationID)
        val update = BSONDocument("$pull" -> BSONDocument("choices" -> BSONDocument("code" -> sicCode)))
        collection.findAndUpdate(selector, update).map(_ => Some(store.search))
      case None => Future(None)
    }
  }
}