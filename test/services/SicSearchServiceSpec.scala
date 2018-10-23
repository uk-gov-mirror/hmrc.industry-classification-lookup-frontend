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

import connectors.ICLConnector
import helpers.UnitTestSpec
import models._
import models.setup.{Identifiers, JourneyData, JourneySetup}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import repositories.SicStoreMongoRepository
import uk.gov.hmrc.http.NotFoundException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SicSearchServiceSpec extends UnitTestSpec {

  class Setup {
    val service: SicSearchService = new SicSearchService {
      protected val iCLConnector: ICLConnector = mockICLConnector
      protected val sicStoreRepository: SicStoreMongoRepository = mockSicStoreRepo
    }
  }

  val sessionId = "session-id-12345"
  val journeyId = "testJourneyId"
  val query = "testQuery"
  val journey: String = JourneyData.QUERY_BUILDER
  val dataSet: String = JourneyData.ONS
  val identifier = Identifiers(
    journeyId = journeyId,
    sessionId = sessionId
  )
  val sicCodeCode = "12345"
  val sicCode = SicCode(sicCodeCode, "some sic code description")
  val oneSearchResult = SearchResults(query, 1, List(sicCode), List(Sector("A", "Fake Sector", 1)))
  val threeSearchResults = SearchResults(query, 3, List(sicCode, sicCode, sicCode), List(Sector("A", "Fake Sector A", 2), Sector("B", "Fake Sector B", 1)))
  val searchResultsEmpty = SearchResults(query, 0, List(), List())
  val choices = List(SicCodeChoice(sicCode, Nil))
  val sicStore = SicStore(sessionId, Some(oneSearchResult), Some(choices))
  val sicStoreNoChoices = SicStore(sessionId, Some(oneSearchResult), None)

  val testJourneyData = JourneyData(
    identifiers          = identifier,
    redirectUrl         = "/test/uri",
    journeySetupDetails = JourneySetup(),
    lastUpdated         = LocalDateTime.now
  )


  "lookupSicCodes" should {

    "return true when a sic code is found in ICL and successfully saved" in new Setup {
      when(mockICLConnector.lookup(eqTo(sicCodeCode))(any()))
        .thenReturn(Future.successful(List(sicCode)))

      when(mockSicStoreRepo.upsertSearchResults(eqTo(journeyId), any())(any()))
        .thenReturn(Future.successful(true))

      when(mockSicStoreRepo.insertChoices(eqTo(journeyId), any())(any()))
        .thenReturn(Future.successful(true))

      when(mockSicStoreRepo.upsertSearchResults(eqTo(journeyId), any())(any()))
        .thenReturn(Future.successful(true))

      awaitAndAssert(service.lookupSicCodes(testJourneyData, List(sicCode, SicCode(sicCodeCode, "Some Description")))) {
        _ mustBe 1
      }
    }

    "return true if a sicCode with no description is passed in and is found in ICL and successfully saved" in new Setup {
      when(mockICLConnector.lookup(eqTo(sicCodeCode))(any()))
        .thenReturn(Future.successful(List(sicCode)))

      when(mockSicStoreRepo.upsertSearchResults(eqTo(journeyId), any())(any()))
        .thenReturn(Future.successful(true))

      when(mockSicStoreRepo.insertChoices(eqTo(journeyId), any())(any()))
        .thenReturn(Future.successful(true))
      when(mockSicStoreRepo.upsertSearchResults(eqTo(journeyId), any())(any()))
        .thenReturn(Future.successful(true))

      awaitAndAssert(service.lookupSicCodes(testJourneyData, List(SicCode(sicCodeCode, "")))) {
        _ mustBe 1
      }
    }

    "return false when a sic code is found in ICL but unsuccessfully saved" in new Setup {
      when(mockICLConnector.lookup(eqTo(sicCodeCode))(any()))
        .thenReturn(Future.successful(Nil))

      when(mockSicStoreRepo.insertChoices(eqTo(journeyId), any())(any()))
        .thenReturn(Future.successful(true))

      when(mockSicStoreRepo.upsertSearchResults(eqTo(journeyId), any())(any()))
        .thenReturn(Future.successful(false))

      awaitAndAssert(service.lookupSicCodes(testJourneyData, List(sicCode))) {
        _ mustBe 0
      }
    }

    "return false when a sic code is not found" in new Setup {
      when(mockICLConnector.lookup(eqTo(sicCodeCode))(any()))
        .thenReturn(Future.successful(Nil))
      when(mockSicStoreRepo.upsertSearchResults(eqTo(journeyId), any())(any()))
        .thenReturn(Future.successful(true))

      awaitAndAssert(service.lookupSicCodes(testJourneyData, List(sicCode))) {
        _ mustBe 0
      }
    }

    "return false when no sic code is provided" in new Setup {
      awaitAndAssert(service.lookupSicCodes(testJourneyData, Nil)) {
        _ mustBe 0
      }
    }
  }

  "searchQuery" should {
    "return true when 1 search result is returned from ICL" in new Setup {
      when(mockICLConnector.search(eqTo(query), any(), any())(any()))
        .thenReturn(Future.successful(oneSearchResult))

      when(mockSicStoreRepo.upsertSearchResults(any(), any())(any()))
        .thenReturn(Future.successful(true))

      when(mockSicStoreRepo.insertChoices(eqTo(journeyId), any())(any()))
        .thenReturn(Future.successful(true))

      when(mockICLConnector.lookup(any())(any()))
        .thenReturn(Future.successful(List(SicCode("",""))))

      awaitAndAssert(service.searchQuery(testJourneyData,query)) {
        _ mustBe 1
      }
    }

    "return true when more than 1 search results are returned from ICL" in new Setup {
      when(mockICLConnector.search(eqTo(query), any(), any())(any()))
        .thenReturn(Future.successful(threeSearchResults))

      when(mockSicStoreRepo.upsertSearchResults(eqTo(journeyId), eqTo(threeSearchResults))(any()))
        .thenReturn(Future.successful(true))

      awaitAndAssert(service.searchQuery(testJourneyData, query, None)) {
        _ mustBe 3
      }
    }

    "return false when a set of search results are returned from ICL" in new Setup {
      when(mockICLConnector.search(eqTo(query), any(), any())(any()))
        .thenReturn(Future.successful(searchResultsEmpty))

      when(mockSicStoreRepo.upsertSearchResults(any(), any())(any()))
        .thenReturn(Future.successful(true))

      awaitAndAssert(service.searchQuery(testJourneyData, query)) {
        _ mustBe 0
      }
    }

    "return false when nothing is returned from ICL" in new Setup {
      when(mockICLConnector.search(eqTo(query), any(), any())(any()))
        .thenReturn(Future.failed(new NotFoundException("404")))

      awaitAndAssert(service.searchQuery(testJourneyData, query)) {
        _ mustBe 0
      }
    }
  }

  "search" should {
    "lookup and return a sic code" in new Setup {
      val query = "12345"

      when(mockICLConnector.lookup(any())(any()))
        .thenReturn(Future.successful(List(sicCode)))

      when(mockSicStoreRepo.upsertSearchResults(eqTo(journeyId), any())(any()))
        .thenReturn(Future.successful(true))

      when(mockSicStoreRepo.insertChoices(eqTo(journeyId), any())(any()))
        .thenReturn(Future.successful(true))

      awaitAndAssert(service.search(testJourneyData, query)) {
        _ mustBe 1
      }
    }

    "search using a query and return a set of search results" in new Setup {
      val query = "some query"

      when(mockICLConnector.search(eqTo(query), any(), any())(any()))
        .thenReturn(Future.successful(oneSearchResult))
      when(mockSicStoreRepo.upsertSearchResults(any(), any())(any()))
        .thenReturn(Future.successful(true))

      when(mockSicStoreRepo.insertChoices(eqTo(journeyId), any())(any()))
        .thenReturn(Future.successful(true))

      when(mockICLConnector.lookup(any())(any()))
        .thenReturn(Future.successful(List(SicCode("",""))))

      awaitAndAssert(service.search(testJourneyData, query, Some("A"))) {
        _ mustBe 1
      }
    }
  }

  "insertChoices" should {

    "return the sic code that was inserted successfully" in new Setup {
      when(mockSicStoreRepo.insertChoices(eqTo(sessionId), eqTo(choices))(any()))
        .thenReturn(Future.successful(true))

      awaitAndAssert(service.insertChoices(sessionId, choices)) {
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
  "retrieveChoicesForApi" should {
    "not replace n.e.c for not elsewhere classified and return a list of sic codes if found for the given session if" in new Setup {
      val sicCode1 = SicCode("12345","This is foo bar n.e.c")
      val sicCode2 = SicCode("45678", "foo bar")
      val choices2Codes = List(SicCodeChoice(sicCode1, Nil),SicCodeChoice(sicCode2, Nil))
      val choices2CodesTextNOTReplaced = List(SicCodeChoice(sicCode1, Nil),SicCodeChoice(sicCode2, Nil))
      val sicStore2 = SicStore(sessionId, Some(SearchResults(query,2,List(sicCode1,sicCode2),List(Sector("A", "Fake Sector A", 1), Sector("B", "Fake Sector B", 1)))),Some(choices2Codes))

      when(mockSicStoreRepo.retrieveSicStore(eqTo(sessionId))(any()))
        .thenReturn(Future.successful(Some(sicStore2)))

      awaitAndAssert(service.retrieveChoicesForApi(sessionId)) {
        _ mustBe Some(choices2CodesTextNOTReplaced)
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
    "returns a list of sic codes if they are found and replace n.e.c for not elsewhere classified"  in new Setup {
      val sicCode1 = SicCode("12345","This is foo bar n.e.c")
      val sicCode2 = SicCode("45678", "n.e")
      val choices2Codes = List(SicCodeChoice(sicCode1, Nil),SicCodeChoice(sicCode2, Nil))
      val choices2CodesTextReplaced = List(SicCodeChoice(sicCode1.copy(description = "This is foo bar not elsewhere classified"), Nil),SicCodeChoice(sicCode2, Nil))
      val sicStore2 = SicStore(sessionId, Some(SearchResults(query,2,List(sicCode1,sicCode2),List(Sector("A", "Fake Sector A", 1), Sector("B", "Fake Sector B", 1)))),Some(choices2Codes))

      when(mockSicStoreRepo.retrieveSicStore(eqTo(sessionId))(any()))
        .thenReturn(Future.successful(Some(sicStore2)))

      awaitAndAssert(service.retrieveChoices(sessionId)) {
        _ mustBe Some(choices2CodesTextReplaced)
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
    "returns a list of sic codes if they are found for the given session id and replace n.e.c for not elsewhere classified" in new Setup {
      val sicCode1 = SicCode("12345", "This is foo bar n.e.c")
      val sicCode2 = SicCode("45678", "n.e")
      val choices2Codes = List(SicCodeChoice(sicCode1, Nil), SicCodeChoice(sicCode2, Nil))

      val sicStore2 = SicStore(sessionId, Some(SearchResults(query, 2, List(sicCode1, sicCode2), List(Sector("A", "Fake Sector A", 1), Sector("B", "Fake Sector B", 1)))), Some(choices2Codes))
      when(mockSicStoreRepo.retrieveSicStore(eqTo(sessionId))(any()))
        .thenReturn(Future.successful(Some(sicStore2)))

      awaitAndAssert(service.retrieveSearchResults(sessionId)) {
        _.get mustBe SearchResults(query, 2, List(sicCode1.copy(description = "This is foo bar not elsewhere classified"), sicCode2), List(Sector("A", "Fake Sector A", 1), Sector("B", "Fake Sector B", 1)))
      }
    }
  }
  "necReplacement" should {
    "replace n.e.c with not elsewhere classified" in new Setup {
      service.necReplacement("foo bar n.e.c") mustBe "foo bar not elsewhere classified"
    }
    "not replace n.e.e" in new Setup {
      service.necReplacement("foo bar n.e.e") mustBe "foo bar n.e.e"
    }
    "handle blank string" in new Setup {
      service.necReplacement("") mustBe ""
    }
  }
  "isLookup" should {
    "return true" when {

      "a 5 digit String is supplied" in new Setup {
        val _5digit = "12345"
        service.isLookup(_5digit) mustBe true
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
        val query = "12345 sometext"
        service.isLookup(query) mustBe false
      }
    }
  }
}