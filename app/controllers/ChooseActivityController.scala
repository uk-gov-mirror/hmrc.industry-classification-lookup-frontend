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
import models.SearchResults
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Result}
import services.SicSearchService
import uk.gov.hmrc.play.frontend.auth.Actions

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

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
          withSearchResults(sessionId) { searchResults =>
            val numResults = searchResults.numFound
            numResults match {
              case 1 => sicSearchService.insertChoice(sessionId,searchResults.results.head.sicCode) map { _ =>
                          Redirect(routes.ConfirmationController.show())
                        }
              case 0 => Future.successful(Redirect(controllers.routes.SicSearchController.show()))
              case _ => Future.successful(Ok(views.html.pages.chooseactivity(ChooseActivityForm.form, searchResults)))
            }
          }
        }
  }

  val submit: Action[AnyContent] = AuthorisedFor(taxRegime = new SicSearchRegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withSessionId { sessionId =>
          withSearchResults(sessionId) { searchResults =>
            ChooseActivityForm.form.bindFromRequest.fold(
              errors => Future.successful(BadRequest(views.html.pages.chooseactivity(errors, searchResults))),
              form => {
                sicSearchService.insertChoice(sessionId, form.code) map { _ =>
                  Redirect(routes.ConfirmationController.show())
                }
              }
            )
          }
        }
  }


  def filter(sectorCode: String): Action[AnyContent] = AuthorisedFor(taxRegime = new SicSearchRegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        withSessionId { sessionId =>
          withSearchResults(sessionId) { searchResults =>
            sicSearchService.search(sessionId, searchResults.query, Some(sectorCode)).map { _ =>
              Redirect(routes.ChooseActivityController.show())
            }
          }
      }
  }

  private[controllers] def withSearchResults(sessionId: String)(f: => SearchResults => Future[Result]): Future[Result] = {
    sicSearchService.retrieveSearchResults(sessionId) flatMap {
      case Some(searchResults) => f(searchResults)
      case None => Future.successful(Redirect(controllers.routes.SicSearchController.show()))
    }
  }
}
