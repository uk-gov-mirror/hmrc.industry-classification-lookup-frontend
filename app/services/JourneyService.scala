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

package services

import javax.inject.Inject

import models.setup.{Identifiers, JourneyData, JourneySetup}
import play.api.libs.json.{JsValue, Json}
import repositories.{JourneyDataMongoRepository, JourneyDataRepo, JourneyDataRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class JourneyServiceImpl @Inject()(journeyDataRepo: JourneyDataRepo) extends JourneyService {
  override val journeyDataRepository: JourneyDataMongoRepository = journeyDataRepo.store
}

trait JourneyService {
  val journeyDataRepository: JourneyDataRepository

  def initialiseJourney(journeyData: JourneyData): Future[JsValue] = {
    journeyDataRepository.upsertJourney(journeyData) map { _ =>
      Json.obj(
        "journeyStartUri" -> s"/sic-search/${journeyData.identifiers.journeyId}/search-standard-industry-classification-codes",
        "fetchResultsUri" -> s"/internal/${journeyData.identifiers.journeyId}/fetch-results"
      )
    }
  }
  def updateJourneyWithJourneySetup(identifiers: Identifiers, journeySetupDetails: JourneySetup):Future[JourneySetup] = {
    journeyDataRepository.updateJourneySetup(identifiers, journeySetupDetails)
  }

  def getJourney(identifiers: Identifiers): Future[JourneyData] = {
    journeyDataRepository.retrieveJourneyData(identifiers)
  }

  def getRedirectUrl(identifiers: Identifiers): Future[String] = {
    journeyDataRepository.retrieveJourneyData(identifiers) map (_.redirectUrl)
  }
}