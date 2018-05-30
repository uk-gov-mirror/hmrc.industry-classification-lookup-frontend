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

import models.setup.{Identifiers, JourneyData}
import play.api.Configuration
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
      key = Seq(
        "identifiers.journeyId" -> IndexType.Ascending,
        "identifiers.sessionId" -> IndexType.Ascending
        ),
      name = Some("SessionIdAndJourneyId"),
      unique = true
    )
  )

  private def identifiersSelector(identifiers: Identifiers) = BSONDocument("identifiers.journeyId" -> identifiers.journeyId, "identifiers.sessionId" -> identifiers.sessionId)

  private def renewJourney[T](identifiers: Identifiers)(f: => T): Future[T] = {
    val selector = identifiersSelector(identifiers)
    collection.update(selector, BSONDocument("$set" -> BSONDocument("lastUpdated" -> BSONDocument("$date" -> LocalDateTime.now.toEpochSecond(ZoneOffset.UTC))))) map { _ =>
      f
    }
  }

  def initialiseJourney(journeyData: JourneyData): Future[WriteResult] = {
    collection.insert[JourneyData](journeyData)
  }

  def retrieveJourneyData(identifiers: Identifiers): Future[JourneyData] = {
    val selector = identifiersSelector(identifiers)
    collection.find(selector).one[JourneyData] flatMap {
      case Some(data) => renewJourney(identifiers)(data)
      case _ =>
        logger.warn("Could not find JourneyId")
        throw new RuntimeException("Missing document")
    }
  }

}
trait JourneyDataRepository {
  def initialiseJourney(journeyData: JourneyData): Future[WriteResult]
  def retrieveJourneyData(identifiers: Identifiers): Future[JourneyData]
}
