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
import helpers.auth.AuthHelpers
import helpers.{UnitTestFakeApp, UnitTestSpec}
import models._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import services.{JourneyService, SicSearchService}
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.Future

class ChooseActivityControllerSpec extends UnitTestSpec with UnitTestFakeApp {

  class Setup extends CodeMocks with AuthHelpers {
    override val authConnector = mockAuthConnector

    val controller: ChooseActivityController = new ChooseActivityController with I18nSupport {
      override val loginURL = "/test/login"

      override implicit val appConfig: AppConfig        = testAppConfig
      override val sicSearchService: SicSearchService   = mockSicSearchService
      override val authConnector: AuthConnector         = mockAuthConnector
      implicit val messagesApi: MessagesApi             = testMessagesApi
      override val journeyService: JourneyService       = mockJourneyService
    }

    val requestWithSession: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSessionId(sessionId)
  }

  val sessionId = "session-12345"

  val SECTOR_A = "A"
  val query = "testQuery"
  val sicCode = SicCode("12345678", "Test Description")
  val sicCode2 = SicCode("12345679", "Test Description2")
  val journey: String = Journey.QUERY_BUILDER
  val dataSet: String = Journey.HMRC_SIC_8

  val searchResults = SearchResults(query, 1, List(sicCode), List(Sector(SECTOR_A, "Fake Sector", 1)))
  val noSearchResults = SearchResults(query, 0, List(), List())
  val multipleSearchResults = SearchResults(query, 2, List(sicCode,sicCode2), List(Sector("A", "Fake Sector", 1), Sector("B", "Faker sector", 1)))
  val sicStore = SicStore("TestId", journey, dataSet, Some(searchResults))

  "show without results" should {
    "return a 303 for an unauthorised user" in new Setup {
      showWithUnauthorisedUser(controller.show(), FakeRequest()) {
        response =>
          status(response) mustBe SEE_OTHER
      }
    }

    "return a 200 with a new search for an authorised user" in new Setup {
      when(mockJourneyService.retrieveJourney(any())(any()))
        .thenReturn(Future.successful(Some((journey, dataSet))))

      requestWithAuthorisedUser(controller.show(), requestWithSession) {
        (response: Future[Result]) =>
          status(response) mustBe OK
          val document = Jsoup.parse(contentAsString(response))
          document.getElementById("sicSearch").attr("name") mustBe "sicSearch"
          document.getElementById("sicSearch").attr("value") mustBe ""
          an[Exception] mustBe thrownBy(document.getElementById("result-count").text)
          an[Exception] mustBe thrownBy(document.getElementById("no-result").text)
      }
    }
  }

  "show with results" should {
    "return a 200 with performed search returning nothing for an authorised user" in new Setup {
      when(mockJourneyService.retrieveJourney(any())(any()))
        .thenReturn(Future.successful(Some((journey, dataSet))))

      when(mockSicSearchService.retrieveSearchResults(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      requestWithAuthorisedUser(controller.show(Some(true)), requestWithSession) {
        (response: Future[Result]) =>
          status(response) mustBe OK
          val document = Jsoup.parse(contentAsString(response))
          document.getElementById("sicSearch").attr("name") mustBe "sicSearch"
          document.getElementById("sicSearch").attr("value") mustBe ""
          an[Exception] mustBe thrownBy(document.getElementById("result-count").text)
          an[Exception] mustBe thrownBy(document.getElementById("no-result").text)
      }
    }

    "return a 303 for an authorised user when the sic code is found and redirect to confirmation page" in new Setup {
      when(mockJourneyService.retrieveJourney(any())(any()))
        .thenReturn(Future.successful(Some((journey, dataSet))))

      when(mockSicSearchService.retrieveSearchResults(any())(any()))
        .thenReturn(Future.successful(Some(searchResults)))

      requestWithAuthorisedUser(controller.show(Some(true)), requestWithSession) {
        (response: Future[Result]) =>
          status(response) mustBe SEE_OTHER
          redirectLocation(response) mustBe Some("/sic-search/confirm-business-activities")
      }
    }

    "return a 200 for an authorised user with multiple sic codes being returned and show the choose activity page and verify sicSearch bar is displayed" in new Setup {
      when(mockJourneyService.retrieveJourney(any())(any()))
        .thenReturn(Future.successful(Some((journey, dataSet))))

      when(mockSicSearchService.retrieveSearchResults(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(multipleSearchResults)))

      requestWithAuthorisedUser(controller.show(Some(true)), requestWithSession) {
        (response: Future[Result]) =>
          status(response) mustBe OK
          val document = Jsoup.parse(contentAsString(response))
          document.getElementById("sicSearch").attr("name") mustBe "sicSearch"
          document.getElementById("sicSearch").attr("value") mustBe multipleSearchResults.query
          an[Exception] mustBe thrownBy(document.getElementById("no-result").text)
          document.getElementById("result-count").text mustBe multipleSearchResults.numFound.toString
      }
    }

    s"return a 200 and show the choose activity page with no search results found and verify sicSearch bar is displayed" in new Setup {
      when(mockJourneyService.retrieveJourney(any())(any()))
        .thenReturn(Future.successful(Some((journey, dataSet))))

      when(mockSicSearchService.retrieveSearchResults(any())(any()))
        .thenReturn(Future.successful(Some(noSearchResults)))

      requestWithAuthorisedUser(controller.show(Some(true)), requestWithSession) { result =>
        status(result) mustBe OK
        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("sicSearch").attr("name") mustBe "sicSearch"
        document.getElementById("sicSearch").attr("value") mustBe noSearchResults.query
        document.getElementById("no-result").text mustBe "0"
        an[Exception] mustBe thrownBy(document.getElementById("result-count").text)
      }
    }
  }

  "Submit a search" should {
    "return a 303 with the search results page" in new Setup {
      when(mockJourneyService.retrieveJourney(any())(any()))
        .thenReturn(Future.successful(Some((journey, dataSet))))

      when(mockSicSearchService.search(eqTo(sessionId), eqTo(query), any(), any(), any())(any()))
        .thenReturn(Future.successful(multipleSearchResults.numFound))

      val request: FakeRequest[AnyContentAsFormUrlEncoded] = requestWithSession.withFormUrlEncodedBody(
        "sicSearch" -> query
      )

      requestWithAuthorisedUser(controller.submit(Some("test")), request) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.ChooseActivityController.show(Some(true)).url)
      }
    }

    "return a 303 with the choices list page for an authorised user when the search returns only 1 result" in new Setup {
      when(mockJourneyService.retrieveJourney(any())(any()))
        .thenReturn(Future.successful(Some((journey, dataSet))))

      when(mockSicSearchService.search(eqTo(sessionId), eqTo(query), any(), any(), any())(any()))
        .thenReturn(Future.successful(searchResults.numFound))

      val request: FakeRequest[AnyContentAsFormUrlEncoded] = requestWithSession.withFormUrlEncodedBody(
        "sicSearch" -> query
      )

      requestWithAuthorisedUser(controller.submit(Some("test")), request) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.ConfirmationController.show().url)
      }
    }

    "return a 400 for an authorised user without a sic search" in new Setup {
      when(mockJourneyService.retrieveJourney(any())(any()))
        .thenReturn(Future.successful(Some((journey, dataSet))))

      when(mockSicSearchService.retrieveSearchResults(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      val request: FakeRequest[AnyContentAsFormUrlEncoded] = requestWithSession.withFormUrlEncodedBody(
        "sicSearch" -> ""
      )

      requestWithAuthorisedUser(controller.submit(Some("test")), requestWithSession) {
        (response: Future[Result]) =>
          status(response) mustBe BAD_REQUEST
      }
    }
  }

  "Submit an activity" should {
    "return a 303 for an unauthorised user" in new Setup {
      submitWithUnauthorisedUser(controller.submit(), FakeRequest().withFormUrlEncodedBody()) { request =>
        status(request) mustBe SEE_OTHER
      }
    }

    "return the choices list page for an authorised user with a selected option" in new Setup {
      when(mockJourneyService.retrieveJourney(any())(any()))
        .thenReturn(Future.successful(Some((journey, dataSet))))

      when(mockSicSearchService.retrieveSearchResults(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(searchResults)))

      when(mockSicSearchService.insertChoice(ArgumentMatchers.anyString(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(true))

      val request: FakeRequest[AnyContentAsFormUrlEncoded] = requestWithSession.withFormUrlEncodedBody(
        "code[0]" -> "12345678"
      )

      requestWithAuthorisedUser(controller.submit(), request) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.ConfirmationController.show().url)
      }
    }

    "return a 400 for an authorised user with no option selected" in new Setup {
      when(mockJourneyService.retrieveJourney(any())(any()))
        .thenReturn(Future.successful(Some((journey, dataSet))))

      when(mockSicSearchService.retrieveSearchResults(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(searchResults)))

      val request: FakeRequest[AnyContentAsFormUrlEncoded] = requestWithSession.withFormUrlEncodedBody(
        "code" -> ""
      )

      requestWithAuthorisedUser(controller.submit(), request) { result =>
        status(result) mustBe BAD_REQUEST
      }
    }
  }

  "filter" should {
    s"return a 303 and redirect to ${routes.ChooseActivityController.show()} when search results are found" in new Setup {
      when(mockJourneyService.retrieveJourney(any())(any()))
        .thenReturn(Future.successful(Some((journey, dataSet))))

      when(mockSicSearchService.retrieveSearchResults(eqTo(sessionId))(any()))
        .thenReturn(Future.successful(Some(searchResults)))

      when(mockSicSearchService.search(eqTo(sessionId), eqTo(query), any(), any(), any())(any()))
        .thenReturn(Future.successful(1))

      requestWithAuthorisedUser(controller.filter(SECTOR_A), requestWithSession) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.ChooseActivityController.show(Some(true)).url)
      }
    }

    s"return a 303 and redirect to ${routes.ChooseActivityController.show()} when no search results are found" in new Setup {
      when(mockJourneyService.retrieveJourney(any())(any()))
        .thenReturn(Future.successful(Some((journey, dataSet))))

      when(mockSicSearchService.retrieveSearchResults(eqTo(sessionId))(any()))
        .thenReturn(Future.successful(Some(noSearchResults)))

      when(mockSicSearchService.search(eqTo(sessionId), eqTo(query), any(), any(), any())(any()))
        .thenReturn(Future.successful(0))

      requestWithAuthorisedUser(controller.filter(SECTOR_A), requestWithSession) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.ChooseActivityController.show(Some(true)).url)
      }
    }
  }
}
