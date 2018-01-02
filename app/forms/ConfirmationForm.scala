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

import models.Confirmation
import play.api.data.{Form, FormError, Forms, Mapping}
import play.api.data.Forms._
import play.api.data.format.Formatter

object ConfirmationForm {

  implicit def confirmationFormatter: Formatter[String] = new Formatter[String] {

    def validate(entry: String): Either[Seq[FormError], String] = {
      entry match {
        case ""  => Left(Seq(FormError("addAnother", "errors.invalid.sic.confirm")))
        case select => Right(select)
      }
    }

    override def bind(key: String, data: Map[String, String]) = {
      validate(data.getOrElse(key, ""))
    }

    override def unbind(key: String, value: String) = Map(key -> value)
  }

  def confirmationField: Mapping[String] = Forms.of[String](confirmationFormatter)

  val form = Form(
    mapping(
      "addAnother" -> confirmationField
    )(Confirmation.apply)(Confirmation.unapply)
  )
}
