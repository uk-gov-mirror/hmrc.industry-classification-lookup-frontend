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
import models.{SearchResults, SicCode, SicStore}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import play.api.test.FakeRequest
import services.SicSearchService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future

class ChooseActivityControllerSpec extends ControllerSpec with WithFakeApplication {

  trait Setup {
    val controller: ChooseActivityController = new ChooseActivityController {
      override val sicSearchService: SicSearchService = mockSicSearchService
      override val authConnector: AuthConnector = mockAuthConnector
      implicit val messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]
    }

    resetMocks()
  }

  val sicCode = SicCode("12345678", "Test Description")
  val searchResults = SearchResults("testQuery", 1, List(sicCode))
  val sicStore = SicStore("TestId", searchResults, None)

  "Showing the choose activity page" should {

    "return a 200 for an authorised user with a sic search" in new Setup {
      when(mockSicSearchService.retrieveSearchResults(ArgumentMatchers.anyString()))
        .thenReturn(Future.successful(Some(searchResults)))
      AuthBuilders.showWithAuthorisedUser(controller.show, mockAuthConnector) {
        (response: Future[Result]) =>
          status(response) shouldBe OK
      }
    }

    "return a 303 for an authorised user without a sic search" in new Setup {
      when(mockSicSearchService.retrieveSearchResults(ArgumentMatchers.anyString()))
        .thenReturn(Future.successful(None))

      AuthBuilders.showWithAuthorisedUser(controller.show, mockAuthConnector) {
        (response: Future[Result]) =>
          status(response) shouldBe SEE_OTHER
      }
    }

    "should return a 303 for an unauthorised user" in new Setup {
      val result = controller.show()(FakeRequest())
      status(result) shouldBe SEE_OTHER
    }
  }

  "Submitting the choose activity page" should {

    "return a 303 for an unauthorised user" in new Setup {
      val result = controller.submit()(FakeRequest())
      status(result) shouldBe SEE_OTHER
    }

    "return the choices list page for an authorised user with a selected option" in new Setup {
      val code = "12345678"
      val request = FakeRequest().withFormUrlEncodedBody(
        "code" -> code
      )

      when(mockSicSearchService.retrieveSearchResults(ArgumentMatchers.anyString()))
        .thenReturn(Future.successful(Some(searchResults)))

      when(mockSicSearchService.insertChoice(ArgumentMatchers.anyString(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(true))

      AuthBuilders.submitWithAuthorisedUser(controller.submit, mockAuthConnector, request) {
        result =>
          status(result) shouldBe SEE_OTHER
      }
    }

    "return a 400 for an authorised user with no option selected" in new Setup {
      val code = ""
      val request = FakeRequest().withFormUrlEncodedBody(
        "code" -> code
      )

      when(mockSicSearchService.retrieveSearchResults(ArgumentMatchers.anyString()))
        .thenReturn(Future.successful(Some(searchResults)))

      AuthBuilders.submitWithAuthorisedUser(controller.submit, mockAuthConnector, request) {
        result =>
          status(result) shouldBe BAD_REQUEST
      }
    }
  }
}
