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

import connectors.ICLConnector
import helpers.UnitTestSpec
import models._
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import repositories.SicStoreRepository
import uk.gov.hmrc.http.NotFoundException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SicSearchServiceSpec extends UnitTestSpec {

  class Setup {
    val service: SicSearchService = new SicSearchService {
      protected val iCLConnector: ICLConnector = mockICLConnector
      protected val sicStoreRepository: SicStoreRepository = mockSicStoreRepo
    }
  }

  val sessionId = "session-id-12345"
  val query = "testQuery"
  val journey: String = Journey.QUERY_BUILDER
  val dataSet: String = Journey.HMRC_SIC_8

  val sicCodeCode = "12345678"
  val sicCode = SicCode(sicCodeCode, "some sic code description")
  val oneSearchResult = SearchResults(query, 1, List(sicCode), List(Sector("A", "Fake Sector", 1)))
  val threeSearchResults = SearchResults(query, 3, List(sicCode, sicCode, sicCode), List(Sector("A", "Fake Sector A", 2), Sector("B", "Fake Sector B", 1)))
  val searchResultsEmpty = SearchResults(query, 0, List(), List())
  val choices = List(sicCode)
  val sicStore = SicStore(sessionId, journey, dataSet, Some(oneSearchResult), Some(choices))
  val sicStoreNoChoices = SicStore(sessionId, journey, dataSet, Some(oneSearchResult), None)

  "lookupSicCode" should {

    "return true when a sic code is found in ICL and successfully saved" in new Setup {
      when(mockICLConnector.lookup(eqTo(sicCodeCode), any())(any()))
        .thenReturn(Future.successful(Some(sicCode)))

      when(mockSicStoreRepo.updateSearchResults(eqTo(sessionId), any())(any()))
        .thenReturn(Future.successful(true))

      when(mockSicStoreRepo.insertChoice(eqTo(sessionId), any())(any()))
        .thenReturn(Future.successful(true))
      when(mockSicStoreRepo.updateSearchResults(eqTo(sessionId), any())(any()))
        .thenReturn(Future.successful(true))

      awaitAndAssert(service.lookupSicCode(sessionId, dataSet, sicCodeCode)) {
        _ mustBe 1
      }
    }

    "return false when a sic code is found in ICL but unsuccessfully saved" in new Setup {
      when(mockICLConnector.lookup(eqTo(sicCodeCode), any())(any()))
        .thenReturn(Future.successful(Some(sicCode)))

      when(mockSicStoreRepo.insertChoice(eqTo(sessionId), any())(any()))
        .thenReturn(Future.successful(true))

      when(mockSicStoreRepo.updateSearchResults(eqTo(sessionId), any())(any()))
        .thenReturn(Future.successful(false))

      awaitAndAssert(service.lookupSicCode(sessionId, dataSet, sicCodeCode)) {
        _ mustBe 0
      }
    }

    "return false when a sic code is not found" in new Setup {
      when(mockICLConnector.lookup(eqTo(sicCodeCode), any())(any()))
        .thenReturn(Future.successful(None))
      when(mockSicStoreRepo.updateSearchResults(eqTo(sessionId), any())(any()))
        .thenReturn(Future.successful(true))

      awaitAndAssert(service.lookupSicCode(sessionId, dataSet, sicCodeCode)) {
        _ mustBe 0
      }
    }
  }

  "searchQuery" should {
    "return true when 1 search result is returned from ICL" in new Setup {
      when(mockICLConnector.search(eqTo(query), any(), any(), any())(any()))
        .thenReturn(Future.successful(oneSearchResult))

      when(mockSicStoreRepo.updateSearchResults(any(), any())(any()))
        .thenReturn(Future.successful(true))

      when(mockSicStoreRepo.insertChoice(eqTo(sessionId), any())(any()))
        .thenReturn(Future.successful(true))

      awaitAndAssert(service.searchQuery(sessionId, query, journey, dataSet)) {
        _ mustBe 1
      }
    }

    "return true when more than 1 search results are returned from ICL" in new Setup {
      when(mockICLConnector.search(eqTo(query), any(), any(), any())(any()))
        .thenReturn(Future.successful(threeSearchResults))

      when(mockSicStoreRepo.updateSearchResults(eqTo(sessionId), eqTo(threeSearchResults))(any()))
        .thenReturn(Future.successful(true))

      awaitAndAssert(service.searchQuery(sessionId, query, journey, dataSet, None)) {
        _ mustBe 3
      }
    }

    "return false when a set of search results are returned from ICL" in new Setup {
      when(mockICLConnector.search(eqTo(query), any(), any(), any())(any()))
        .thenReturn(Future.successful(searchResultsEmpty))

      when(mockSicStoreRepo.updateSearchResults(any(), any())(any()))
        .thenReturn(Future.successful(true))

      awaitAndAssert(service.searchQuery(sessionId, query, journey, dataSet)) {
        _ mustBe 0
      }
    }

    "return false when nothing is returned from ICL" in new Setup {
      when(mockICLConnector.search(eqTo(query), any(), any(), any())(any()))
        .thenReturn(Future.failed(new NotFoundException("404")))

      awaitAndAssert(service.searchQuery(sessionId, query, journey, dataSet)) {
        _ mustBe 0
      }
    }
  }

  "search" should {
    "lookup and return a sic code" in new Setup {
      val query = "12345678"

      when(mockICLConnector.lookup(eqTo(sicCodeCode), any())(any()))
        .thenReturn(Future.successful(Some(sicCode)))

      when(mockSicStoreRepo.updateSearchResults(eqTo(sessionId), any())(any()))
        .thenReturn(Future.successful(true))

      when(mockSicStoreRepo.insertChoice(eqTo(sessionId), any())(any()))
        .thenReturn(Future.successful(true))

      awaitAndAssert(service.search(sessionId, query, journey, dataSet)) {
        _ mustBe 1
      }
    }

    "search using a query and return a set of search results" in new Setup {
      val query = "some query"

      when(mockICLConnector.search(eqTo(query), any(), any(), any())(any()))
        .thenReturn(Future.successful(oneSearchResult))
      when(mockSicStoreRepo.updateSearchResults(any(), any())(any()))
        .thenReturn(Future.successful(true))
      when(mockSicStoreRepo.insertChoice(eqTo(sessionId), any())(any()))
        .thenReturn(Future.successful(true))

      awaitAndAssert(service.search(sessionId, query, journey, dataSet)) {
        _ mustBe 1
      }
    }
  }

  "insertChoice" should {

    "return the sic code that was inserted successfully" in new Setup {
      when(mockSicStoreRepo.insertChoice(eqTo(sessionId), eqTo(sicCodeCode))(any()))
        .thenReturn(Future.successful(true))

      awaitAndAssert(service.insertChoice(sessionId, sicCodeCode)) {
        _ mustBe true
      }
    }
  }

  "removeChoice" should {

    "return the sic code that was removed successfully" in new Setup {
      when(mockSicStoreRepo.removeChoice(eqTo(sessionId), eqTo(sicCodeCode))(any()))
        .thenReturn(Future.successful(true))

      awaitAndAssert(service.removeChoice(sessionId, sicCodeCode)) {
        _ mustBe true
      }
    }
  }

  "retrieveChoices" should {

    "return a list of sic codes if they are found for the given session id" in new Setup {
      when(mockSicStoreRepo.retrieveSicStore(eqTo(sessionId))(any()))
        .thenReturn(Future.successful(Some(sicStore)))

      awaitAndAssert(service.retrieveChoices(sessionId)) {
        _ mustBe Some(choices)
      }
    }
  }

  "retrieveSearchResults" should {

    "return a list of sic codes if they are found for the given session id" in new Setup {
      when(mockSicStoreRepo.retrieveSicStore(eqTo(sessionId))(any()))
        .thenReturn(Future.successful(Some(sicStore)))

      awaitAndAssert(service.retrieveSearchResults(sessionId)) {
        _ mustBe Some(oneSearchResult)
      }
    }
  }

  "isLookup" should {

    "return true" when {

      "a 8 digit String is supplied" in new Setup {
        val _8digit = "12345678"
        service.isLookup(_8digit) mustBe true
      }
    }

    "return false" when {

      "a 7 digit String is supplied" in new Setup {
        val _7digit = "1234567"
        service.isLookup(_7digit) mustBe false
      }

      "a normal String is supplied" in new Setup {
        val query = "some query"
        service.isLookup(query) mustBe false
      }

      "a 8 digit String is supplied along with some text" in new Setup {
        val query = "12345678sometext"
        service.isLookup(query) mustBe false
      }

      "a 8 digit String is supplied along with a space and then some text" in new Setup {
        val query = "12345678 sometext"
        service.isLookup(query) mustBe false
      }
    }
  }
}
