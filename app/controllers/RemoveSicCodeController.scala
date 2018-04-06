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
import forms.ConfirmationForm
import javax.inject.Inject
import models.Confirmation.{NO, YES}
import models.{SicCode, SicCodeGroup}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Controller}
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
  val confirmationForm = ConfirmationForm.form

  def show(sicCode: String): Action[AnyContent] = Action.async {
    implicit request =>
      withJourney { journey =>
        withCurrentUsersChoices(journey.sessionId) { codes =>
          Future.successful(Ok(views.html.pages.removeActivityConfirmation(confirmationForm, SicCodeGroup(codes.find(_.sicCode == sicCode).get, Nil))))
        }
      }
  }

  def submit(sicCode: String): Action[AnyContent] = Action.async {
    implicit request =>
      authorised {
        withJourney { journey =>
          ConfirmationForm.form.bindFromRequest().fold(
            errors => Future.successful(BadRequest(views.html.pages.removeActivityConfirmation(errors, SicCodeGroup(SicCode("011110", "description crip of sic code"), List("qwerty"))))),
            {
              case YES => sicSearchService.removeChoice(journey.sessionId, sicCode) map { _ =>
                Redirect(controllers.routes.ConfirmationController.show())
              }
              case NO => Future.successful(Redirect(controllers.routes.ConfirmationController.show()))
            }
          )
        }
      }
  }
}
