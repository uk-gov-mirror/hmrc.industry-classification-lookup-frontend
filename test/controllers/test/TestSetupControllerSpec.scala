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

package controllers.test

import builders.AuthBuilders
import controllers.{ControllerSpec, MockMessages}
import play.api.i18n.MessagesApi
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.test.FakeRequest
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import Journeys._

class TestSetupControllerSpec extends ControllerSpec with AuthBuilders with MockMessages {

  override val mockMessagesAPI: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]

  trait Setup {
    val controller: TestSetupController = new TestSetupController {
      override val messagesApi: MessagesApi = mockMessagesAPI
      override val authConnector: AuthConnector = mockAuthConnector
    }
  }

  "show" should {

    "return a 200 and render the SetupJourneyView page" in new Setup {
      showWithAuthorisedUser(controller.show, mockAuthConnector){ result =>
        status(result) shouldBe 200
      }
    }
  }

  "submit" should {

    "return a 400 when the form is empty" in new Setup {
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withFormUrlEncodedBody(
        "journey" -> ""
      )

      submitWithAuthorisedUser(controller.submit, mockAuthConnector, request){ result =>
        status(result) shouldBe 400
      }
    }

    "return a 200" in new Setup {
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withFormUrlEncodedBody(
        "journey" -> QUERY_PARSER
      )

      submitWithAuthorisedUser(controller.submit, mockAuthConnector, request){ result =>
        status(result) shouldBe 200
      }
    }
  }
}
