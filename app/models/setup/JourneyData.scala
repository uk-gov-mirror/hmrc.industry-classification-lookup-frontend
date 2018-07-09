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

package models.setup

import java.time.LocalDateTime
import java.util.UUID

import models.TimeFormat
import models.setup.messages.CustomMessages
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class JourneyData(identifiers: Identifiers,
                       redirectUrl: String,
                       customMessages: Option[CustomMessages],
                       journeySetupDetails: JourneySetup,
                       lastUpdated: LocalDateTime)

case class Identifiers(journeyId: String, sessionId: String)

object Identifiers {
  implicit val format: Format[Identifiers] = Json.format[Identifiers]
}

case class JourneySetup(dataSet: String = JourneyData.ONS,
                        queryParser: Boolean = false,
                        queryBooster: Option[Boolean],
                        amountOfResults: Int = 50)
object JourneySetup {
  val mongoWrites: Writes[JourneySetup] = new Writes[JourneySetup] {
    override def writes(o: JourneySetup): JsValue = Json.obj(
      "journeySetupDetails.dataSet" -> o.dataSet,
      "journeySetupDetails.queryParser" -> o.queryParser,
      "journeySetupDetails.queryBooster" -> o.queryBooster,
      "journeySetupDetails.amountOfResults" -> o.amountOfResults
    )
  }
}

object JourneyData extends TimeFormat {
  //Journeys
  val QUERY_BUILDER = "query-builder"
  val QUERY_PARSER  = "query-parser"
  val QUERY_BOOSTER = "query-boost-first-term"
  val journeyNames = Seq(QUERY_PARSER, QUERY_BUILDER, QUERY_BOOSTER)
  //Data sets
  val GDS        = "gds-register-sic5"
  val ONS        = "ons-supplement-sic5"
  val dataSets = Seq(GDS, ONS)



  implicit val journeySetupFormat: Format[JourneySetup] = Json.format[JourneySetup]

  implicit val format: OFormat[JourneyData] = (
    (__ \ "identifiers").format[Identifiers] and
    (__ \ "redirectUrl").format[String] and
    (__ \ "customMessages").formatNullable[CustomMessages] and
    (__ \ "journeySetupDetails").format[JourneySetup] and
    (__ \ "lastUpdated").format[LocalDateTime](dateTimeRead)(dateTimeWrite)
  )(JourneyData.apply, unlift(JourneyData.unapply))

  def initialRequestReads(sessionId: String): Reads[JourneyData] = (
    (__ \ "identifiers").read(Identifiers(UUID.randomUUID().toString, sessionId)) and
    (__ \ "redirectUrl").read[String] and
    (__ \ "customMessages").readNullable[CustomMessages] and
    (__ \ "journeySetupDetails").read(JourneySetup(queryBooster = None)) and
    (__ \ "lastUpdated").read(LocalDateTime.now)
  )(JourneyData.apply _)
}