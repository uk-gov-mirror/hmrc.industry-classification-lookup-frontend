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

import models.Journey
import play.api.i18n.I18nSupport
import play.api.mvc.{Request, Result}
import services.JourneyService
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

trait ICLController extends FrontendController with Actions with I18nSupport {

  val journeyService: JourneyService

  def withSessionId(f: => String => Future[Result])(implicit req: Request[_]): Future[Result] = {
    hc(req).sessionId match {
      case Some(sessionId) => f(sessionId.value)
      case None => Future.successful(throw new RuntimeException("No session id found in request"))
    }
  }

  def withJourney(f: => Journey => Future[Result])(implicit req: Request[_]): Future[Result] = {
    withSessionId { sessionId =>
      journeyService.retrieveJourney(sessionId) flatMap {
        case Some(journey) => f(Journey(sessionId, journey))
        case None => Future.successful(Redirect(controllers.test.routes.TestSetupController.show()))//TODO should default the journey instead?
      }
    }
  }
}
