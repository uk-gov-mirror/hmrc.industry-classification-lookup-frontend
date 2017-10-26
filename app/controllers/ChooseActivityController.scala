/*
 * Copyright 2017 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}

import auth.SicSearchRegime
import config.FrontendAuthConnector
import forms.chooseactivity.ChooseActivityForm
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.SicSearchService
import uk.gov.hmrc.play.frontend.auth.Actions

import scala.concurrent.Future

@Singleton
class ChooseActivityControllerImpl @Inject()(val messagesApi: MessagesApi,
                                             val sicSearchService: SicSearchService,
                                             val authConnector: FrontendAuthConnector) extends ChooseActivityController

trait ChooseActivityController extends Actions with I18nSupport {

  val sicSearchService : SicSearchService

  val show: Action[AnyContent] = AuthorisedFor(taxRegime = new SicSearchRegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withSessionId { sessionId =>
          sicSearchService.retrieveSearchResults(sessionId) flatMap {
            case Some(searchResults) => Future.successful(Ok(views.html.pages.chooseactivity(ChooseActivityForm.form, searchResults)))
            case None => Future.successful(Redirect(controllers.routes.SicSearchController.show()))
          }
        }
  }

  val submit: Action[AnyContent] = AuthorisedFor(taxRegime = new SicSearchRegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withSessionId { sessionId =>
          sicSearchService.retrieveSearchResults(sessionId) flatMap {
            case Some(searchResults) =>
              ChooseActivityForm.form.bindFromRequest.fold(
                errors => Future.successful(BadRequest(views.html.pages.chooseactivity(errors, searchResults))),
                form => {
                  sicSearchService.insertChoice(sessionId, form.code) map { _ =>
                    Redirect(routes.ConfirmationController.show())
                  }
                }
              )
            case None => Future.successful(Redirect(controllers.routes.SicSearchController.show()))
          }
        }
  }
}
