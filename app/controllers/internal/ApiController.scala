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

import auth.SicSearchExternalURLs
import config.Logging
import controllers.BasicController
import javax.inject.Inject
import models.setup.JourneyData
import play.api.libs.json.JsValue
import play.api.mvc.Action
import services.JourneySetupService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.config.ServicesConfig

class ApiControllerImpl @Inject()(val authConnector: AuthConnector,
                                  val journeySetupService: JourneySetupService,
                                  val servicesConfig: ServicesConfig) extends ApiController with SicSearchExternalURLs

trait ApiController extends BasicController with Logging {

  val journeySetupService: JourneySetupService

  def journeyInitialisation(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)

    userAuthorised(api = true) {
      withSessionId { sessionId =>
        withJsBody[JourneyData](JourneyData.newPublicJourneyReads(sessionId)) { journeyData =>
          journeySetupService.initialiseJourney(journeyData).map(Ok(_))
        }
      }
    }
  }
}
