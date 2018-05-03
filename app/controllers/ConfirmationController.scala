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

package controllers

import auth.SicSearchExternalURLs
import config.AppConfig
import javax.inject.Inject
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.{JourneyService, SicSearchService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.Future

class ConfirmationControllerImpl @Inject()(val messagesApi: MessagesApi,
                                           val servicesConfig: ServicesConfig,
                                           val appConfig: AppConfig,
                                           val sicSearchService: SicSearchService,
                                           val journeyService: JourneyService,
                                           val authConnector: AuthConnector) extends ConfirmationController with SicSearchExternalURLs

trait ConfirmationController extends ICLController {
  val sicSearchService: SicSearchService

  val show: Action[AnyContent] = Action.async {
    implicit request =>
      userAuthorised() {
        withJourney { journey =>
          withCurrentUsersChoices(journey.sessionId) { choices =>
            Future.successful(Ok(views.html.pages.confirmation(choices)))
          }
        }
      }
  }

  val submit: Action[AnyContent] = Action.async {
    implicit request =>
      userAuthorised() {
        withJourney { journey =>
          withCurrentUsersChoices(journey.sessionId) { choices =>
            if (choices.size <= 4) {
              Future.successful(Ok("End of journey"))
            } else {
              val amountToRemove = (choices.size - 4).toString
              Future.successful(BadRequest(views.html.pages.confirmation(choices, Some(Seq(amountToRemove)))))
            }
          }
        }
      }
  }
}
