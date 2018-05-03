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

package controllers.internal

import helpers.auth.AuthHelpers
import helpers.{UnitTestFakeApp, UnitTestSpec}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import services.JourneySetupService
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.Future

class ApiControllerSpec extends UnitTestSpec with UnitTestFakeApp with AuthHelpers {
  val authConnector: AuthConnector = mockAuthConnector

  val validRequestedJson = Json.parse(
    """{
      | "redirectUrl": "test/url",
      | "customMessages": {
      |   "summary": {
      |     "heading": "value1",
      |     "lead": "value2"
      |   }
      | }
      |}""".stripMargin)
  val validRequestedJsonWithoutCustomMessages = Json.parse(
    """{
      | "redirectUrl": "test/url"
      |}""".stripMargin)

  implicit val requestWithSessionId: FakeRequest[JsValue] = FakeRequest()
    .withSessionId("test-session-id")
    .withBody(validRequestedJson)

  trait Setup {
    val controller: ApiController = new ApiController {
      override val loginURL = "login/url"

      override def authConnector: AuthConnector = mockAuthConnector
      val journeySetupService: JourneySetupService = mockJourneySetupService
    }
  }

  "journeyInitialisation" should {
    "return 200 with json" when {
      val expectedJsonResponse = Json.obj(
        "journeyStartUri" -> "/test/uri/",
        "fetchResultsUri" -> "/test/uri/"
      )

      "journey is initialised with custom messages" in new Setup {
        when(mockJourneySetupService.initialiseJourney(ArgumentMatchers.any()))
          .thenReturn(Future(expectedJsonResponse))

        postWithAuthorisedUser(controller.journeyInitialisation(), requestWithSessionId) { result =>
          status(result) mustBe OK
          contentAsJson(result) mustBe expectedJsonResponse
        }
      }

      "journey is initialised without custom messages" in new Setup {
        val fakeRequest: FakeRequest[JsValue] = FakeRequest()
          .withSessionId("test-session-id")
          .withBody(validRequestedJsonWithoutCustomMessages)

        when(mockJourneySetupService.initialiseJourney(ArgumentMatchers.any()))
          .thenReturn(Future(expectedJsonResponse))

        postWithAuthorisedUser(controller.journeyInitialisation(), fakeRequest) { result =>
          status(result) mustBe OK
          contentAsJson(result) mustBe expectedJsonResponse
        }
      }
    }

    "return 400 when payload provided is not valid" in new Setup {
      val invalidRequest: FakeRequest[JsValue] = FakeRequest()
        .withSessionId("test-session-id")
        .withBody(
          Json.parse(
            """{
              | "customMessages": {
              |   "summary": {
              |     "heading": "value1",
              |     "lead": "value2"
              |   }
              | }
              |}""".stripMargin)
        )

      postWithAuthorisedUser(controller.journeyInitialisation(), invalidRequest) { result =>
        status(result) mustBe BAD_REQUEST
        contentAsString(result) must include("redirectUrl")
      }
    }

    "return 412 when sessionId is missing" in new Setup {
      val requestWithoutSessionId: FakeRequest[JsValue] = FakeRequest().withBody(validRequestedJson)

      postWithAuthorisedUser(controller.journeyInitialisation(), requestWithoutSessionId) { result =>
        status(result) mustBe PRECONDITION_FAILED
      }
    }

    "return 500 when something went wrong" in new Setup {
      when(mockJourneySetupService.initialiseJourney(ArgumentMatchers.any()))
        .thenReturn(Future.failed(new RuntimeException("something went wrong")))

      postWithAuthorisedUser(controller.journeyInitialisation(), requestWithSessionId) { result =>
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "return 403 when the requester is not authorised" in new Setup {
      postWithUnauthorisedUser(controller.journeyInitialisation(), requestWithSessionId) { result =>
        status(result) mustBe FORBIDDEN
      }
    }
  }
}
