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

import helpers.UnitTestSpec
import models.{Journey, SicStore}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import repositories.SicStoreMongoRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class JourneyServiceSpec extends UnitTestSpec {

  trait Setup {
    val service: JourneyService = new JourneyService {
      override val sicStore: SicStoreMongoRepository = mockSicStoreRepo
    }
  }

  val sessionId = "session-12345"
  val journeyName: String = Journey.QUERY_BUILDER
  val dataSet: String     = Journey.HMRC_SIC_8
  val journey = Journey(sessionId, journeyName, Journey.HMRC_SIC_8)
  val sicStore = SicStore(sessionId, journeyName, dataSet)

  "upsertJourney" should {

    "return a SicStore from the repository" in new Setup {
      when(mockSicStoreRepo.upsertJourney(eqTo(journey)))
        .thenReturn(Future.successful(sicStore))

      awaitAndAssert(service.upsertJourney(journey)) {
        _ mustBe sicStore
      }
    }
  }

  "retrieveJourney" should {

    "return a the journey Id found in the sic store" in new Setup {
      when(mockSicStoreRepo.retrieveSicStore(eqTo(sessionId))(any()))
        .thenReturn(Future.successful(Some(sicStore)))

      awaitAndAssert(service.retrieveJourney(sessionId)) {
        _ mustBe Some((journeyName, dataSet))
      }
    }

    "return a None when a sic store is not found" in new Setup {
      when(mockSicStoreRepo.retrieveSicStore(eqTo(sessionId))(any()))
        .thenReturn(Future.successful(None))

      awaitAndAssert(service.retrieveJourney(sessionId)) {
        _ mustBe None
      }
    }
  }

}
