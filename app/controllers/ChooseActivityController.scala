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
import forms.chooseactivity.ChooseMultipleActivitiesForm
import forms.sicsearch.SicSearchForm
import models.setup.Identifiers
import models.{Journey, SearchResults, SicSearch}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.{JourneyService, SicSearchService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ChooseActivityControllerImpl @Inject()(val messagesApi: MessagesApi,
                                             val servicesConfig: ServicesConfig,
                                             val appConfig: AppConfig,
                                             val sicSearchService: SicSearchService,
                                             val journeyService: JourneyService,
                                             val authConnector: AuthConnector) extends ChooseActivityController with SicSearchExternalURLs

trait ChooseActivityController extends ICLController {

  val sicSearchService : SicSearchService
  val chooseActivityForm = ChooseMultipleActivitiesForm.form

  def show(journeyId: String, doSearch: Option[Boolean] = None): Action[AnyContent] = Action.async {
    implicit request =>
      userAuthorised() {
        withJourney(journeyId) { journey =>
          if (doSearch.contains(true)) {
            withSearchResults(Identifiers(journeyId, journey.sessionId)) { searchResults =>
              Future.successful(Ok(views.html.pages.chooseactivity(journeyId, SicSearchForm.form.fill(SicSearch(searchResults.query)), chooseActivityForm, Some(searchResults))))
            }
          } else {
            Future.successful(Ok(views.html.pages.chooseactivity(journeyId, SicSearchForm.form, chooseActivityForm, None)))
          }
        }
      }
  }

  def submit(journeyId: String, doSearch: Option[String] = None): Action[AnyContent] = Action.async {
    implicit request =>
      userAuthorised() {
        withJourney(journeyId) { journey =>
          doSearch.fold(performActivity(journeyId, journey))(_ => performSearch(journeyId, journey))
        }
      }
  }

  def filter(journeyId: String, sectorCode: String): Action[AnyContent] = Action.async {
    implicit request =>
      userAuthorised() {
        withJourney(journeyId) { journey =>
          withSearchResults(Identifiers(journeyId, journey.sessionId)) { searchResults =>
            sicSearchService.search(journey.sessionId, searchResults.query, journey.name, journey.dataSet, Some(sectorCode)).map { _ =>
              Redirect(routes.ChooseActivityController.show(journeyId, Some(true)))
            }
          }
        }
      }
  }

  private[controllers] def performSearch(journeyId: String, journey: Journey)(implicit request: Request[_]): Future[Result] = {
    SicSearchForm.form.bindFromRequest.fold(
      errors => Future.successful(BadRequest(views.html.pages.chooseactivity(journeyId, errors, chooseActivityForm, None))),
      form => sicSearchService.search(journey.sessionId, form.sicSearch, journey.name, journey.dataSet, None) map {
        case 1 => Redirect(routes.ConfirmationController.show(journeyId))
        case _ => Redirect(routes.ChooseActivityController.show(journeyId, Some(true)))
      }
    )
  }

  private[controllers] def performActivity(journeyId: String, journey: Journey)(implicit request: Request[_]): Future[Result] = {
    withSearchResults(Identifiers(journeyId, journey.sessionId)) { searchResults =>
      chooseActivityForm.bindFromRequest().fold(
        errors => Future.successful(BadRequest(views.html.pages.chooseactivity(journeyId, SicSearchForm.form.fill(SicSearch(searchResults.query)), errors, Some(searchResults)))),
        code => sicSearchService.lookupSicCodes(journey.sessionId, code) map { _ =>
            Redirect(routes.ConfirmationController.show(journeyId))
          }
      )
    }
  }

  private[controllers] def withSearchResults(identifiers: Identifiers)(f: => SearchResults => Future[Result])(implicit request: Request[_]): Future[Result] = {
    sicSearchService.retrieveSearchResults(identifiers.sessionId) flatMap {
      case Some(searchResults) => f(searchResults)
      case None => Future.successful(Ok(views.html.pages.chooseactivity(identifiers.journeyId, SicSearchForm.form, chooseActivityForm, None)))
    }
  }
}
