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

package controllers.internal

import javax.inject.Inject
import controllers.{BasicController, JourneyManager}
import models.setup.{Identifiers, JourneyData}
import play.api.libs.json._
import play.api.mvc.Results._
import play.api.mvc.{Action, AnyContent, BodyParsers}
import services.{JourneyService, SicSearchService}
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.ExecutionContext.Implicits.global

class ApiControllerImpl @Inject()(val journeyService: JourneyService,
                                  val sicSearchService: SicSearchService) extends ApiController {
}

trait ApiController extends BasicController with JourneyManager {

  val sicSearchService: SicSearchService

  def journeyInitialisation(): Action[JsValue] = Action.async(BodyParsers.parse.json) { implicit request =>
    withSessionId { sessionId =>
      withJsBody[JourneyData](JourneyData.initialRequestReads(sessionId)) { journeyData =>
        implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers)
        journeyService.initialiseJourney(journeyData).map(Ok(_))
      }
    }
  }

  def fetchResults(journeyId: String): Action[AnyContent] = Action.async { implicit request =>
    withSessionId { sessionId =>
      hasJourney(Identifiers(journeyId, sessionId)) { _ =>
        sicSearchService.retrieveChoicesForApi(journeyId) map {
          case Some(choices) => Ok(Json.obj("sicCodes" -> Json.toJson(choices)))
          case None => NotFound
        }
      }
    }
  }
}
