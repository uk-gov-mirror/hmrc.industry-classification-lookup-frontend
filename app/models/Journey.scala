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

case class Journey(sessionId: String, name: String){
  require(Journey.isValid(name), s"$name is not a valid journey identifier")
}

object Journey {

  val QUERY_BUILDER = "query-builder"
  val QUERY_PARSER = "query-parser"

  private val journeyNames = Seq(QUERY_PARSER, QUERY_BUILDER)

  private[Journey] def isValid(journey: String): Boolean = journeyNames contains journey
}