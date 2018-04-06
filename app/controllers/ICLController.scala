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

import auth.AuthFunction
import config.AppConfig
import models.{Journey, SicCode}
import play.api.i18n.I18nSupport
import play.api.mvc.{Request, Result}
import services.{JourneyService, SicSearchService}

import scala.concurrent.{ExecutionContext, Future}

trait ICLController extends AuthFunction with I18nSupport {

  val journeyService: JourneyService
  val sicSearchService: SicSearchService
  implicit val appConfig: AppConfig

  def withSessionId(f: => String => Future[Result])(implicit req: Request[_]): Future[Result] = {
    hc(req).sessionId match {
      case Some(sessionId) => f(sessionId.value)
      case None => Future.successful(throw new RuntimeException("No session id found in request"))
    }
  }

  def withJourney(f: => Journey => Future[Result])(implicit req: Request[_]): Future[Result] = {
    withSessionId { sessionId =>
      journeyService.retrieveJourney(sessionId) flatMap {
        case Some((journey, dataSet)) => f(Journey(sessionId, journey, dataSet))
        case None => Future.successful(Redirect(controllers.test.routes.TestSetupController.show()))//TODO should default the journey instead?
      }
    }
  }

  private[controllers] def withCurrentUsersChoices(sessionId: String)(f: List[SicCode] => Future[Result])(implicit ec: ExecutionContext): Future[Result] = {
    sicSearchService.retrieveChoices(sessionId) flatMap {
      case Some(choices) => choices match {
        case Nil => Future.successful(Redirect(controllers.routes.ChooseActivityController.show()))
        case listOfChoices => f(listOfChoices)
      }
      case None => Future.successful(Redirect(controllers.routes.ChooseActivityController.show()))
    }
  }

}
