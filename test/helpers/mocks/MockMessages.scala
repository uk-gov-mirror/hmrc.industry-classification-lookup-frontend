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

package helpers.mocks

import controllers.Ok
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.{RequestHeader, Result}
import play.mvc.Http

import scala.io.Source

trait MockMessages {

  object MockMessages extends MessagesApi {
    lazy val messagesMap: Map[String, String] = Source.fromFile("conf/messages").getLines().foldLeft(Map[String, String]()){
      case (map, line) => val splitLine = line.replaceAll("''", "'").split("=", 2).map(_.trim)
        map + (splitLine.head -> splitLine.last)
    }

    override def messages: Map[String, Map[String, String]] = Map()
    override def preferred(candidates: Seq[Lang]): Messages = Messages(Lang("en"), this)
    override def preferred(request: RequestHeader): Messages = Messages(Lang("en"), this)
    override def preferred(request: Http.RequestHeader): Messages = Messages(Lang("en"), this)
    override def setLang(result: Result, lang: Lang): Result = Ok
    override def clearLang(result: Result): Result = Ok
    override def apply(key: String, args: Any*)(implicit lang: Lang): String = args.zipWithIndex.foldLeft(messagesMap.getOrElse(key, key)){
      case (message, (argument, index)) => message.replaceAll(s"\\{$index\\}", s"$argument")
    }
    override def apply(keys: Seq[String], args: Any*)(implicit lang: Lang): String = keys.mkString(" ")
    override def translate(key: String, args: Seq[Any])(implicit lang: Lang): Option[String] = None
    override def isDefinedAt(key: String)(implicit lang: Lang): Boolean = false
    override def langCookieName: String = "en"
    override def langCookieSecure: Boolean = false
    override def langCookieHttpOnly: Boolean = false
  }
}
