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

case class JourneyData(journeyId: String,
                       sessionId: String,
                       redirectUrl: String,
                       customMessages: Option[CustomMessages],
                       journeySetupDetails: JourneySetup,
                       lastUpdated: LocalDateTime)

case class JourneySetup(dataSet: String = JourneyData.ONS,
                        journeyType: String = JourneyData.QUERY_BOOSTER)

object JourneyData extends TimeFormat {
  //Journeys
  val QUERY_BUILDER = "query-builder"
  val QUERY_PARSER  = "query-parser"
  val QUERY_BOOSTER = "query-boost-first-term"
  val FUZZY_QUERY   = "fuzzy-query"

  //Data sets
  val HMRC_SIC_8 = "hmrc-sic8"
  val GDS        = "gds-register-sic5"
  val ONS        = "ons-supplement-sic5"

  implicit val journeySetupFormat: Format[JourneySetup] = Json.format[JourneySetup]

  implicit val format: OFormat[JourneyData] = (
    (__ \ "journeyId").format[String] and
    (__ \ "sessionId").format[String] and
    (__ \ "redirectUrl").format[String] and
    (__ \ "customMessages").formatNullable[CustomMessages] and
    (__ \ "journeySetupDetails").format[JourneySetup] and
    (__ \ "lastUpdated").format[LocalDateTime](dateTimeRead)(dateTimeWrite)
  )(JourneyData.apply, unlift(JourneyData.unapply))

  def newPublicJourneyReads(sessionId: String): Reads[JourneyData] = (
    (__ \ "journeyId").read(UUID.randomUUID().toString) and
    (__ \ "sessionId").read(sessionId) and
    (__ \ "redirectUrl").read[String] and
    (__ \ "customMessages").readNullable[CustomMessages] and
    (__ \ "journeySetupDetails").read(JourneySetup()) and
    (__ \ "lastUpdated").read(LocalDateTime.now)
  )(JourneyData.apply _)
}
