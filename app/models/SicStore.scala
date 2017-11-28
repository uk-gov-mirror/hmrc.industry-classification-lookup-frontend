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

import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, _}
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

case class SicStore(
  registrationID: String,
  searchResults: SearchResults,
  choices: Option[List[SicCode]],
  lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC)
)

case class SicCode(
  sicCode: String,
  description: String
)

case class SearchResults(
  query: String,
  numFound: Int,
  results: List[SicCode],
  sectors: List[Sector]
)

case class Sector(
  code: String,
  name: String,
  count: Int
)

object SicStore {
  implicit val dateFormat = ReactiveMongoFormats.dateTimeFormats

  implicit val format: Format[SicStore] =
    (
      (__ \ "registrationID").format[String] and
      (__ \ "search").format[SearchResults](SearchResults.format) and
      (__ \ "choices").formatNullable[List[SicCode]] and
      (__ \ "lastUpdated").format[DateTime]
    )(SicStore.apply, unlift(SicStore.unapply))
}

object SicCode {
  implicit val format: Format[SicCode] =
    (
      (__ \ "code").format[String] and
      (__ \ "desc").format[String]
    )(SicCode.apply, unlift(SicCode.unapply))
}

object Sector {
  implicit val format: Format[Sector] =
    (
      (__ \ "code").format[String] and
      (__ \ "name").format[String] and
      (__ \ "count").format[Int]
    )(Sector.apply, unlift(Sector.unapply))
}

object SearchResults {

  def fromSicCode(sicCode: SicCode): SearchResults = SearchResults(sicCode.sicCode, 1, List(sicCode), List())

  def readsWithQuery(query: String): Reads[SearchResults] =
    (
      Reads.pure(query) and
      (__ \ "numFound").read[Int] and
      (__ \ "results").read[List[SicCode]] and
      (__ \ "sectors").read[List[Sector]]
    )(SearchResults.apply _)

  implicit val format: Format[SearchResults] =
    (
      (__ \ "query").format[String] and
      (__ \ "numFound").format[Int] and
      (__ \ "results").format[List[SicCode]] and
      (__ \ "sectors").format[List[Sector]]
    )(SearchResults.apply, unlift(SearchResults.unapply))

}
