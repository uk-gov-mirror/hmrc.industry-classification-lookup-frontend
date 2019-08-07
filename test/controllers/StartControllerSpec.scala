/*
 * Copyright 2019 HM Revenue & Customs
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
import helpers.UnitTestSpec
import helpers.auth.AuthHelpers
import helpers.mocks.{MockAppConfig, MockMessages}
import models.setup.{Identifiers, JourneyData, JourneySetup}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.i18n.MessagesApi
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import services.{JourneyService, SicSearchService}
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.Future

class StartControllerSpec extends UnitTestSpec with MockAppConfig with MockMessages with AuthHelpers {
  val authConnector: AuthConnector = mockAuthConnector

  class Setup {
    def controller = new StartController {
      override val sicSearchService: SicSearchService = mockSicSearchService
      override implicit val appConfig: AppConfig = mockAppConfig
      override val journeyService: JourneyService = mockJourneyService
      override val loginURL: String = "test/login/url"

      override def messagesApi: MessagesApi = MockMessages

      override def authConnector: AuthConnector = mockAuthConnector
    }
  }

  val journeyId = "some-journey-Id"
  val sessionId = "session-12345"
  val requestWithSession: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSessionId(sessionId)

  val identifiers = Identifiers(journeyId, sessionId)

  "startJourney" should {
    "redirect to the search page" when {
      "no sic-codes in journey setup" in new Setup {
        val journeyData = JourneyData(identifiers, "redirectUrl", JourneySetup(), LocalDateTime.now())
        val url = routes.ChooseActivityController.show(journeyId).url

        when(mockJourneyService.getJourney(ArgumentMatchers.any())) thenReturn Future.successful(journeyData)

        AuthHelpers.requestWithAuthorisedUser(controller.startJourney(journeyId), requestWithSession){
          result =>
            status(result) mustBe 303
            redirectLocation(result) mustBe Some(url)
        }
      }
    }

    "redirect to the confirmation page" when {
      "there are sic-codes in journey setup" in new Setup {
        val journeySetup = JourneySetup(sicCodes = Seq("12345"))
        val journeyData = JourneyData(identifiers, "redirectUrl", journeySetup, LocalDateTime.now())
        val url = routes.ConfirmationController.show(journeyId).url

        when(mockJourneyService.getJourney(ArgumentMatchers.any())) thenReturn Future.successful(journeyData)

        AuthHelpers.requestWithAuthorisedUser(controller.startJourney(journeyId), requestWithSession){
          result =>
            status(result) mustBe 303
            redirectLocation(result) mustBe Some(url)
        }
      }
    }
  }
}
