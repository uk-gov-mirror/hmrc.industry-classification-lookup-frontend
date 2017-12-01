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

package services

import models.{Journey, SicStore}
import org.scalatest.mockito.MockitoSugar
import repositories.SicStoreMongoRepository
import uk.gov.hmrc.play.test.UnitSpec
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{any, eq => eqTo}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class JourneyServiceSpec extends UnitSpec with MockitoSugar {

  val mockSicStoreRepo: SicStoreMongoRepository = mock[SicStoreMongoRepository]

  trait Setup {
    val service: JourneyService = new JourneyService {
      override val sicStore: SicStoreMongoRepository = mockSicStoreRepo
    }
  }

  val sessionId = "session-12345"
  val journeyName: String = Journey.QUERY_BUILDER
  val journey = Journey(sessionId, journeyName)
  val sicStore = SicStore(sessionId, journeyName)

  "upsertJourney" should {

    "return a SicStore from the repository" in new Setup {
      when(mockSicStoreRepo.upsertJourney(eqTo(journey)))
        .thenReturn(Future.successful(sicStore))

      val result: SicStore = service.upsertJourney(journey)
      result shouldBe sicStore
    }
  }

  "retrieveJourney" should {

    "return a the journey Id found in the sic store" in new Setup {
      when(mockSicStoreRepo.retrieveSicStore(eqTo(sessionId))(any()))
        .thenReturn(Future.successful(Some(sicStore)))

      val result: Option[String] = service.retrieveJourney(sessionId)
      result shouldBe Some(journeyName)
    }

    "return a None when a sic store is not found" in new Setup {
      when(mockSicStoreRepo.retrieveSicStore(eqTo(sessionId))(any()))
        .thenReturn(Future.successful(None))

      val result: Option[String] = service.retrieveJourney(sessionId)
      result shouldBe None
    }
  }

}
