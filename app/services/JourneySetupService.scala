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
import models.setup.{JourneyData, JourneySetup}
import models.setup.JourneyData._
import play.api.libs.json.{JsValue, Json}
import repositories._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class JourneySetupServiceImpl @Inject()(journeyDataRepo: JourneyDataRepo) extends JourneySetupService {
  override val journeyDataRepository = journeyDataRepo.store
}

trait JourneySetupService {

  val journeyDataRepository: JourneyDataRepository

  def initialiseJourney(journeyData: JourneyData): Future[JsValue] = {
    journeyDataRepository.initialiseJourney(journeyData) map { _ =>
      Json.obj(
        //TODO: Change to real uri's
        "journeyStartUri" -> s"/test/uri/search-standard-industry-classification-codes?journey=${journeyData.journeyId}",
        "fetchResultsUri" -> s"/test/uri/${journeyData.journeyId}/fetch-results"
      )
    }
  }

  def getRedirectUrl(journeyId: String, sessionId: String): Future[String] = {
    journeyDataRepository.getRedirectUrl(journeyId, sessionId)
  }

  def getSetupDetails(journeyId: String, sessionId: String): Future[JourneySetup] = {
    journeyDataRepository.getSetupDetails(journeyId, sessionId)
  }
}
