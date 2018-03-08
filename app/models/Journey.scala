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

case class Journey(sessionId: String,
                   name: String,
                   dataSet: String) {

  require(Journey.validName(name), s"$name is not a valid journey identifier")
  require(Journey.validDataSet(dataSet), s"$dataSet is not a valid data set identifier")
}

object Journey {

  //Journeys
  val QUERY_BUILDER = "query-builder"
  val QUERY_PARSER  = "query-parser"
  val QUERY_BOOSTER = "query-boost-first-term"
  val FUZZY_QUERY   = "fuzzy-query"
  private val journeyNames = Seq(QUERY_PARSER, QUERY_BUILDER, QUERY_BOOSTER, FUZZY_QUERY)

  //Data sets
  val HMRC_SIC_8 = "hmrc-sic8"
  val GDS        = "gds-register-sic5"
  val ONS        = "ons-supplement-sic5"
  private val dataSets = Seq(HMRC_SIC_8, GDS, ONS)

  private def validName(journey: String): Boolean    = journeyNames contains journey
  private def validDataSet(dataSet: String): Boolean = dataSets contains dataSet
}