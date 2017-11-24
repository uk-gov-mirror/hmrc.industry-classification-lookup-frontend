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
import forms.sicsearch.SicSearchForm
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.SicSearchService
import uk.gov.hmrc.play.frontend.auth.Actions

import scala.concurrent.Future

@Singleton
class SicSearchControllerImpl @Inject()(val messagesApi: MessagesApi,
                                        val sicSearchService: SicSearchService,
                                        val authConnector: FrontendAuthConnector) extends SicSearchController

trait SicSearchController extends Actions with I18nSupport {

  val sicSearchService : SicSearchService

  val show: Action[AnyContent] = AuthorisedFor(taxRegime = new SicSearchRegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        Future.successful(Ok(views.html.pages.sicsearch(SicSearchForm.form)))
  }

  val submit: Action[AnyContent] = AuthorisedFor(taxRegime = new SicSearchRegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withSessionId { sessionId =>
          SicSearchForm.form.bindFromRequest.fold(
            errors => Future.successful(BadRequest(views.html.pages.sicsearch(errors))),
            form => sicSearchService.search(sessionId, form.sicSearch).map {
              case 0 => Ok(views.html.pages.sicsearch(SicSearchForm.form, Some(form.sicSearch)))
              case 1 => Redirect(routes.ConfirmationController.show())
              case _ => Redirect(routes.ChooseActivityController.show())
            }
          )
        }
  }
}
