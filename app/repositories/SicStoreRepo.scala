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

import javax.inject.{Inject, Singleton}

import models.{Journey, SearchResults, SicStore}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Configuration
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.DB
import reactivemongo.bson.{BSONDocument, BSONObjectID, BSONString}
import reactivemongo.play.json.BSONFormats
import reactivemongo.play.json.ImplicitBSONHandlers.BSONDocumentWrites
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.collection.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SicStoreRepo @Inject()(configuration: Configuration, mongo: ReactiveMongoComponent) {
  val repo = new SicStoreMongoRepository(configuration, mongo.mongoConnector.db)
}

trait SicStoreRepository {
  def updateSearchResults(sessionId: String, searchResult: SearchResults)(implicit ec: ExecutionContext): Future[Boolean]
  def retrieveSicStore(sessionId: String)(implicit ec: ExecutionContext): Future[Option[SicStore]]
  def insertChoice(sessionId: String, sicCode: String)(implicit ec: ExecutionContext): Future[Boolean]
  def removeChoice(sessionId: String, choice: String)(implicit ec: ExecutionContext): Future[Boolean]
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

    val searchjson = Json.obj(
      "search" -> Json.obj(
        "query" -> searchResults.query,
        "numFound" -> searchResults.numFound,
        "results" -> Json.toJson(searchResults.results),
        "sectors" -> Json.toJson(searchResults.sectors)
      ),
      "lastUpdated" -> Json.toJson(now)(ReactiveMongoFormats.dateTimeWrite))

    val update = BSONDocument("$set" -> BSONFormats.readAsBSONValue(searchjson).get)

    collection.update(selector, update).map(_.ok)
  }

  override def insertChoice(sessionId: String, sicCode: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    retrieveSicStore(sessionId) flatMap {
      case Some(store) =>
        store.searchResults.flatMap(_.results.find(_.sicCode == sicCode)) match {
          case Some(sicCodeToAdd) =>
            val selector = sessionIdSelector(sessionId)

            val choicesjson = Json.obj("choices" -> Json.obj(
              "code" -> sicCodeToAdd.sicCode,
              "desc" -> sicCodeToAdd.description
            ))

            val set = Json.obj(
              "lastUpdated" -> Json.toJson(now)(ReactiveMongoFormats.dateTimeWrite)
            )

            val update = BSONDocument(
              "$addToSet" -> BSONFormats.readAsBSONValue(choicesjson).get,
              "$set" -> BSONFormats.readAsBSONValue(set).get
            )

            collection.update(selector, update, upsert = true).map(_.ok)
          case None => Future.successful(false)
        }
      case None => Future.successful(false)
    }
  }

  override def removeChoice(sessionId: String, sicCode: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    retrieveSicStore(sessionId) flatMap {
      case Some(_) =>
        val selector = sessionIdSelector(sessionId)

        val pull = Json.obj(
          "choices" -> Json.obj("code" -> sicCode)
        )

        val set = Json.obj(
          "lastUpdated" -> Json.toJson(now)(ReactiveMongoFormats.dateTimeWrite)
        )

        val update = BSONDocument(
          "$pull" -> BSONFormats.readAsBSONValue(pull).get,
          "$set" ->  BSONFormats.readAsBSONValue(set).get
        )

        collection.update(selector, update).map(_.nModified == 1)
      case None => Future.successful(false)
    }
  }

  private def initJourney(journey: Journey): Future[SicStore] = {
    val sicStore = SicStore(journey.sessionId, journey.name)
    val insertion = Json.toJson(sicStore).as[JsObject]
    val document = BSONDocument("$set" -> BSONFormats.readAsBSONValue(insertion).get)
    val selector = sessionIdSelector(journey.sessionId)
    collection.update(selector, document, upsert = true) map (_ => sicStore)
  }

  def upsertJourney(journey: Journey): Future[SicStore] = {

    retrieveSicStore(journey.sessionId) flatMap {
      case Some(sicStore) =>
        val selector = sessionIdSelector(journey.sessionId)
        val sicStoreWithJourney = sicStore.copy(journey = journey.name)
        val json = Json.toJson(sicStoreWithJourney).as[JsObject]
        val update = BSONDocument("$set" -> BSONFormats.readAsBSONValue(json).get)

        collection.update(selector, update, upsert = true) map { updateResult =>
          if(updateResult.nModified == 1) {
            sicStoreWithJourney
          } else {
            throw new Exception(s"Did not update sic store with new journey for session id : ${journey.sessionId}" +
              s" - number of documents modified ${updateResult.nModified}")
          }
        }
      case None => initJourney(journey)
    }
  }
}