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
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import services.{JourneyService, SicSearchService}
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.Future

class ICLControllerSpec extends UnitTestSpec with UnitTestFakeApp {

  trait Setup {
    val controller: ICLController = new ICLController with I18nSupport {
      override val loginURL = "/test/login"

      override implicit val appConfig: AppConfig        = app.injector.instanceOf[AppConfig]
      override val journeyService: JourneyService       = mockJourneyService
      override val authConnector: AuthConnector         = mockAuthConnector
      override val messagesApi: MessagesApi             = testMessagesApi
      override val sicSearchService: SicSearchService   = mockSicSearchService
    }
  }

  val sessionId = "session-12345"
  val requestWithSessionId: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSessionId(sessionId)

  "withSessionId" should {
    "supply the sessionId to the function parameter and return the supllied result" in new Setup {
      val suppliedFunction: String => Future[Result] = _ => Future.successful(Ok)
      val result: Future[Result] = controller.withSessionId(suppliedFunction)(requestWithSessionId)

      awaitAndAssert(controller.withSessionId(suppliedFunction)(requestWithSessionId)) {
        _ mustBe await(suppliedFunction(sessionId))
      }
    }

    "throw an exception when the request does not contain a session Id" in new Setup {
      val suppliedFunction: String => Future[Result] = _ => Future.successful(Ok)
      val result: RuntimeException = intercept[RuntimeException](await(controller.withSessionId(suppliedFunction)(FakeRequest())))

      result.getMessage mustBe "No session id found in request"
    }
  }
}
