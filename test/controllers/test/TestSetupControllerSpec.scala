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

package controllers.test

import builders.AuthBuilders
import controllers.{ControllerSpec, MockMessages}
import models._
import play.api.i18n.MessagesApi
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.JourneyService
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class TestSetupControllerSpec extends ControllerSpec with AuthBuilders with MockMessages {

  override val mockMessagesAPI: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]

  trait Setup {
    val controller: TestSetupController = new TestSetupController {
      override val messagesApi: MessagesApi = mockMessagesAPI
      override val authConnector: AuthConnector = mockAuthConnector
      override val journeyService: JourneyService = mockJourneyService
    }
  }

  val sessionId = "session-12345"
  val requestWithSessionId: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSessionId(sessionId)

  val journeyName: String = Journey.QUERY_BUILDER
  val journey = Journey(sessionId, journeyName)

  val sicStore = SicStore(
    sessionId,
    journeyName,
    Some(SearchResults("test-query", 1, List(SicCode("19283746", "Search Sic Code Result Description")), List()))
  )

  "show" should {

    "return a 200 and render the SetupJourneyView page when a journey has already been initialised" in new Setup {

      when(mockJourneyService.retrieveJourney(eqTo(sessionId))(any()))
        .thenReturn(Future.successful(Some(journeyName)))

      requestWithAuthorisedUser(controller.show, requestWithSessionId){ result =>
        status(result) shouldBe 200
      }
    }

    "return a 200 and render the SetupJourneyView page when a journey has not been initialised" in new Setup {

      when(mockJourneyService.retrieveJourney(eqTo(sessionId))(any()))
        .thenReturn(Future.successful(None))

      requestWithAuthorisedUser(controller.show, requestWithSessionId){ result =>
        status(result) shouldBe 200
      }
    }
  }

  "submit" should {

    "return a 400 when the form is empty" in new Setup {
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = requestWithSessionId.withFormUrlEncodedBody(
        "journey" -> ""
      )

      requestWithAuthorisedUser(controller.submit, request){ result =>
        status(result) shouldBe 400
      }
    }

    s"return a 303 and redirect to ${controllers.routes.SicSearchController.show()} when a journey is initialised" in new Setup {
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = requestWithSessionId.withFormUrlEncodedBody(
        "journey" -> journeyName
      )

      when(mockJourneyService.upsertJourney(eqTo(journey))(any()))
        .thenReturn(Future.successful(sicStore))

      requestWithAuthorisedUser(controller.submit, request){ result =>
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SicSearchController.show().toString)
      }
    }
  }
}
