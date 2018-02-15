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
import models._
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import play.api.test.FakeRequest
import services.{JourneyService, SicSearchService}
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ConfirmationControllerSpec extends UnitTestSpec with UnitTestFakeApp {

  class Setup {
    val controller: ConfirmationController = new ConfirmationController with I18nSupport {
      override val loginURL = "/test/login"

      override implicit val appConfig: AppConfig      = app.injector.instanceOf[AppConfig]
      override val sicSearchService: SicSearchService = mockSicSearchService
      override val authConnector: AuthConnector       = mockAuthConnector
      override val messagesApi: MessagesApi           = testMessagesApi
      override val journeyService: JourneyService     = mockJourneyService
    }
  }

  val sessionId = "session-12345"
  val requestWithSessionId: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSessionId(sessionId)

  val sicCodeCode = "12345"
  val sicCodeDescription = "some description"
  val sicCode = SicCode(sicCodeCode, sicCodeDescription)
  val journey: String = Journey.QUERY_BUILDER
  val searchResults = SearchResults("testQuery", 1, List(sicCode), List(Sector("A", "Fake Sector", 1)))

  val sicStore = SicStore(
    sessionId,
    journey,
    Some(searchResults),
    Some(List(sicCode))
  )

  val sicStoreNoChoices = SicStore(sessionId, journey, Some(searchResults), None)
  val sicStoreEmptyChoiceList = SicStore(sessionId, journey, Some(searchResults), Some(List()))

  "show" should {

    "return a 200 when a SicStore is returned from mongo" in new Setup {
      mockWithJourney(sessionId, Some(journey))

      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(Some(List(sicCode))))

      AuthHelpers.showWithAuthorisedUser(controller.show, requestWithSessionId){
        result =>
          status(result) mustBe 200
      }
    }

    "return a 303 when previous choices are not found in mongo" in new Setup {
      mockWithJourney(sessionId, Some(journey))

      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(None))

      AuthHelpers.showWithAuthorisedUser(controller.show, requestWithSessionId){
        result =>
          status(result) mustBe 303
      }
    }
  }

  "submit" should {

    "return a 200 when the form field 'addAnother' is no" in new Setup {
      mockWithJourney(sessionId, Some(journey))

      val request: FakeRequest[AnyContentAsFormUrlEncoded] = requestWithSessionId.withFormUrlEncodedBody("addAnother" -> "no")

      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(Some(List(sicCode))))

      AuthHelpers.submitWithAuthorisedUser(controller.submit, request){
        result =>
          status(result) mustBe 200
      }
    }

    "return a 303 when the form field 'addAnother' is yes" in new Setup {
      mockWithJourney(sessionId, Some(journey))

      val request: FakeRequest[AnyContentAsFormUrlEncoded] = requestWithSessionId.withFormUrlEncodedBody("addAnother" -> "yes")

      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(Some(List(sicCode))))

      AuthHelpers.submitWithAuthorisedUser(controller.submit, request){
        result =>
          status(result) mustBe 303
      }
    }

    "return a 200 and render end of journey page when 4 choices have already been made" in new Setup {
      mockWithJourney(sessionId, Some(journey))

      when(mockSicSearchService.retrieveChoices(eqTo(sessionId))(any()))
        .thenReturn(Future.successful(Some(List(sicCode, sicCode, sicCode, sicCode, sicCode))))

      AuthHelpers.submitWithAuthorisedUser(controller.submit, requestWithSessionId.withFormUrlEncodedBody()){ result =>
        status(result) mustBe OK
      }
    }

    "return a 400 when an empty form is submitted" in new Setup {
      mockWithJourney(sessionId, Some(journey))

      when(mockSicSearchService.retrieveChoices(eqTo(sessionId))(any()))
        .thenReturn(Future.successful(Some(List(sicCode, sicCode, sicCode))))

      val request: FakeRequest[AnyContentAsFormUrlEncoded] = requestWithSessionId.withFormUrlEncodedBody(
        "addAnother" -> ""
      )

      AuthHelpers.submitWithAuthorisedUser(controller.submit, request){ result =>
        status(result) mustBe BAD_REQUEST
      }
    }
  }

  "removeChoice" should {

    "return a 200 when the supplied sic code is removed" in new Setup {
      mockWithJourney(sessionId, Some(journey))

      when(mockSicSearchService.removeChoice(any(), any())(any()))
        .thenReturn(Future.successful(true))

      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(Some(List(sicCode))))

      AuthHelpers.showWithAuthorisedUser(controller.removeChoice(sicCodeCode), requestWithSessionId){
        result =>
          status(result) mustBe OK
      }
    }
  }

  "withCurrentUsersChoices" should {

    "return a 303 and redirect to SicSearch when a SicStore does not exist" in new Setup {
      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(None))

      val f: List[SicCode] => Future[Result] = _ => Future.successful(Ok)
      val result: Future[Result] = controller.withCurrentUsersChoices(sessionId)(f)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some("/sic-search/enter-keywords")
    }

    "return a 303 and redirect to SicSearch when a SicStore does exist but does not contain any choices" in new Setup {
      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(Some(List())))

      val f: List[SicCode] => Future[Result] = _ => Future.successful(Ok)
      val result: Future[Result] = controller.withCurrentUsersChoices(sessionId)(f)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some("/sic-search/enter-keywords")
    }

    "return a 200 when a SicStore does exist and the choices list is populated" in new Setup {
      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(Some(List(sicCode))))

      val f: List[SicCode] => Future[Result] = _ => Future.successful(Ok)
      val result: Future[Result] = controller.withCurrentUsersChoices(sessionId)(f)

      status(result) mustBe OK
    }
  }
}
