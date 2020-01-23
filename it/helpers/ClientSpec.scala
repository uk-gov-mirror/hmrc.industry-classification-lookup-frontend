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

package helpers

import org.scalatest.concurrent.{IntegrationPatience, PatienceConfiguration}
import org.scalatest.{Assertion, BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.HeaderNames
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.http.{HeaderNames => HmrcHeaderNames}

import scala.concurrent.Future

trait ClientSpec extends PlaySpec with GuiceOneServerPerSuite with Wiremock with TestAppConfig
  with FutureAwaits with DefaultAwaitTimeout with HeaderNames with ClientHelper
  with BeforeAndAfterEach with BeforeAndAfterAll with LoginStub with ICLStub with PatienceConfiguration with IntegrationPatience {

  def buildClient(path: String)(implicit app: Application): WSRequest = {
    app.injector.instanceOf[WSClient]
      .url(s"http://localhost:$port$path")
      .withFollowRedirects(false)
  }

  def assertFutureResponse(func: => Future[WSResponse])(assertions: WSResponse => Assertion): Assertion = {
    assertions(await(func))
  }

  override def beforeEach(): Unit = resetWiremock()

  override def beforeAll(){
    super.beforeAll()
    startWiremock()
  }

  override def afterAll(){
    stopWiremock()
    super.afterAll()
  }
}

trait ClientHelper {
  this: ClientSpec =>

  implicit class RichWSRequest(req: WSRequest) {
    def withTrueClientIPHeader(trueClientIp: String): WSRequest = req.withHeaders(HmrcHeaderNames.trueClientIp -> trueClientIp)
    def withSessionIdHeader(sessionId: String = "test-session-id"): WSRequest = req.withHeaders(HmrcHeaderNames.xSessionId -> sessionId)
  }

  implicit class RichWSResponse(res: WSResponse){
    def redirectLocation: Option[String] = res.header(LOCATION)
  }
}
