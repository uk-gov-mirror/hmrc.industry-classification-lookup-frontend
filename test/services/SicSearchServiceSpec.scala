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

import connectors.ICLConnector
import org.mockito.ArgumentMatchers
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec
import org.mockito.Mockito._
import repositories.SicStoreRepository
import repositories.models.{SicCode, SicStore}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class SicSearchServiceSpec extends UnitSpec with MockitoSugar {

  val mockICLConnector: ICLConnector = mock[ICLConnector]
  val mockSicStoreRepo: SicStoreRepository = mock[SicStoreRepository]

  trait Setup {
    val service: SicSearchService = new SicSearchService {
      protected val iCLConnector: ICLConnector = mockICLConnector
      protected val sicStoreRepository: SicStoreRepository = mockSicStoreRepo
    }
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val regId = "reg-12345"

  val sicCodeCode = "12345678"
  val sicCodeDescription = "some sic code description"
  val sicCode = SicCode(sicCodeCode, sicCodeDescription)
  val sicStore = SicStore(regId, sicCode, None)

  "lookupSicCode" should {

    "return a sic code case class if one if returned from the connector" in new Setup {
      when(mockICLConnector.lookupSicCode(ArgumentMatchers.eq(sicCodeCode))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(sicCode)))

      val result: Option[SicCode] = service.lookupSicCode(sicCodeCode)
      result shouldBe Some(sicCode)
    }
  }

  "updateSearchResults" should {

    "return a sic code on success" in new Setup {
      when(mockSicStoreRepo.upsertSearchCode(ArgumentMatchers.eq(regId), ArgumentMatchers.eq(sicCode)))
        .thenReturn(Future.successful(Some(sicCode)))

      val result: Option[SicCode] = service.updateSearchResults(regId, sicCode)
      result shouldBe Some(sicCode)
    }
  }

  "retrieveSicSearch" should {

    "return the sic store for the user specified" in new Setup {
      when(mockSicStoreRepo.retrieveSicStore(ArgumentMatchers.eq(regId)))
        .thenReturn(Future.successful(Some(sicStore)))

      val result: Option[SicStore] = service.retrieveSicStore(regId)
      result shouldBe Some(sicStore)
    }
  }

  "insertChoice" should {

    "return the sic code that was inserted successfully" in new Setup {
      when(mockSicStoreRepo.insertChoice(ArgumentMatchers.eq(regId)))
        .thenReturn(Future.successful(Some(sicCode)))

      val result: Option[SicCode] = service.insertChoice(regId)
      result shouldBe Some(sicCode)
    }
  }

  "removeChoice" should {
    "return the sic code that was removed successfully" in new Setup {
      when(mockSicStoreRepo.removeChoice(ArgumentMatchers.eq(regId), ArgumentMatchers.eq(sicCode.sicCode)))
        .thenReturn(Future.successful(Some(sicCode)))

      val result: Option[SicCode] = service.removeChoice(regId, sicCode.sicCode)
      result shouldBe Some(sicCode)
    }
  }

}
