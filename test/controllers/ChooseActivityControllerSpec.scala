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
import models._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import play.api.i18n.MessagesApi
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{JourneyService, SicSearchService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future

class ChooseActivityControllerSpec extends ControllerSpec with AuthBuilders {

  trait Setup {
    val controller: ChooseActivityController = new ChooseActivityController {
      override val sicSearchService: SicSearchService = mockSicSearchService
      override val authConnector: AuthConnector = mockAuthConnector
      implicit val messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]
      override val journeyService: JourneyService = mockJourneyService
    }

    resetMocks()
  }

  val sessionId = "session-12345"
  val requestWithSession: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders(
    SessionKeys.sessionId -> sessionId
  )

  val SECTOR_A = "A"
  val query = "testQuery"
  val sicCode = SicCode("12345678", "Test Description")
  val sicCode2 = SicCode("12345679", "Test Description2")
  val journey: String = Journey.QUERY_BUILDER

  val searchResults = SearchResults(query, 1, List(sicCode), List(Sector(SECTOR_A, "Fake Sector", 1)))
  val noSearchResults = SearchResults(query, 0, List(), List())
  val multipleSearchResults = SearchResults(query, 2, List(sicCode,sicCode2), List(Sector("A", "Fake Sector", 1), Sector("B", "Faker sector", 1)))
  val sicStore = SicStore("TestId", journey, Some(searchResults))

  "show" should {

    "return a 303 for an authorised user when the sic code is found and redirect to confirmation page" in new Setup {
      mockWithJourney(sessionId, Some(journey))

      when(mockSicSearchService.retrieveSearchResults(any())(any()))
        .thenReturn(Future.successful(Some(searchResults)))
      when(mockSicSearchService.insertChoice(any(), any())(any()))
        .thenReturn(Future.successful(true))

      requestWithAuthorisedUser(controller.show, requestWithSession) {
        (response: Future[Result]) =>
          status(response) shouldBe SEE_OTHER
          response.header.headers("Location") shouldBe "/sic-search/confirm-business-activities"
      }
    }

    "return a 303 for an authorised user with the sic code isn't found and redirect to search page" in new Setup {
      mockWithJourney(sessionId, Some(journey))

      when(mockSicSearchService.retrieveSearchResults(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      requestWithAuthorisedUser(controller.show, requestWithSession) {
        (response: Future[Result]) =>
          status(response) shouldBe SEE_OTHER
          response.header.headers("Location") shouldBe "/sic-search/enter-keywords"
      }
    }

    "return a 303 for an authorised user with multiple sic codes being returned and show the choose activity page" in new Setup {
      mockWithJourney(sessionId, Some(journey))

      when(mockSicSearchService.retrieveSearchResults(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(multipleSearchResults)))

      requestWithAuthorisedUser(controller.show, requestWithSession) {
        (response: Future[Result]) =>
          status(response) shouldBe OK
      }
    }

    s"return a 303 and redirect to ${routes.SicSearchController.show()} when no search results are found" in new Setup {
      mockWithJourney(sessionId, Some(journey))

      when(mockSicSearchService.retrieveSearchResults(any())(any()))
        .thenReturn(Future.successful(Some(noSearchResults)))

      requestWithAuthorisedUser(controller.show, requestWithSession) { result =>
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.SicSearchController.show().toString)
      }
    }

    "return a 303 for an authorised user without a sic search" in new Setup {
      mockWithJourney(sessionId, Some(journey))

      when(mockSicSearchService.retrieveSearchResults(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      requestWithAuthorisedUser(controller.show, requestWithSession) {
        (response: Future[Result]) =>
          status(response) shouldBe SEE_OTHER
      }
    }

    "should return a 303 for an unauthorised user" in new Setup {
      val result = controller.show()(FakeRequest())
      status(result) shouldBe SEE_OTHER
    }
  }

  "Submit" should {

    "return a 303 for an unauthorised user" in new Setup {
      val result = controller.submit()(FakeRequest())
      status(result) shouldBe SEE_OTHER
    }

    "return the choices list page for an authorised user with a selected option" in new Setup {
      mockWithJourney(sessionId, Some(journey))

      when(mockSicSearchService.retrieveSearchResults(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(searchResults)))

      when(mockSicSearchService.insertChoice(ArgumentMatchers.anyString(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(true))

      val request: FakeRequest[AnyContentAsFormUrlEncoded] = requestWithSession.withFormUrlEncodedBody(
        "code" -> "12345678"
      )

      requestWithAuthorisedUser(controller.submit, request) {
        result =>
          status(result) shouldBe SEE_OTHER
      }
    }

    "return a 400 for an authorised user with no option selected" in new Setup {
      mockWithJourney(sessionId, Some(journey))

      when(mockSicSearchService.retrieveSearchResults(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(searchResults)))

      val request: FakeRequest[AnyContentAsFormUrlEncoded] = requestWithSession.withFormUrlEncodedBody(
        "code" -> ""
      )

      requestWithAuthorisedUser(controller.submit, request) { result =>
        status(result) shouldBe BAD_REQUEST
      }
    }
  }

  "filter" should {

    s"return a 303 and redirect to ${routes.ChooseActivityController.show()} when search results are found" in new Setup {
      mockWithJourney(sessionId, Some(journey))

      when(mockSicSearchService.retrieveSearchResults(eqTo(sessionId))(any()))
        .thenReturn(Future.successful(Some(searchResults)))

      when(mockSicSearchService.search(eqTo(sessionId), eqTo(query), any(), any())(any()))
        .thenReturn(Future.successful(1))

      requestWithAuthorisedUser(controller.filter(SECTOR_A), requestWithSession) {
        result =>
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(routes.ChooseActivityController.show().toString)
      }
    }

    s"return a 303 and redirect to ${routes.SicSearchController.show()} when no search results are found" in new Setup {
      mockWithJourney(sessionId, Some(journey))

      when(mockSicSearchService.retrieveSearchResults(eqTo(sessionId))(any()))
        .thenReturn(Future.successful(None))

      requestWithAuthorisedUser(controller.filter(SECTOR_A), requestWithSession) {
        result =>
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(routes.SicSearchController.show().toString)
      }
    }
  }
}
