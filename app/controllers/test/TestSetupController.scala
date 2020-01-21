/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.LocalDateTime

import auth.SicSearchExternalURLs
import config.AppConfig
import controllers.{ICLController, JourneyManager}
import javax.inject.Inject

import models.setup.{Identifiers, JourneyData, JourneySetup}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import services.{JourneyService, SicSearchService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.voa.play.form.ConditionalMappings.{isEqual, mandatoryIf}

import scala.concurrent.Future

class TestSetupControllerImpl @Inject()(val messagesApi: MessagesApi,
                                        val appConfig: AppConfig,
                                        val journeyService: JourneyService,
                                        val servicesConfig: ServicesConfig,
                                        val sicSearchService: SicSearchService,
                                        val authConnector: AuthConnector) extends TestSetupController with SicSearchExternalURLs

trait TestSetupController extends ICLController with JourneyManager {

  val journeyService: JourneyService

  val journeySetupForm = {
    def journeySetupApply(dataSet: String = JourneyData.ONS,
                          queryParser: Option[Boolean] = None,
                          queryBooster: Option[Boolean] = None,
                          amountOfResults: Int = 50): JourneySetup = new JourneySetup(dataSet, queryParser, queryBooster, amountOfResults, None)

    def journeySetupUnapply(arg: JourneySetup): Option[(String, Option[Boolean], Option[Boolean], Int)] = Some(arg.dataSet, arg.queryParser, arg.queryBooster, arg.amountOfResults)

    Form(
      mapping(
        "dataSet" -> nonEmptyText.verifying(dSet => JourneyData.dataSets.contains(dSet)),
        "queryParser" -> optional(boolean),
        "booster" -> mandatoryIf(isEqual("queryParser", "false"), boolean),
        "amountOfResults" -> number(1)
      )(journeySetupApply)(journeySetupUnapply)
    )
  }

  def show(journeyId: String): Action[AnyContent] = Action.async {
    implicit request =>
      userAuthorised() {
        withSessionId { sessionId =>
          hasJourney(Identifiers(journeyId, sessionId)) { journeyData =>
          Future.successful(Ok(views.html.test.SetupJourneyView(journeyId, journeySetupForm.fill(journeyData.journeySetupDetails))))
          }
        }
      }
  }

  def submit(journeyId: String): Action[AnyContent] = Action.async {
    implicit request =>
      userAuthorised() {
        withSessionId { sessionId =>
          hasJourney(Identifiers(journeyId, sessionId)) { journeyData =>
            journeySetupForm.bindFromRequest.fold(
              errors =>
                Future.successful(BadRequest(views.html.test.SetupJourneyView(journeyId, errors))),
              validJourney => {
                journeyService.updateJourneyWithJourneySetup(journeyData.identifiers, validJourney).map( _ => Redirect(controllers.routes.ChooseActivityController.show(journeyId)))
              }
            )
          }
        }
      }
  }

  val testSetup: Action[AnyContent] = Action.async {
    implicit request =>
      userAuthorised(api = true) {
        withSessionId { sessionId =>
          val journeyId: String = sessionId
          val journeyData: JourneyData = JourneyData(
            identifiers = Identifiers(journeyId, sessionId),
            redirectUrl = s"/sic-search/test-only/$journeyId/end-of-journey",
            journeySetupDetails = JourneySetup(),
            lastUpdated = LocalDateTime.now()
          )
          journeyService.initialiseJourney(journeyData).map( _ => Redirect(controllers.test.routes.TestSetupController.show(journeyId)))
        }
      }
  }

  def endOfJourney(journeyId: String): Action[AnyContent] = Action.async {
    implicit request =>
      userAuthorised() {
        withSessionId { sessionId =>
          hasJourney(Identifiers(journeyId, sessionId)) { _ =>
            sicSearchService.retrieveChoices(journeyId) map { choices =>
              Ok("End of Journey" + Json.prettyPrint(Json.toJson(choices)))
            }
          }
        }
      }
  }
}