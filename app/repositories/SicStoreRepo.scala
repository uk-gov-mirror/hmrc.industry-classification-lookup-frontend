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

import models.{SearchResults, SicStore}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.{Configuration, Logger}
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.DB
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONLong, BSONObjectID, BSONString}
import reactivemongo.json.BSONFormats
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.collection.Seq
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class SicStoreRepo @Inject()(configuration: Configuration, mongo: ReactiveMongoComponent) {
  val repo = new SicStoreMongoRepository(configuration, mongo.mongoConnector.db)
}

trait SicStoreRepository {
  def updateSearchResults(registrationID: String, searchResult: SearchResults)(implicit ec: ExecutionContext): Future[Boolean]
  def retrieveSicStore(registrationID: String)(implicit ec: ExecutionContext): Future[Option[SicStore]]
  def insertChoice(registrationID: String, sicCode: String)(implicit ec: ExecutionContext): Future[Boolean]
  def removeChoice(registrationID: String, choice: String)(implicit ec: ExecutionContext): Future[Boolean]
}

class SicStoreMongoRepository(config: Configuration, mongo: () => DB)
  extends ReactiveRepository[SicStore, BSONObjectID]("sic-store", mongo, SicStore.format)
  with SicStoreRepository with TTLIndexing[SicStore, BSONObjectID] {

  private[repositories] def sessionIdSelector(sessionId: String): BSONDocument = BSONDocument(
    "registrationID" -> BSONString(sessionId)
  )

  override val ttl: Long = config.getLong("mongodb.timeToLiveInSeconds").get

  private[repositories] def now = DateTime.now(DateTimeZone.UTC)

  override def ensureIndexes(implicit ec: ExecutionContext): Future[Seq[Boolean]] = {
    super.ensureIndexes flatMap { l =>
      ensureTTLIndexes map {
        ttl => l ++ ttl
      }
    }
  }

  override def retrieveSicStore(sessionId: String)(implicit ec: ExecutionContext): Future[Option[SicStore]] = {
    collection.find(sessionIdSelector(sessionId)).one[SicStore]
  }

  override def updateSearchResults(sessionId: String, searchResults: SearchResults)(implicit ec: ExecutionContext): Future[Boolean] = {
    val selector = sessionIdSelector(sessionId)

    val searchjson = Json.obj("search" -> Json.obj(
      "query" -> searchResults.query,
      "numFound" -> searchResults.numFound,
      "results" -> Json.toJson(searchResults.results),
      "sectors" -> Json.toJson(searchResults.sectors)
    ), "lastUpdated" -> Json.toJson(now)(ReactiveMongoFormats.dateTimeWrite))
    val update = Json.obj("$set" -> searchjson)

    collection.update(selector, update, upsert = true).map(_.ok)
  }

  override def insertChoice(registrationID: String, sicCode: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    retrieveSicStore(registrationID) flatMap {
      case Some(store) =>
        store.searchResults.results.find(_.sicCode == sicCode) match {
          case Some(sicCodeToAdd) =>
            val selector = sessionIdSelector(registrationID)
            val choicesjson = Json.obj("choices" -> Json.obj(
              "code" -> sicCodeToAdd.sicCode,
              "desc" -> sicCodeToAdd.description
            ))
            val update = Json.obj("$addToSet" -> choicesjson, "$set" -> Json.obj(
              "lastUpdated" -> Json.toJson(now)(ReactiveMongoFormats.dateTimeWrite)
            ))

            collection.update(selector, update, upsert = true).map(_.ok)
          case None => Future.successful(false)
        }
      case None => Future.successful(false)
    }
  }

  override def removeChoice(registrationID: String, sicCode: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    retrieveSicStore(registrationID) flatMap {
      case Some(_) =>
        val selector = sessionIdSelector(registrationID)
        val update = Json.obj(
          "$pull" -> Json.obj(
            "choices" -> Json.obj("code" -> sicCode)
          ), "$set" -> Json.obj(
            "lastUpdated" -> Json.toJson(now)(ReactiveMongoFormats.dateTimeWrite)
          )
        )
        collection.update(selector, update).map(_.nModified == 1)
      case None => Future.successful(false)
    }
  }
}