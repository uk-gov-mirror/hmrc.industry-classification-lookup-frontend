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

import config.WSHttp
import org.mockito.ArgumentMatchers
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, NotFoundException}
import uk.gov.hmrc.play.test.UnitSpec
import org.mockito.Matchers._
import org.mockito.Mockito._
import repositories.models.SicCode

import scala.concurrent.Future

class ICLConnectorSpec extends UnitSpec with MockitoSugar {

  val mockHttp: WSHttp = mock[WSHttp]
  val iCLUrl = "http://localhost:12345/"

  trait Setup {
    val connector: ICLConnector = new ICLConnector {
      val http: HttpGet = mockHttp
      val ICLUrl: String = iCLUrl
    }
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "lookupSicCode" should {

    val sicCode = "12345678"
    val sicCodeDescription = "some description"
    val sicCodeResult = SicCode(sicCode, sicCodeDescription)

    "return a sic code case class matching the code provided" in new Setup {
      when(mockHttp.GET[SicCode](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(sicCodeResult))

      val result: Option[SicCode] = connector.lookupSicCode(sicCode)
      result shouldBe Some(sicCodeResult)
    }

    "return none when ICL returns a 404" in new Setup {
      when(mockHttp.GET[SicCode](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new NotFoundException("404")))

      val result: Option[SicCode] = connector.lookupSicCode(sicCode)
      result shouldBe None
    }
  }
}
