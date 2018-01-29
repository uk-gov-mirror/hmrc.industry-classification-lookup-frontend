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

package models

import play.api.libs.json.{JsSuccess, JsValue, Json}
import uk.gov.hmrc.play.test.UnitSpec

class ChooseActivitySpec extends UnitSpec {

  val testBusinessActivityModel = ChooseActivity("12345678")

  val testBusinessActivityJson : JsValue = Json.parse(
    """
      |{
      |  "code":"12345678"
      |}
    """.stripMargin
  )

  "Business Activity Model" should {

    "read from json with data" in {
      Json.fromJson(testBusinessActivityJson)(ChooseActivity.format).get shouldBe testBusinessActivityModel
    }

    "write to json with data" in {
      Json.toJson(testBusinessActivityModel)(ChooseActivity.format) shouldBe testBusinessActivityJson
    }

  }

}
