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

package connectors

import models.{SearchResults, Sector, SicCode}
import uk.gov.hmrc.http.{CoreGet, HeaderCarrier, NotFoundException}
import org.mockito.Mockito._

import scala.concurrent.Future

class ICLConnectorSpec extends ConnectorSpec {

  val iCLUrl = "http://localhost:12345"
  implicit val hc: HeaderCarrier = HeaderCarrier()

  trait Setup {
    val connector: ICLConnector = new ICLConnector {
      val http: CoreGet = mockHttp
      val ICLUrl: String = iCLUrl
    }

    reset(mockHttp)
  }

  "lookup" should {

    val sicCode = "12345678"
    val sicCodeResult = SicCode(sicCode, "some description")

    val lookupUrl = s"$iCLUrl/industry-classification-lookup/lookup/$sicCode"

    "return a sic code case class matching the code provided" in new Setup {
      mockHttpGet[SicCode](lookupUrl).thenReturn(Future.successful(sicCodeResult))

      val result: Option[SicCode] = connector.lookup(sicCode)
      result shouldBe Some(sicCodeResult)
    }

    "return none when ICL returns a 404" in new Setup {
      mockHttpGet[SicCode](lookupUrl).thenReturn(Future.failed(new NotFoundException("404")))

      val result: Option[SicCode] = connector.lookup(sicCode)
      result shouldBe None
    }

    "throw the exception when the future recovers an the exception is not http related" in new Setup {
      mockHttpGet[SicCode](lookupUrl).thenReturn(Future.failed(new RuntimeException("something went wrong")))

      val result: RuntimeException = intercept[RuntimeException](await(connector.lookup(sicCode)))
      result.getMessage shouldBe "something went wrong"
    }
  }

  "search" should {

    val query = "test query"
    val zeroResults = SearchResults(query, 0, List(), List())
    val searchResults = SearchResults(query, 1, List(SicCode("12345", "some description")), List(Sector("A", "Example of a business sector", 1)))

    val searchUrl = s"$iCLUrl/industry-classification-lookup/search?query=$query&pageResults=500"

    "return a SearchResults case class when one is returned from ICL" in new Setup {
      mockHttpGet[SearchResults](searchUrl).thenReturn(Future.successful(searchResults))

      val result: SearchResults = connector.search(query)
      result shouldBe searchResults
    }

    "return 0 results when ICL returns a 404" in new Setup {
      mockHttpGet[SearchResults](searchUrl).thenReturn(Future.failed(new NotFoundException("404")))

      val result: SearchResults = connector.search(query)
      result shouldBe zeroResults
    }

    "throw the exception when the future recovers an the exception is not http related" in new Setup {
      mockHttpGet[SearchResults](searchUrl).thenReturn(Future.failed(new RuntimeException("something went wrong")))

      val result: RuntimeException = intercept[RuntimeException](await(connector.search(query)))
      result.getMessage shouldBe "something went wrong"
    }
  }
}
