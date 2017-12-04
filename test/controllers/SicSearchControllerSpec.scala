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

import builders.AuthBuilders
import models.Journey
import uk.gov.hmrc.http.HeaderCarrier
import org.mockito.Mockito._
import play.api.i18n.MessagesApi
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import play.api.test.Helpers.redirectLocation
import services.{JourneyService, SicSearchService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

class SicSearchControllerSpec extends ControllerSpec with AuthBuilders {

  trait Setup {
    val controller: SicSearchController = new SicSearchController {
      override val sicSearchService: SicSearchService = mockSicSearchService
      override val authConnector: AuthConnector = mockAuthConnector
      implicit val messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]
      override val journeyService: JourneyService = mockJourneyService
    }

    resetMocks()
  }

  val sessionId = "session-12345"
  val requestWithSession: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSessionId(sessionId)
  val query = "test query"
  val sicCode = "12345678"
  val journey: String = Journey.QUERY_BUILDER

  "show" should {

    "return 303 for an unauthorised user" in new Setup {
      val result: Result = controller.show()(FakeRequest())
      status(result) shouldBe SEE_OTHER
    }

    "return 200 for an authorised user with an initialised journey" in new Setup {
      mockWithJourney(sessionId, Some(journey))

      requestWithAuthorisedUser(controller.show, requestWithSession) { result =>
        status(result) shouldBe OK
      }
    }

    s"return 303 and redirect to ${controllers.test.routes.TestSetupController.show()} for an authorised user without an initialised journey" in new Setup {
      mockWithJourney(sessionId, None)

      requestWithAuthorisedUser(controller.show, requestWithSession) { result =>
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.test.routes.TestSetupController.show().toString)
      }
    }
  }

  "submit" should {

    "return 303 for an unauthorised user" in new Setup {
      val result: Result = controller.submit()(FakeRequest())
      status(result) shouldBe SEE_OTHER
    }

    "return 400 for an authorised user with no data" in new Setup {
      mockWithJourney(sessionId, Some(journey))

      val request: FakeRequest[AnyContentAsFormUrlEncoded] = requestWithSession.withFormUrlEncodedBody(
        "sicSearch" -> ""
      )

      requestWithAuthorisedUser(controller.submit, request) { result =>
        status(result) shouldBe BAD_REQUEST
      }
    }

    "redirect to choose-business-activity when a search match is found" in new Setup {
      mockWithJourney(sessionId, Some(journey))

      when(mockSicSearchService.search(any(), any(), any(), any())(any[HeaderCarrier]()))
        .thenReturn(Future.successful(2))

      val request: FakeRequest[AnyContentAsFormUrlEncoded] = requestWithSession.withFormUrlEncodedBody(
        "sicSearch" -> sicCode
      )

      requestWithAuthorisedUser(controller.submit, request) {
        result =>
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some("/sic-search/choose-business-activity")
      }
    }

    s"return a 200 and render the sicSearch page when 0 results are returned from search" in new Setup {
      mockWithJourney(sessionId, Some(journey))

      val request: FakeRequest[AnyContentAsFormUrlEncoded] = requestWithSession.withFormUrlEncodedBody(
        "sicSearch" -> query
      )

      when(mockSicSearchService.search(eqTo(sessionId), eqTo(query), any(), any())(any[HeaderCarrier]()))
        .thenReturn(Future.successful(0))

      requestWithAuthorisedUser(controller.submit, request) {
        result =>
          status(result) shouldBe OK

      }
    }

    s"return a 303 and redirect to ${routes.ConfirmationController.show()} when 1 result is returned from search" in new Setup {
      mockWithJourney(sessionId, Some(journey))

      val request: FakeRequest[AnyContentAsFormUrlEncoded] = requestWithSession.withFormUrlEncodedBody(
        "sicSearch" -> query
      )

      when(mockSicSearchService.search(eqTo(sessionId), eqTo(query), any(), any())(any[HeaderCarrier]()))
        .thenReturn(Future.successful(1))

      requestWithAuthorisedUser(controller.submit, request) {
        result =>
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(routes.ConfirmationController.show().toString)
      }
    }
  }
}
