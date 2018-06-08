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

import java.time.LocalDateTime

import config.AppConfig
import helpers.{UnitTestFakeApp, UnitTestSpec}
import models._
import models.setup.{Identifiers, JourneyData, JourneySetup}
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

  val journeyId = "testJourneyId"
  val sessionId = "session-12345"
  val identifiers = Identifiers(journeyId, sessionId)
  val journeyData = JourneyData(identifiers, "redirectUrl", None, JourneySetup(), LocalDateTime.now())

  val requestWithSessionId: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSessionId(sessionId)

  val sicCodeCode = "12345"
  val sicCodeDescription = "some description"
  val sicCode = SicCode(sicCodeCode, sicCodeDescription)
  val sicCodeChoice = SicCodeChoice(sicCode, List("fake item"))
  val searchResults = SearchResults("testQuery", 1, List(sicCode), List(Sector("A", "Fake Sector", 1)))

  "show" should {

    "return a 200 when a SicStore is returned from mongo" in new Setup {

      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(Some(List(sicCodeChoice))))

      when(mockJourneyService.getJourney(any())) thenReturn Future.successful(journeyData)

      AuthHelpers.showWithAuthorisedUser(controller.show(journeyId), requestWithSessionId){
        result =>
          status(result) mustBe 200
      }
    }

    "return a 303 when previous choices are not found in mongo" in new Setup {

      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(None))

      when(mockJourneyService.getJourney(any())) thenReturn Future.successful(journeyData)

      AuthHelpers.showWithAuthorisedUser(controller.show(journeyId), requestWithSessionId){
        result =>
          status(result) mustBe 303
      }
    }
  }

  "submit" should {
    "redirect out of the service to the redirect url setup via the api" in new Setup {

      when(mockSicSearchService.retrieveChoices(eqTo(sessionId))(any()))
        .thenReturn(Future.successful(Some(List(sicCodeChoice, sicCodeChoice, sicCodeChoice, sicCodeChoice))))

      when(mockJourneyService.getJourney(any())) thenReturn Future.successful(journeyData)

      when(mockJourneyService.getRedirectUrl(any())) thenReturn Future.successful("redirect-url")

      AuthHelpers.submitWithAuthorisedUser(controller.submit(journeyId), requestWithSessionId.withFormUrlEncodedBody()){ result =>
        status(result) mustBe 303
        redirectLocation(result) mustBe Some("redirect-url")
      }
    }

    "return a 400 when more than 4 choices have been made" in new Setup {
      when(mockSicSearchService.retrieveChoices(eqTo(sessionId))(any()))
        .thenReturn(Future.successful(Some(List(sicCodeChoice, sicCodeChoice, sicCodeChoice, sicCodeChoice, sicCodeChoice))))

      when(mockJourneyService.getJourney(any())) thenReturn Future.successful(journeyData)

      val request: FakeRequest[AnyContentAsFormUrlEncoded] = requestWithSessionId.withFormUrlEncodedBody()

      AuthHelpers.submitWithAuthorisedUser(controller.submit(journeyId), request){ result =>
        status(result) mustBe BAD_REQUEST
      }
    }
  }

  "withCurrentUsersChoices" should {

    "return a 303 and redirect to SicSearch when a SicStore does not exist" in new Setup {
      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(None))

      val f: List[SicCodeChoice] => Future[Result] = _ => Future.successful(Ok)
      val result: Future[Result] = controller.withCurrentUsersChoices(identifiers)(f)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.ChooseActivityController.show(journeyId).url)
    }

    "return a 303 and redirect to SicSearch when a SicStore does exist but does not contain any choices" in new Setup {
      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(Some(List())))

      val f: List[SicCodeChoice] => Future[Result] = _ => Future.successful(Ok)
      val result: Future[Result] = controller.withCurrentUsersChoices(identifiers)(f)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.ChooseActivityController.show(journeyId).url)
    }

    "return a 200 when a SicStore does exist and the choices list is populated" in new Setup {
      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(Some(List(sicCodeChoice))))

      val f: List[SicCodeChoice] => Future[Result] = _ => Future.successful(Ok)
      val result: Future[Result] = controller.withCurrentUsersChoices(identifiers)(f)

      status(result) mustBe OK
    }
  }
}
