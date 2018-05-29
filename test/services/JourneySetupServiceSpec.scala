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

package services

import java.time.LocalDateTime

import helpers.UnitTestSpec
import helpers.mocks.MockJourneyDataRepo
import models.setup.{Identifiers, JourneyData, JourneySetup}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.libs.json.Json

import scala.concurrent.Future

class JourneySetupServiceSpec extends UnitTestSpec with MockJourneyDataRepo {

  val now = LocalDateTime.now

  val testService = new JourneyService {
    override val journeyDataRepository = mockJourneyDataRepo
  }

  val identifier = Identifiers(
    journeyId = "testJourneyId",
    sessionId = "testSessionId"
  )

  val journeyData = JourneyData(identifier, "/test/uri", None, JourneySetup(), now)

  val testJourneyData = JourneyData(
    identifiers          = identifier,
    redirectUrl         = "/test/uri",
    customMessages      = None,
    journeySetupDetails = JourneySetup(),
    lastUpdated         = now
  )

  "initialiseJourney" should {
    "return Json containing the start and fetch uri's" in {
      mockInitialiseJourney(success = true)

      assertAndAwait(testService.initialiseJourney(testJourneyData)) {
        _ mustBe Json.obj(
          "journeyStartUri" -> s"/sic-search/testJourneyId/search-standard-industry-classification-codes",
          "fetchResultsUri" -> s"/internal/testJourneyId/fetch-results"
        )
      }
    }
  }

  "getRedirectUrl" should {
    "return a redirect url" in {
      when(mockJourneyDataRepo.retrieveJourneyData(ArgumentMatchers.any()))
        .thenReturn(Future.successful(journeyData))

      assertAndAwait(testService.getRedirectUrl(identifier)) {
        _ mustBe "/test/uri"
      }
    }
  }

  "getSetupDetails" should {
    "return a JourneySetup model" in {
      when(mockJourneyDataRepo.retrieveJourneyData(ArgumentMatchers.any()))
        .thenReturn(Future.successful(journeyData))

      assertAndAwait(testService.getSetupDetails(identifier)) {
        _ mustBe JourneySetup()
      }
    }
  }
}
