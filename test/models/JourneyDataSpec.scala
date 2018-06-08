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

import java.util.UUID

import models.setup.{JourneyData, JourneySetup}
import models.setup.messages.{CustomMessages, Summary}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

import scala.util.Try

class JourneyDataSpec extends PlaySpec {

  def isValidUUID(uuid: String): Boolean = Try(UUID.fromString(uuid)).isSuccess

  val partialJson = Json.obj(
    "redirectUrl" -> "/test/uri"
  )

  val partialJsonWithMessages = Json.obj(
    "redirectUrl" -> "/test/uri",
    "customMessages" -> Json.obj(
      "summary" -> Json.obj(
        "heading" -> "testMessage",
        "lead"    -> "testMessage"
      )
    )
  )

  val partialJsonWithSetupDetails = Json.obj(
    "redirectUrl" -> "/test/uri",
    "customMessages" -> Json.obj(
      "summary" -> Json.obj(
        "heading" -> "testMessage",
        "lead"    -> "testMessage"
      )
    ),
    "journeySetupDetails" -> Json.obj(
      "dataSet"     -> "testSet",
      "journeyType" -> "testType"
    )
  )

  val sessionId = "testSessionId"

  "newPublicJourneyReads" should {
    "construct a JourneyData case class" when {
      "only given a redirectUrl" in {
        val result = Json.fromJson(partialJson)(JourneyData.initialRequestReads(sessionId)).get

        assert(isValidUUID(result.identifiers.journeyId))
        result.identifiers.sessionId mustBe sessionId

        result.redirectUrl    mustBe "/test/uri"
        result.customMessages mustBe None
      }

      "given a redirect url and messages" in {
        val result = Json.fromJson(partialJsonWithMessages)(JourneyData.initialRequestReads(sessionId)).get

        assert(isValidUUID(result.identifiers.journeyId))
        result.identifiers.sessionId mustBe sessionId

        result.redirectUrl    mustBe "/test/uri"
        result.customMessages mustBe Some(CustomMessages(
          summary = Some(Summary(heading = Some("testMessage"), lead = Some("testMessage")))
        ))
      }

      "ignore the journeySetup details if provided" in {
        val result = Json.fromJson(partialJsonWithSetupDetails)(JourneyData.initialRequestReads(sessionId)).get

        assert(isValidUUID(result.identifiers.journeyId))
        result.identifiers.sessionId mustBe sessionId

        result.redirectUrl    mustBe "/test/uri"
        result.customMessages mustBe Some(CustomMessages(
          summary = Some(Summary(heading = Some("testMessage"), lead = Some("testMessage")))
        ))

        result.journeySetupDetails.dataSet mustBe "ons-supplement-sic5"
        result.journeySetupDetails.dataSet must not be "testSet"

        result.journeySetupDetails.journeyType mustBe "query-boost-first-term"
        result.journeySetupDetails.journeyType must not be "testType"

      }
    }
  }
  "journeySetup mongoWrites" should {
    "produce valid json" in {
      val journeySetup = JourneySetup("foo","bar",5)
      val expectedJson = Json.parse(
        """
          |{
          | "journeySetupDetails.dataSet" : "foo",
          | "journeySetupDetails.journeyType" : "bar",
          | "journeySetupDetails.amountOfResults" : 5
          |}
        """.stripMargin)
      Json.toJson(journeySetup)(JourneySetup.mongoWrites)
    }
  }
}
