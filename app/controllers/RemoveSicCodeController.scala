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

import javax.inject.Inject

import auth.SicSearchExternalURLs
import config.AppConfig
import forms.RemoveSicCodeForm
import models.SicCodeChoice
import models.setup.Identifiers
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc._
import services.{JourneyService, SicSearchService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.Future

class RemoveSicCodeControllerImpl @Inject()(val messagesApi: MessagesApi,
                                            val servicesConfig: ServicesConfig,
                                            val appConfig: AppConfig,
                                            val sicSearchService: SicSearchService,
                                            val journeyService: JourneyService,
                                            val authConnector: AuthConnector) extends RemoveSicCodeController with SicSearchExternalURLs

trait RemoveSicCodeController extends ICLController {
  implicit val appConfig: AppConfig
  val sicSearchService: SicSearchService
  def confirmationForm(description: String): Form[String] = RemoveSicCodeForm.form(description)

  private def withSicCodeChoice(journeyId: String, codes: List[SicCodeChoice], sicCode: String)(f: SicCodeChoice => Future[Result]): Future[Result] =
    codes.find(_.code == sicCode).fold(
      Future.successful(Redirect(controllers.routes.ChooseActivityController.show(journeyId, Some(true))))
    )(f)

  def show(journeyId: String, sicCode: String): Action[AnyContent] = Action.async {
    implicit request =>
      userAuthorised() {
        withJourney(journeyId) { journey =>
          withCurrentUsersChoices(Identifiers(journeyId, journey.sessionId)) { codes =>
            withSicCodeChoice(journeyId, codes, sicCode){ code =>
              Future.successful(Ok(views.html.pages.removeActivityConfirmation(journeyId, confirmationForm(code.desc), code)))
            }
          }
        }
      }
  }

  def submit(journeyId: String, sicCode: String): Action[AnyContent] = Action.async {
    implicit request =>
      userAuthorised() {
        withJourney(journeyId) { journey =>
          withCurrentUsersChoices(Identifiers(journeyId, journey.sessionId)) { codes =>
            withSicCodeChoice(journeyId, codes, sicCode) { code =>
              confirmationForm(code.desc).bindFromRequest().fold(
                errors => Future.successful(BadRequest(views.html.pages.removeActivityConfirmation(journeyId, errors, code))),
                {
                  case "yes" => sicSearchService.removeChoice(journey.sessionId, sicCode) map { _ =>
                    Redirect(controllers.routes.ConfirmationController.show(journeyId))
                  }
                  case "no" => Future.successful(Redirect(controllers.routes.ConfirmationController.show(journeyId)))
                }
              )
            }
          }
        }
      }
  }
}
