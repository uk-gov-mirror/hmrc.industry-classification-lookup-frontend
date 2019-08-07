/*
 * Copyright 2019 HM Revenue & Customs
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

import auth.AuthFunction
import config.AppConfig
import models.SicCodeChoice
import models.setup.{Identifiers, JourneyData}
import play.api.i18n.I18nSupport
import play.api.mvc.{Request, Result}
import services.SicSearchService

import scala.concurrent.{ExecutionContext, Future}

trait ICLController extends BasicController with AuthFunction with I18nSupport with JourneyManager {

  val sicSearchService: SicSearchService

  implicit val appConfig: AppConfig

  def withJourney(journeyId: String)(f: => JourneyData => Future[Result])(implicit req: Request[_]): Future[Result] = {
    withSessionId { sessionId =>
      hasJourney(Identifiers(journeyId, sessionId)) { journeyData =>
        f(journeyData)
      }.recoverWith {
        case e => Future.successful(Redirect(controllers.test.routes.TestSetupController.show(journeyId)))
    }
      }
    }


  private[controllers] def withCurrentUsersChoices(identifiers: Identifiers)(f: List[SicCodeChoice] => Future[Result])(implicit ec: ExecutionContext): Future[Result] = {
    sicSearchService.retrieveChoices(identifiers.journeyId) flatMap {
      case Some(choices) => choices match {
        case Nil => Future.successful(Redirect(controllers.routes.ChooseActivityController.show(identifiers.journeyId)))
        case listOfChoices => f(listOfChoices)
      }
      case None => Future.successful(Redirect(controllers.routes.ChooseActivityController.show(identifiers.journeyId)))
    }
  }
}
