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

import config.AppConfig
import helpers.{UnitTestFakeApp, UnitTestSpec}
import models._
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import services.JourneyService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future

class TestSetupControllerSpec extends UnitTestSpec with UnitTestFakeApp {

  class Setup {
    val controller: TestSetupController = new TestSetupController with I18nSupport {
      override val loginURL = "/test/login"

      override implicit val appConfig: AppConfig  = testAppConfig
      override val messagesApi: MessagesApi       = testMessagesApi
      override val authConnector: AuthConnector   = mockAuthConnector
      override val journeyService: JourneyService = mockJourneyService
    }

    val requestWithSessionId: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId
    )
  }

  val sessionId = "session-12345"


  val journeyName: String = Journey.QUERY_BUILDER
  val journey = Journey(sessionId, journeyName)

  val sicStore = SicStore(
    sessionId,
    journeyName,
    Some(SearchResults("test-query", 1, List(SicCode("19283746", "Search Sic Code Result Description")), List()))
  )

  "show" should {

    "return a 200 and render the SetupJourneyView page when a journey has already been initialised" in new Setup {

      when(mockJourneyService.retrieveJourney(eqTo(sessionId))(any()))
        .thenReturn(Future.successful(Some(journeyName)))

      AuthHelpers.showWithAuthorisedUser(controller.show, requestWithSessionId){ result =>
        status(result) mustBe 200
      }
    }

    "return a 200 and render the SetupJourneyView page when a journey has not been initialised" in new Setup {

      when(mockJourneyService.retrieveJourney(eqTo(sessionId))(any()))
        .thenReturn(Future.successful(None))

      AuthHelpers.showWithAuthorisedUser(controller.show, requestWithSessionId){ result =>
        status(result) mustBe 200
      }
    }
  }

  "submit" should {

    "return a 400 when the form is empty" in new Setup {
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = requestWithSessionId.withFormUrlEncodedBody(
        "journey" -> ""
      )

      AuthHelpers.submitWithAuthorisedUser(controller.submit, request){ result =>
        status(result) mustBe 400
      }
    }

    s"return a 303 and redirect to ${controllers.routes.ChooseActivityController.show()} when a journey is initialised" in new Setup {
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = requestWithSessionId.withFormUrlEncodedBody(
        "journey" -> journeyName
      )

      when(mockJourneyService.upsertJourney(eqTo(journey))(any()))
        .thenReturn(Future.successful(sicStore))

      AuthHelpers.submitWithAuthorisedUser(controller.submit, request){ result =>
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.ChooseActivityController.show().toString)
      }
    }
  }
}
