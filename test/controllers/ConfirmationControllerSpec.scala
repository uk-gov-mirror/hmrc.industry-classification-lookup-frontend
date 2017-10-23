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

import play.api.i18n.MessagesApi
import services.SicSearchService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.test.WithFakeApplication
import builders.AuthBuilder._
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import play.api.mvc.{AnyContent, AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Request}
import play.api.test.FakeRequest
import repositories.models.{SicCode, SicStore}
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future

class ConfirmationControllerSpec extends ControllerSpec with WithFakeApplication {

  trait Setup {
    val controller: ConfirmationController = new ConfirmationController {
      val sicSearchService: SicSearchService = mockSicSearchService
      val authConnector: AuthConnector = mockAuthConnector
      val messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]
    }
  }

  val sessionId = "session-id-12345"

  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders(SessionKeys.sessionId -> sessionId)

  val sicStore = SicStore(
    sessionId,
    SicCode("12345", "some description"),
    Some(List(SicCode("12345", "some description")))
  )

  "show" should {

    "return a 200 when a SicStore is returned from mongo" in new Setup {

      when(mockSicSearchService.retrieveSicStore(any()))
        .thenReturn(Future.successful(Some(sicStore)))

      showWithAuthorisedUser(controller.show, mockAuthConnector){
        result =>
          status(result) shouldBe 200
      }
    }

    "return a 303 when a SicStore ir not found in mongo" in new Setup {

      when(mockSicSearchService.retrieveSicStore(any()))
        .thenReturn(Future.successful(None))

      showWithAuthorisedUser(controller.show, mockAuthConnector){
        result =>
          status(result) shouldBe 303
      }
    }
  }

  "submit" should {

    "return a 200 when the form field 'addAnother' is no" in new Setup {

      val fakeRequestWithFormBody: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest.withFormUrlEncodedBody("addAnother" -> "no")

      when(mockSicSearchService.retrieveSicStore(any()))
        .thenReturn(Future.successful(Some(sicStore)))

      submitWithAuthorisedUser(controller.submit, mockAuthConnector, fakeRequestWithFormBody){
        result =>
          status(result) shouldBe 200
      }
    }

    "return a 303 when the form field 'addAnother' is yes" in new Setup {

      val fakeRequestWithFormBody: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest.withFormUrlEncodedBody("addAnother" -> "yes")

      when(mockSicSearchService.retrieveSicStore(any()))
        .thenReturn(Future.successful(Some(sicStore)))

      submitWithAuthorisedUser(controller.submit, mockAuthConnector, fakeRequestWithFormBody){
        result =>
          status(result) shouldBe 303
      }
    }
  }
}
