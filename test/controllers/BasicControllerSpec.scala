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

package controllers

import helpers.UnitTestSpec
import helpers.mocks.MockAppConfig
import play.api.libs.json.{JsValue, Json, OFormat}
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest

import scala.concurrent.Future

case class Tester(key: String)
object Tester {
  implicit val format = Json.format[Tester]
}

class BasicControllerSpec extends UnitTestSpec with MockAppConfig {
  val controller = new BasicController {}

  def okFunction(sessionId: String) = Future(Ok(sessionId))

  "withSessionId" should {
    "successfully call the partial function with sessionId as argument" in {
      implicit val requestWithSessionId: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSessionId("test-session-id")

      val result = controller.withSessionId { sessionId =>
        okFunction(sessionId)
      }

      contentAsString(result) mustBe "test-session-id"
    }

    "return a 400 if the sessionId is missing" in {
      implicit val requestWithoutSessionId: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

      val result = controller.withSessionId { sessionId =>
        okFunction(sessionId)
      }

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "SessionId is missing from request"
    }
  }
}
