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
import models.{SearchResults, SicCode, SicStore}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec
import org.mockito.Mockito._
import repositories.SicStoreRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class SicSearchServiceSpec extends UnitSpec with MockitoSugar {

  val mockICLConnector: ICLConnector = mock[ICLConnector]
  val mockSicStoreRepo: SicStoreRepository = mock[SicStoreRepository]

  trait Setup {
    val service: SicSearchService = new SicSearchService {
      protected val iCLConnector: ICLConnector = mockICLConnector
      protected val sicStoreRepository: SicStoreRepository = mockSicStoreRepo
    }

    reset(mockICLConnector, mockSicStoreRepo)
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val sessionId = "session-id-12345"

  val query = "testQuery"

  val sicCodeCode = "12345678"
  val sicCode = SicCode(sicCodeCode, "some sic code description")
  val searchResults = SearchResults(query, 1, List(sicCode))
  val searchResultsEmpty = SearchResults(query, 0, List())
  val choices = List(sicCode)
  val sicStore = SicStore(sessionId, searchResults, Some(choices))
  val sicStoreNoChoices = SicStore(sessionId, searchResults, None)

  "lookupSicCode" should {

    "return true when a sic code is found in ICL and successfully saved" in new Setup {
      when(mockICLConnector.lookup(eqTo(sicCodeCode))(any()))
        .thenReturn(Future.successful(Some(sicCode)))
      when(mockSicStoreRepo.updateSearchResults(eqTo(sessionId), any())(any()))
        .thenReturn(Future.successful(true))

      val result: Boolean = service.lookupSicCode(sessionId, sicCodeCode)
      result shouldBe true
    }

    "return false when a sic code is found in ICL but unsuccessfully saved" in new Setup {
      when(mockICLConnector.lookup(eqTo(sicCodeCode))(any()))
        .thenReturn(Future.successful(Some(sicCode)))
      when(mockSicStoreRepo.updateSearchResults(eqTo(sessionId), any())(any()))
        .thenReturn(Future.successful(false))

      val result: Boolean = service.lookupSicCode(sessionId, sicCodeCode)
      result shouldBe false
    }

    "return false when a sic code is not found" in new Setup {
      when(mockICLConnector.lookup(eqTo(sicCodeCode))(any()))
        .thenReturn(Future.successful(None))

      val result: Boolean = service.lookupSicCode(sessionId, sicCodeCode)
      result shouldBe false
    }
  }

  "searchQuery" should {

    val query = "some query"

    "return true when a set of search results are returned from ICL" in new Setup {
      when(mockICLConnector.search(eqTo(query))(any()))
        .thenReturn(Future.successful(Some(searchResults)))
      when(mockSicStoreRepo.updateSearchResults(any(), any())(any()))
        .thenReturn(Future.successful(true))

      val result: Boolean = service.searchQuery(sessionId, query)
      result shouldBe true
    }

    "return false when a set of search results are returned from ICL" in new Setup {
      when(mockICLConnector.search(eqTo(query))(any()))
        .thenReturn(Future.successful(Some(searchResultsEmpty)))

      val result: Boolean = service.searchQuery(sessionId, query)
      result shouldBe false
    }

    "return false when nothing is returned from ICL" in new Setup {
      when(mockICLConnector.search(eqTo(query))(any()))
        .thenReturn(Future.successful(None))

      val result: Boolean = service.searchQuery(sessionId, query)
      result shouldBe false
    }
  }

  "search" should {

    "lookup and return a sic code" in new Setup {
      val query = "12345678"

      when(mockICLConnector.lookup(eqTo(sicCodeCode))(any()))
        .thenReturn(Future.successful(Some(sicCode)))
      when(mockSicStoreRepo.updateSearchResults(eqTo(sessionId), any())(any()))
        .thenReturn(Future.successful(true))

      val result: Boolean = service.search(sessionId, query)
      result shouldBe true
    }

    "search using a query and return a set of search results" in new Setup {
      val query = "some query"

      when(mockICLConnector.search(eqTo(query))(any()))
        .thenReturn(Future.successful(Some(searchResults)))
      when(mockSicStoreRepo.updateSearchResults(any(), any())(any()))
        .thenReturn(Future.successful(true))

      val result: Boolean = service.search(sessionId, query)
      result shouldBe true
    }
  }

  "updateSearchResults" should {

    "return a sic code on success" in new Setup {
      when(mockSicStoreRepo.updateSearchResults(eqTo(sessionId), any())(any()))
        .thenReturn(Future.successful(true))

      val result: Boolean = service.updateSearchResults(sessionId, searchResults)
      result shouldBe true
    }
  }

  "retrieveSicStore" should {

    "return the sic store for the user specified" in new Setup {
      when(mockSicStoreRepo.retrieveSicStore(eqTo(sessionId))(any()))
        .thenReturn(Future.successful(Some(sicStoreNoChoices)))

      val result: Option[SicStore] = service.retrieveSicStore(sessionId)
      result shouldBe Some(sicStoreNoChoices)
    }
  }

  "insertChoice" should {

    "return the sic code that was inserted successfully" in new Setup {
      when(mockSicStoreRepo.insertChoice(eqTo(sessionId), eqTo(sicCodeCode))(any()))
        .thenReturn(Future.successful(true))

      val result: Boolean = service.insertChoice(sessionId, sicCodeCode)
      result shouldBe true
    }
  }

  "removeChoice" should {

    "return the sic code that was removed successfully" in new Setup {
      when(mockSicStoreRepo.removeChoice(eqTo(sessionId), eqTo(sicCodeCode))(any()))
        .thenReturn(Future.successful(true))

      val result: Boolean = service.removeChoice(sessionId, sicCodeCode)
      result shouldBe true
    }
  }

  "retrieveChoices" should {

    "return a list of sic codes if they are found for the given session id" in new Setup {
      when(mockSicStoreRepo.retrieveSicStore(eqTo(sessionId))(any()))
        .thenReturn(Future.successful(Some(sicStore)))

      val result: Option[List[SicCode]] = service.retrieveChoices(sessionId)
      result shouldBe Some(choices)
    }
  }

  "retrieveSearchResults" should {

    "return a list of sic codes if they are found for the given session id" in new Setup {
      when(mockSicStoreRepo.retrieveSicStore(eqTo(sessionId))(any()))
        .thenReturn(Future.successful(Some(sicStore)))

      val result: Option[SearchResults] = service.retrieveSearchResults(sessionId)
      result shouldBe Some(searchResults)
    }
  }

  "isLookup" should {

    "return true" when {

      "a 8 digit String is supplied" in new Setup {
        val _8digit = "12345678"
        service.isLookup(_8digit) shouldBe true
      }
    }

    "return false" when {

      "a 7 digit String is supplied" in new Setup {
        val _7digit = "1234567"
        service.isLookup(_7digit) shouldBe false
      }

      "a normal String is supplied" in new Setup {
        val query = "some query"
        service.isLookup(query) shouldBe false
      }

      "a 8 digit String is supplied along with some text" in new Setup {
        val query = "12345678sometext"
        service.isLookup(query) shouldBe false
      }

      "a 8 digit String is supplied along with a space and then some text" in new Setup {
        val query = "12345678 sometext"
        service.isLookup(query) shouldBe false
      }
    }
  }
}
