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
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import org.mockito.Mockito._
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import org.mockito.ArgumentMatchers
import play.api.test.Helpers.redirectLocation
import repositories.models.SicCode
import services.SicSearchService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

class SicSearchControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication {
  val mockSicSearchService = mock[SicSearchService]
  val mockAuthConnector = mock[AuthConnector]

  trait Setup {
    val controller = new SicSearchCtrl {
      override val sicSearchService = mockSicSearchService
      override val authConnector = mockAuthConnector
      implicit val messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]
    }
  }

  "showing the sic search page" should {
    "return 303 for an unauthorised user" in new Setup {
      val result = controller.show()(FakeRequest())
      status(result) shouldBe SEE_OTHER
    }

    "return 200 for an authorised user" in new Setup {
      AuthBuilders.showWithAuthorisedUser(controller.show, mockAuthConnector) {
        (response: Future[Result]) =>
          status(response) shouldBe OK
      }
    }
  }

  "submitting the sic search page" should {
    "return 303 for an unauthorised user" in new Setup {
      val result = controller.submit()(FakeRequest())
      status(result) shouldBe SEE_OTHER
    }

    "return 400 for an authorised user with no data" in new Setup {
      val request = FakeRequest().withFormUrlEncodedBody(
        "search" -> ""
      )

      AuthBuilders.submitWithAuthorisedUser(controller.submit, mockAuthConnector, request) {
        (response: Future[Result]) =>
          status(response) shouldBe BAD_REQUEST
      }
    }

    "return the select sic code page when a sic code is found" in new Setup {
      val sicCode = "111"
      val request = FakeRequest().withFormUrlEncodedBody(
        "sicSearch" -> sicCode
      )
      val siccodemodel = SicCode(
        sicCode,
        "Test"
      )

      when(mockSicSearchService.lookupSicCode(ArgumentMatchers.eq(sicCode))(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Some(siccodemodel)))
      when(mockSicSearchService.updateSearchResults(ArgumentMatchers.anyString(), ArgumentMatchers.eq(siccodemodel)))
        .thenReturn(Future.successful(Some(siccodemodel)))

      AuthBuilders.submitWithAuthorisedUser(controller.submit, mockAuthConnector, request) {
        result =>
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some("/sic-search/choose-business-activity")
      }
    }

    "return 400 with no codes were found if the sic code was not found" in new Setup {
      val sicCode = "123"
      val request = FakeRequest().withFormUrlEncodedBody(
        "search" -> sicCode
      )

      when(mockSicSearchService.lookupSicCode(ArgumentMatchers.eq(sicCode))(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(None))

      AuthBuilders.submitWithAuthorisedUser(controller.submit, mockAuthConnector, request) {
        result =>
          status(result) shouldBe BAD_REQUEST
      }
    }
  }
}
