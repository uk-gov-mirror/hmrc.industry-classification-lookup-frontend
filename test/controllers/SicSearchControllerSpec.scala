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

import config.AppConfig
import helpers.{UnitTestFakeApp, UnitTestSpec}
import models.Journey
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import services.{JourneyService, SicSearchService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class SicSearchControllerSpec extends UnitTestSpec with UnitTestFakeApp {

  class Setup {
    val controller: SicSearchController = new SicSearchController with I18nSupport {
      override val loginURL = "/test/login"

      override implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
      override val sicSearchService: SicSearchService = mockSicSearchService
      override val authConnector: AuthConnector = mockAuthConnector
      implicit val messagesApi: MessagesApi = testMessagesApi
      override val journeyService: JourneyService = mockJourneyService
    }
  }

  val sessionId = "session-12345"
  val requestWithSession: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSessionId(sessionId)
  val query = "test query"
  val sicCode = "12345678"
  val journey: String = Journey.QUERY_BUILDER

  "show" should {

    "return 303 for an unauthorised user" in new Setup {
      AuthHelpers.showWithUnauthorisedUser(controller.show(), FakeRequest()) { request =>
        status(request) mustBe SEE_OTHER
      }
    }

    "return 200 for an authorised user with an initialised journey" in new Setup {
      mockWithJourney(sessionId, Some(journey))

      AuthHelpers.showWithAuthorisedUser(controller.show, requestWithSession) { result =>
        status(result) mustBe OK
      }
    }

    s"return 303 and redirect to ${controllers.test.routes.TestSetupController.show()} for an authorised user without an initialised journey" in new Setup {
      mockWithJourney(sessionId, None)

      AuthHelpers.showWithAuthorisedUser(controller.show, requestWithSession) { result =>
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.test.routes.TestSetupController.show().toString)
      }
    }
  }

  "submit" should {

    "return 303 for an unauthorised user" in new Setup {
      AuthHelpers.submitWithUnauthorisedUser(controller.submit, FakeRequest().withFormUrlEncodedBody()) { request =>
        status(request) mustBe SEE_OTHER
      }
    }

    "return 400 for an authorised user with no data" in new Setup {
      mockWithJourney(sessionId, Some(journey))

      val request: FakeRequest[AnyContentAsFormUrlEncoded] = requestWithSession.withFormUrlEncodedBody(
        "sicSearch" -> ""
      )

      AuthHelpers.submitWithAuthorisedUser(controller.submit, request) { result =>
        status(result) mustBe BAD_REQUEST
      }
    }

    "redirect to choose-business-activity when a search match is found" in new Setup {
      mockWithJourney(sessionId, Some(journey))

      when(mockSicSearchService.search(any(), any(), any(), any())(any[HeaderCarrier]()))
        .thenReturn(Future.successful(2))

      val request: FakeRequest[AnyContentAsFormUrlEncoded] = requestWithSession.withFormUrlEncodedBody(
        "sicSearch" -> sicCode
      )

      AuthHelpers.submitWithAuthorisedUser(controller.submit, request) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some("/sic-search/choose-business-activity")
      }
    }

    s"return a 200 and render the sicSearch page when 0 results are returned from search" in new Setup {
      mockWithJourney(sessionId, Some(journey))

      val request: FakeRequest[AnyContentAsFormUrlEncoded] = requestWithSession.withFormUrlEncodedBody(
        "sicSearch" -> query
      )

      when(mockSicSearchService.search(eqTo(sessionId), eqTo(query), any(), any())(any[HeaderCarrier]()))
        .thenReturn(Future.successful(0))

      AuthHelpers.submitWithAuthorisedUser(controller.submit, request) {
        result =>
          status(result) mustBe OK
      }
    }

    s"return a 303 and redirect to ${routes.ConfirmationController.show()} when 1 result is returned from search" in new Setup {
      mockWithJourney(sessionId, Some(journey))

      val request: FakeRequest[AnyContentAsFormUrlEncoded] = requestWithSession.withFormUrlEncodedBody(
        "sicSearch" -> query
      )

      when(mockSicSearchService.search(eqTo(sessionId), eqTo(query), any(), any())(any[HeaderCarrier]()))
        .thenReturn(Future.successful(1))

      AuthHelpers.submitWithAuthorisedUser(controller.submit, request) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.ConfirmationController.show().toString)
      }
    }
  }
}
