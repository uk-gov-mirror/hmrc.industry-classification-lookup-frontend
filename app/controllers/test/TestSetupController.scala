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

package controllers.test

import javax.inject.Inject

import auth.SicSearchExternalURLs
import config.AppConfig
import controllers.ICLController
import models.Journey
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.JourneyService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.Future

object Journeys {
  val QUERY_BUILDER = "query-builder"
  val QUERY_PARSER  = "query-parser"
}

class TestSetupControllerImpl @Inject()(val messagesApi: MessagesApi,
                                        val appConfig: AppConfig,
                                        val journeyService: JourneyService,
                                        val servicesConfig: ServicesConfig,
                                        val authConnector: AuthConnector) extends TestSetupController with SicSearchExternalURLs

trait TestSetupController extends ICLController {

  val journeyService: JourneyService
  val form: Form[String] = Form(single("journey" -> nonEmptyText))

  val show: Action[AnyContent] = Action.async {
    implicit request =>
      authorised {
        withSessionId { sessionId =>
          journeyService.retrieveJourney(sessionId) map {
            case Some(journey) => Ok(views.html.test.SetupJourneyView(form.fill(journey)))
            case None => Ok(views.html.test.SetupJourneyView(form))
          }
        }
      }
  }

  val submit: Action[AnyContent] = Action.async {
    implicit request =>
      authorised {
        withSessionId { sessionId =>
          form.bindFromRequest.fold(
            errors => Future.successful(BadRequest(views.html.test.SetupJourneyView(errors))),
            journeyName => {
              val journey = Journey(sessionId, journeyName)
              journeyService.upsertJourney(journey) map { _ =>
                Redirect(controllers.routes.SicSearchController.show())
              }
            }
          )
        }
      }
  }
}

