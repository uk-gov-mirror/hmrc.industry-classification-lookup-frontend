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
import forms.sicsearch.SicSearchForm
import play.api.i18n.{I18nSupport, MessagesApi}
import services.SicSearchService
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

@Singleton
class ChooseActivityController @Inject()(val messagesApi: MessagesApi,
                                         val sicSearchService: SicSearchService,
                                         val authConnector: FrontendAuthConnector) extends ChooseActCtrl

trait ChooseActCtrl extends FrontendController with Actions with I18nSupport {

  val sicSearchService : SicSearchService

  val show = AuthorisedFor(taxRegime = new SicSearchRegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withSessionId { sessionId =>
          sicSearchService.retrieveSicStore(sessionId) flatMap {
            case Some(store) => Future.successful(Ok(views.html.pages.chooseactivity(ChooseActivityForm.form, Seq(store.search))))
            case None => Future.successful(Redirect(controllers.routes.SicSearchController.show()))
          }
        }
  }

  val submit = AuthorisedFor(taxRegime = new SicSearchRegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withSessionId { sessionId =>
          sicSearchService.retrieveSicStore(sessionId) flatMap {
            case Some(store) =>
              ChooseActivityForm.form.bindFromRequest.fold(
                errors => Future.successful(BadRequest(views.html.pages.chooseactivity(errors, Seq(store.search)))),
                form => {
                  form.code match {
                    case store.search.sicCode =>
                      sicSearchService.insertChoice(sessionId) map {
                        case Some(_) => Redirect(routes.ConfirmationController.show())
                        case _ => throw new RuntimeException("Failed to update sic store repo with choice")
                      }
                    case _ => Future.successful(BadRequest(views.html.pages.chooseactivity(ChooseActivityForm.form, Seq(store.search))))
                  }

                }
              )
            case None => Future.successful(Redirect(controllers.routes.SicSearchController.show()))
          }
        }
  }
}
