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

package forms

import forms.chooseactivity.ChooseMultipleActivityForm
import org.scalatestplus.play.PlaySpec
import play.api.data.FormError
import play.api.test.FakeRequest

import scala.collection.mutable.ArrayBuffer

class ChooseMultipleActivityFormSpec extends PlaySpec {

  implicit val request = FakeRequest().withFormUrlEncodedBody(
    "code[0]" -> "testCode0",
    "code[1]" -> "testCode1",
    "code[2]" -> "testCode2",
    "code[3]" -> "testCode3",
    "code[4]" -> "testCode4"
  )

  val inputMap = Map(
    "code[0]" -> "testCode0",
    "code[1]" -> "testCode1",
    "code[2]" -> "testCode2",
    "code[3]" -> "testCode3",
    "code[4]" -> "testCode4"
  )

  val inputMap2 = Map(
    "code[0]" -> "testCode0",
    "code[1]" -> "testCode1",
    "code[2]" -> "",
    "code[3]" -> "testCode3",
    "code[4]" -> "testCode4"
  )

  val inputMap3 = Map(
    "code[0]" -> "testCode0-description-sdsdds",
    "code[1]" -> "testCode1-description-sdsd",
    "code[2]" -> "",
    "code[3]" -> "testCode3-description-sdsds",
    "code[4]" -> "testCode4-description-sdsdd"
  )

  val expectedList = List("testCode0", "testCode1", "testCode2", "testCode3", "testCode4")

  "SicSearchMultipleForm" should {
    "bind successfully" when {
      "binding from a request" in {
        ChooseMultipleActivityForm.form.bindFromRequest().get mustBe expectedList
      }

      "binding from a map and transform data" in {
        ChooseMultipleActivityForm.form.bind(inputMap3).get mustBe List("testCode0", "testCode1", "testCode3", "testCode4")
      }

      "binding from a map" in {
        ChooseMultipleActivityForm.form.bind(inputMap).get mustBe expectedList
      }

      "binding from a map with gaps" in {
        ChooseMultipleActivityForm.form.bind(inputMap2).get mustBe List("testCode0", "testCode1", "testCode3", "testCode4")
      }
    }

    "bind unsuccessfully" when {
      val emptyErrors = List(
        FormError("code", List("errors.invalid.sic.noSelection"))
      )

      "nothing is the request" in {
        ChooseMultipleActivityForm.form.bindFromRequest()(FakeRequest()).errors mustBe emptyErrors
      }

      "the map is empty" in {
        ChooseMultipleActivityForm.form.bind(Map.empty[String, String]).errors mustBe emptyErrors
      }
    }
  }
}
