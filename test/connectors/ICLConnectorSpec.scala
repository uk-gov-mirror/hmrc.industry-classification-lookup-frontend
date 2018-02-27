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

package connectors

import helpers.UnitTestSpec
import models.{Journey, SearchResults, Sector, SicCode}
import uk.gov.hmrc.http.{CoreGet, NotFoundException}

import scala.concurrent.Future

class ICLConnectorSpec extends UnitTestSpec {

  val iCLUrl = "http://localhost:12345"

  val dataSet: String = Journey.HMRC_SIC_8

  class Setup extends CodeMocks {
    val connector: ICLConnector = new ICLConnector {
      val http: CoreGet  = mockWSHttp
      val ICLUrl: String = iCLUrl
    }
  }

  "lookup" should {

    val sicCode = "12345678"
    val sicCodeResult = SicCode(sicCode, "some description")

    val lookupUrl = s"$iCLUrl/industry-classification-lookup/lookup/$sicCode?indexName=$dataSet"

    "return a sic code case class matching the code provided" in new Setup {
      mockHttpGet[SicCode](lookupUrl).thenReturn(Future.successful(sicCodeResult))

      awaitAndAssert(connector.lookup(sicCode, dataSet)) {
        _ mustBe Some(sicCodeResult)
      }
    }

    "return none when ICL returns a 404" in new Setup {
      mockHttpGet[SicCode](lookupUrl).thenReturn(Future.failed(new NotFoundException("404")))

      awaitAndAssert(connector.lookup(sicCode, dataSet)) {
        _ mustBe None
      }
    }

    "throw the exception when the future recovers an the exception is not http related" in new Setup {
      mockHttpGet[SicCode](lookupUrl).thenReturn(Future.failed(new RuntimeException("something went wrong")))

      val result: RuntimeException = intercept[RuntimeException](await(connector.lookup(sicCode, dataSet)))
      result.getMessage mustBe "something went wrong"
    }
  }

  "search" should {

    val query = "test query"
    val journey = Journey.QUERY_BUILDER
    val zeroResults = SearchResults(query, 0, List(), List())
    val searchResults = SearchResults(query, 1, List(SicCode("12345", "some description")), List(Sector("A", "Example of a business sector", 1)))
    val sector = "B"

    val searchUrl = s"$iCLUrl/industry-classification-lookup/search?query=$query&pageResults=500&queryType=$journey&indexName=$dataSet"
    val searchSectorUrl = s"$iCLUrl/industry-classification-lookup/search?query=$query&pageResults=500&sector=$sector&queryType=$journey&indexName=$dataSet"

    "return a SearchResults case class when one is returned from ICL" in new Setup {
      mockHttpGet[SearchResults](searchUrl).thenReturn(Future.successful(searchResults))

      awaitAndAssert(connector.search(query, journey, dataSet)) {
        _ mustBe searchResults
      }
    }

    "return a SearchResults case class when a sector search is returned from ICL" in new Setup {
      mockHttpGet[SearchResults](searchSectorUrl).thenReturn(Future.successful(searchResults))

      awaitAndAssert(connector.search(query, journey, dataSet, Some(sector))) {
        _ mustBe searchResults
      }
    }

    "return 0 results when ICL returns a 404" in new Setup {
      mockHttpGet[SearchResults](searchUrl).thenReturn(Future.failed(new NotFoundException("404")))

      awaitAndAssert(connector.search(query, journey, dataSet)) {
        _ mustBe zeroResults
      }
    }

    "throw the exception when the future recovers an the exception is not http related" in new Setup {
      mockHttpGet[SearchResults](searchUrl).thenReturn(Future.failed(new RuntimeException("something went wrong")))

      val result: RuntimeException = intercept[RuntimeException](await(connector.search(query, journey, dataSet)))
      result.getMessage mustBe "something went wrong"
    }
  }
}
