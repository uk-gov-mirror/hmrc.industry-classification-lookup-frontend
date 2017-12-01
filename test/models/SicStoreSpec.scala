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

import org.joda.time.DateTime
import uk.gov.hmrc.play.test.UnitSpec
import play.api.libs.json.{JsSuccess, JsValue, Json}
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

class SicStoreSpec extends UnitSpec {

  val query = "testQuery"
  val journey: String = Journey.QUERY_BUILDER
  val dateTime: DateTime = DateTime.parse("2017-06-15T10:06:28.434Z")
  val now: JsValue = Json.toJson(dateTime)(ReactiveMongoFormats.dateTimeWrite)

  val sicStoreWithChoicesJson : JsValue = Json.parse(
    s"""
      |{
      |  "registrationID" : "12345678",
      |  "journey" : "$journey",
      |  "search" : {
      |    "query":"$query",
      |    "numFound":1,
      |    "results":[
      |      {"code" : "19283746", "desc" : "Search Sic Code Result Description"}
      |    ],
      |    "sectors":[
      |      {"code" : "A", "name" : "Clearly fake business sector", "count": 22}
      |    ]
      |  },
      |  "choices" : [
      |    {"code" : "57384893", "desc" : "Sic Code Test Description 1"},
      |    {"code" : "11920233", "desc" : "Sic Code Test Description 2"},
      |    {"code" : "12994930", "desc" : "Sic Code Test Description 3"},
      |    {"code" : "39387282", "desc" : "Sic Code Test Description 4"}
      |  ],
      |  "lastUpdated" : $now
      |}
    """.stripMargin
  )

  val sicStoreNoChoicesJson : JsValue = Json.parse(
    s"""
      |{
      |  "registrationID" : "12345678",
      |   "journey" : "$journey",
      |  "search" : {
      |    "query":"$query",
      |    "numFound":1,
      |    "results":[
      |      {"code" : "19283746", "desc" : "Search Sic Code Result Description"}
      |    ],
      |    "sectors":[
      |      {"code" : "A", "name" : "Clearly fake business sector", "count": 22}
      |    ]
      |  },
      |  "lastUpdated" : $now
      |}
    """.stripMargin
  )

  val sicStoreWithChoices = SicStore(
    "12345678",
    journey,
    Some(SearchResults(
      query,
      1,
      List(SicCode("19283746", "Search Sic Code Result Description")),
      List(Sector("A", "Clearly fake business sector", 22))
    )),
    Some(List(
      SicCode("57384893", "Sic Code Test Description 1"),
      SicCode("11920233", "Sic Code Test Description 2"),
      SicCode("12994930", "Sic Code Test Description 3"),
      SicCode("39387282", "Sic Code Test Description 4")
    )),
    dateTime
  )

  val sicStoreNoChoices = SicStore(
    "12345678",
    journey,
    Some(SearchResults(
      query,
      1,
      List(SicCode("19283746", "Search Sic Code Result Description")),
      List(Sector("A", "Clearly fake business sector", 22))
    )),
    None,
    dateTime
  )

  "SicStore" should {

    "be able to be parsed into a json structure with choices" in {
      Json.toJson(sicStoreWithChoices)(SicStore.format) shouldBe sicStoreWithChoicesJson
    }

    "be able to be parsed into a json structure without choices" in {
      Json.toJson(sicStoreNoChoices)(SicStore.format) shouldBe sicStoreNoChoicesJson
    }

    "be able to be parsed from json structure with choices" in {
      Json.fromJson(sicStoreWithChoicesJson)(SicStore.format) shouldBe JsSuccess(sicStoreWithChoices)
    }

    "be able to be parsed from json structure without choices" in {
      Json.fromJson(sicStoreNoChoicesJson)(SicStore.format) shouldBe JsSuccess(sicStoreNoChoices)
    }
  }
}
