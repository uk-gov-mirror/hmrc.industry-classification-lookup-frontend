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

import java.time.{LocalDateTime, ZoneOffset}

import javax.inject.Inject
import models.setup.{JourneyData, JourneySetup}
import play.api.Configuration
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.DB
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json._
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.collection.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class JourneyDataRepo @Inject()(config: Configuration, mongoComponent: ReactiveMongoComponent) {
  val store = new JourneyDataMongoRepository(config, mongoComponent.mongoConnector.db)
}

class JourneyDataMongoRepository(config: Configuration, mongo: () => DB)
  extends ReactiveRepository[JourneyData, BSONObjectID]("journey-data", mongo, JourneyData.format)
    with JourneyDataRepository
    with TTLIndexing[JourneyData, BSONObjectID] {

  override val ttl: Long = config.getLong("mongodb.timeToLiveInSeconds").get

  override def ensureIndexes(implicit ec: ExecutionContext): Future[Seq[Boolean]] = {
    super.ensureIndexes flatMap { l =>
      ensureTTLIndexes map {
        ttl => l ++ ttl
      }
    }
  }

  override def indexes: Seq[Index] = Seq(
    Index(
      key    = Seq("journeyId" -> IndexType.Ascending),
      name   = Some("JourneyId"),
      unique = true
    ),
    Index(
      key    = Seq(
        "journeyId" -> IndexType.Ascending,
        "sessionId" -> IndexType.Ascending
      ),
      name   = Some("SessionIdAndJourneyId"),
      unique = true
    ),
    Index(
      key    = Seq(
        "journeyId"   -> IndexType.Ascending,
        "redirectUrl" -> IndexType.Ascending
      ),
      name   = Some("RedirectUrl"),
      unique = true
    )
  )

  private def journeySelector(journeyId: String, sessionId: String) = BSONDocument(
    "journeyId" -> journeyId,
    "sessionId" -> sessionId
  )

  private def renewJourney[T](journeyId: String, sessionId: String)(f: => T): Future[T] = {
    collection.update(journeySelector(journeyId, sessionId), BSONDocument("$set" -> BSONDocument("lastUpdated" -> BSONDocument("$date" -> LocalDateTime.now.toEpochSecond(ZoneOffset.UTC))))) map {
      _ => f
    }
  }

  def initialiseJourney(journeyData: JourneyData)(implicit writes: OWrites[JourneyData]): Future[WriteResult] = {
    collection.insert[JourneyData](journeyData)
  }

  def getRedirectUrl(journeyId: String, sessionId: String): Future[String] = {
    collection.find[BSONDocument, BSONDocument](journeySelector(journeyId, sessionId), BSONDocument("journeyId" -> 1, "redirectUrl" -> 1, "_id" -> 0)).one[JsValue] flatMap {
      case Some(json) => renewJourney(journeyId, sessionId) {
        json.\("redirectUrl").as[String]
      }
      case None =>
        logger.error(s"[getSetupDetails] - No journey found matching journeyId $journeyId")
        throw new NoSuchElementException(s"No redirect url was found for journey $journeyId")
    }
  }

  def getMessagesFor[T](journeyId: String, sessionId: String, messageKey: String)(implicit reads: Reads[T]): Future[Option[T]] = {
    collection.find(journeySelector(journeyId, sessionId), BSONDocument(s"customMessages.$messageKey" -> 1, "_id" -> 0)).one[JsValue] flatMap {
      case Some(json) => renewJourney(journeyId, sessionId) {
        json.\("customMessages").validate[JsValue].fold(
          _ => None,
          _.\(messageKey).asOpt[T]
        )
      }
      case _ =>
        logger.warn(s"No message block was found for journey $journeyId matching message key $messageKey")
        Future(None)
    } recover {
      case _ => throw new IllegalStateException(s"Journey data for journey $journeyId was not in the correct format to get messages for message key $messageKey")
    }
  }

  def getSetupDetails(journeyId: String, sessionId: String)(implicit reads: Reads[JourneySetup]): Future[JourneySetup] = {
    collection.find(journeySelector(journeyId, sessionId), BSONDocument("journeySetupDetails" -> 1)).one[JsValue] flatMap {
      case Some(json) => renewJourney(journeyId, sessionId) {
        json.\("journeySetupDetails").as[JourneySetup]
      }
      case None =>
        logger.error(s"[getSetupDetails] - No journey found matching journeyId $journeyId")
        throw new NoSuchElementException(s"No journey for $journeyId")
    }
  }
}

trait JourneyDataRepository {
  def initialiseJourney(journeyData: JourneyData)(implicit writes: OWrites[JourneyData]): Future[WriteResult]
  def getRedirectUrl(journeyId: String, sessionId: String): Future[String]
  def getMessagesFor[T](journeyId: String, sessionId: String, messageKey: String)(implicit reads: Reads[T]): Future[Option[T]]
  def getSetupDetails(journeyId: String, sessionId: String)(implicit reads: Reads[JourneySetup]): Future[JourneySetup]
}
