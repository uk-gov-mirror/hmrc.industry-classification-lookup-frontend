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

package models

import uk.gov.hmrc.play.test.UnitSpec
import play.api.libs.json.{JsSuccess, JsValue, Json}
import repositories.models.{SicCode, SicStore}

class SicStoreSpec extends UnitSpec {

  val sicStoreWithChoices : JsValue = Json.parse(
    """
      |{
      |  "registrationID" : "12345678",
      |  "search" : {"code" : "19283746", "desc" : "Search Sic Code Result Description"},
      |  "choices" : [
      |    {"code" : "57384893", "desc" : "Sic Code Test Description 1"},
      |    {"code" : "11920233", "desc" : "Sic Code Test Description 2"},
      |    {"code" : "12994930", "desc" : "Sic Code Test Description 3"},
      |    {"code" : "39387282", "desc" : "Sic Code Test Description 4"}
      |  ]
      |}
    """.stripMargin
  )

  val sicStoreWithoutChoices : JsValue = Json.parse(
    """
      |{
      |  "registrationID" : "12345678",
      |  "search" : {"code" : "19283746", "desc" : "Search Sic Code Result Description"}
      |}
    """.stripMargin
  )

  val createdStoreWithChoices = SicStore(
    "12345678",
    SicCode("19283746", "Search Sic Code Result Description"),
    Some(List(
      SicCode("57384893", "Sic Code Test Description 1"),
      SicCode("11920233", "Sic Code Test Description 2"),
      SicCode("12994930", "Sic Code Test Description 3"),
      SicCode("39387282", "Sic Code Test Description 4")
    ))
  )

  val createdStoreWithoutChoices = SicStore(
    "12345678",
    SicCode("19283746", "Search Sic Code Result Description"),
    None
  )

  "SicStore" should {

    "be able to be parsed into a json structure with choices" in {
      Json.toJson(createdStoreWithChoices)(SicStore.format) shouldBe sicStoreWithChoices
    }

    "be able to be parsed into a json structure without choices" in {
      Json.toJson(createdStoreWithoutChoices)(SicStore.format) shouldBe sicStoreWithoutChoices
    }

    "be able to be parsed from json structure with choices" in {
      Json.fromJson(sicStoreWithChoices)(SicStore.format) shouldBe JsSuccess(createdStoreWithChoices)
    }

    "be able to be parsed from json structure without choices" in {
      Json.fromJson(sicStoreWithoutChoices)(SicStore.format) shouldBe JsSuccess(createdStoreWithoutChoices)
    }
  }
}
