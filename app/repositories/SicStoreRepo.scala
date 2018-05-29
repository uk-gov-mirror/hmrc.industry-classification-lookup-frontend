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

import models.{Journey, SearchResults, SicCodeChoice, SicStore}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Configuration
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}
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
  def insertChoices(sessionId: String, sicCodes: List[SicCodeChoice])(implicit ec: ExecutionContext): Future[Boolean]
  def removeChoice(sessionId: String, choice: String)(implicit ec: ExecutionContext): Future[Boolean]
}


class SicStoreMongoRepository(config: Configuration, mongo: () => DB)
  extends ReactiveRepository[SicStore, BSONObjectID]("sic-store", mongo, SicStore.format)
  with SicStoreRepository with TTLIndexing[SicStore, BSONObjectID] {

  private[repositories] def sessionIdSelector(sessionId: String): BSONDocument = BSONDocument(
    "sessionId" -> BSONString(sessionId)
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

    val getCurrentSectorJson: JsObject = searchResults.currentSector.fold(Json.obj())(sector => Json.obj("currentSector" -> Json.toJson(sector)))

    val searchjson = Json.obj(
      "search" -> Json.obj(
        "query" -> searchResults.query,
        "numFound" -> searchResults.numFound,
        "results" -> Json.toJson(searchResults.results),
        "sectors" -> Json.toJson(searchResults.sectors)
      ).++(getCurrentSectorJson),
      "lastUpdated" -> Json.toJson(now)(ReactiveMongoFormats.dateTimeWrite))

    val update = BSONDocument("$set" -> BSONFormats.readAsBSONValue(searchjson).get)

    collection.update(selector, update).map(_.ok)
  }

  private def getChoicesJsonObject(sicCodes: List[SicCodeChoice]): JsValue = JsArray(sicCodes.map {
    scc => Json.obj(
      "code" -> scc.code,
      "desc"        -> scc.desc,
      "indexes"     -> scc.indexes
    )
  })

  private def getIndexesJsonObject(strings: List[String]): JsValue = JsArray(strings.map(Json.toJson[String]))

  override def insertChoices(sessionId: String, sicCodes: List[SicCodeChoice])(implicit ec: ExecutionContext): Future[Boolean] = {
    retrieveSicStore(sessionId) flatMap {
      case Some(store) =>

        val selector = sessionIdSelector(sessionId)

        val (toUpdate, toAdd) = store.choices match {
          case Some(choices) =>
            sicCodes.partition (
              x => choices.exists(_.code == x.code)
            )
          case None => (Nil, sicCodes)
        }

        val set = Json.obj(
          "lastUpdated" -> Json.toJson(now)(ReactiveMongoFormats.dateTimeWrite)
        )

        val update = BSONDocument(
          "$addToSet" -> BSONDocument("choices" -> BSONDocument("$each" -> BSONFormats.readAsBSONValue(getChoicesJsonObject(toAdd)).get)),
          "$set"      -> BSONFormats.readAsBSONValue(set).get
        )

        toUpdate.map{
          sicCodeChoice =>
            val indexes = store.choices match {
              case Some(list) =>
                list.find(_.code == sicCodeChoice.code)
                  .fold(sicCodeChoice.indexes)(choice => (choice.indexes ++ sicCodeChoice.indexes).distinct)
              case None => sicCodeChoice.indexes
            }
            val upsertSelector = BSONDocument("choices" -> BSONDocument("$elemMatch" -> BSONDocument("code" -> sicCodeChoice.code))) ++ selector
            val upsert = BSONDocument(
              "$addToSet" -> BSONDocument("choices.$.indexes" -> BSONDocument("$each" -> BSONFormats.readAsBSONValue(getIndexesJsonObject(indexes)).get)),
              "$set"      -> BSONFormats.readAsBSONValue(set).get
            )
            collection.update(upsertSelector, upsert, upsert = true).map(_.ok)
        }

        collection.update(selector, update, upsert = true).map(_.ok)
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
    val sicStore  = SicStore(journey.sessionId, journey.name, journey.dataSet)
    val insertion = Json.toJson(sicStore).as[JsObject]
    val document  = BSONDocument("$set" -> BSONFormats.readAsBSONValue(insertion).get)
    val selector  = sessionIdSelector(journey.sessionId)
    collection.update(selector, document, upsert = true) map (_ => sicStore)
  }

  def upsertJourney(journey: Journey): Future[SicStore] = {

    retrieveSicStore(journey.sessionId) flatMap {
      case Some(sicStore) =>
        val selector = sessionIdSelector(journey.sessionId)
        val sicStoreWithJourney = sicStore.copy(journey = journey.name, dataSet = journey.dataSet)
        val json = Json.toJson(sicStoreWithJourney).as[JsObject]
        val update = BSONDocument("$set" -> BSONFormats.readAsBSONValue(json).get)


        if(sicStoreWithJourney == sicStore) {
          Future.successful(sicStoreWithJourney)
        } else {
          collection.update(selector, update, upsert = true) map { updateResult =>
            if(updateResult.nModified == 1) {
              sicStoreWithJourney
            } else {
              throw new Exception(s"Did not update sic store with new journey for session id : ${journey.sessionId}" +
                s" - number of documents modified ${updateResult.nModified}")
            }
          }
        }
      case None => initJourney(journey)
    }
  }
}