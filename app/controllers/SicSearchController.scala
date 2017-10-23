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
import services.{SicSearchService, SicSearchServiceImpl}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

@Singleton
class SicSearchController @Inject()(val messagesApi: MessagesApi,
                                    val sicSearchService: SicSearchService,
                                    val authConnector: FrontendAuthConnector) extends SicSearchCtrl

trait SicSearchCtrl extends FrontendController with Actions with I18nSupport {

  val sicSearchService : SicSearchService

  val show = AuthorisedFor(taxRegime = new SicSearchRegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        Future.successful(Ok(views.html.pages.sicsearch(SicSearchForm.form)))
  }

  val submit = AuthorisedFor(taxRegime = new SicSearchRegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withSessionId { sessionId =>
          SicSearchForm.form.bindFromRequest.fold(
            errors => Future.successful(BadRequest(views.html.pages.sicsearch(errors))),
            form => sicSearchService.lookupSicCode(form.sicSearch).flatMap {
              case Some(code) => sicSearchService.updateSearchResults(sessionId, code).map {
                case Some(res) => Redirect(routes.ChooseActivityController.show())
                case None => throw new RuntimeException("Failed to add to sic-search repo")
              }
              case None => Future.successful(BadRequest(
                views.html.pages.sicsearch(SicSearchForm.form, Some(form.sicSearch))
              ))
            }
          )
        }
  }
}
