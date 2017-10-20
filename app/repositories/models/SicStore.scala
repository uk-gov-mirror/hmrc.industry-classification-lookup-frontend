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

package repositories.models

import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, _}

case class SicStore(registrationID: String, search: SicCode, choices: Option[List[SicCode]])

case class SicCode(sicCode: String, description: String)

object SicStore {
  implicit val format: Format[SicStore] = (
    (__ \ "registrationID").format[String] and
    (__ \ "search").format[SicCode] and
    (__ \ "choices").formatNullable[List[SicCode]]
  )(SicStore.apply, unlift(SicStore.unapply))
}

object SicCode {
  implicit val format: Format[SicCode] = (
    (__ \ "code").format[String] and
    (__ \ "desc").format[String]
  )(SicCode.apply, unlift(SicCode.unapply))
}