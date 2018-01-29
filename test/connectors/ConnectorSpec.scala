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

import config.WSHttp
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing

import scala.concurrent.Future

trait ConnectorSpec extends UnitSpec with MockitoSugar {

  val mockHttp: WSHttp = mock[WSHttp]

  def mockHttpGet[T]: OngoingStubbing[Future[T]] = when(mockHttp.GET[T](any())(any(), any(), any()))
  def mockHttpGet[T](url: String): OngoingStubbing[Future[T]] = when(mockHttp.GET[T](eqTo(url))(any(), any(), any()))
}
