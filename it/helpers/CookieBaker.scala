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

package helpers

import java.net.{URLDecoder, URLEncoder}
import java.util.UUID

import play.api.libs.crypto.DefaultCookieSigner
import play.api.libs.ws.WSCookie
import uk.gov.hmrc.crypto.{CompositeSymmetricCrypto, Crypted, PlainText}
import uk.gov.hmrc.http.SessionKeys

trait CookieBaker {

  val cookieSigner: DefaultCookieSigner

  val sessionId         = s"stubbed-${UUID.randomUUID}"

  val cookieKey = "gvBoGdgzqG1AarzF1LY0zQ=="
  def cookieValue(sessionData: Map[String,String]) = {
    def encode(data: Map[String, String]): PlainText = {
      val encoded = data.map {
        case (k, v) => URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
      }.mkString("&")
      val key = "yNhI04vHs9<_HWbC`]20u`37=NGLGYY5:0Tg5?y`W<NoJnXWqmjcgZBec@rOxb^G".getBytes
      PlainText(cookieSigner.sign(encoded, key) + "-" + encoded)
    }

    val encodedCookie = encode(sessionData)
    val encrypted = CompositeSymmetricCrypto.aesGCM(cookieKey, Seq()).encrypt(encodedCookie).value

    s"""mdtp="$encrypted"; Path=/; HTTPOnly"; Path=/; HTTPOnly"""
  }

  def getCookieData(cookie: WSCookie): Map[String, String] = {
    getCookieData(cookie.value)
  }

  def getCookieData(cookieData: String): Map[String, String] = {

    val decrypted = CompositeSymmetricCrypto.aesGCM(cookieKey, Seq()).decrypt(Crypted(cookieData)).value
    val result = decrypted.split("&")
      .map(_.split("="))
      .map { case Array(k, v) => (k, URLDecoder.decode(v))}
      .toMap

    result
  }

  private def cookieData(additionalData: Map[String, String], timeStampRollback: Long, sessionID: String): Map[String, String] = {

    val timeStamp = new java.util.Date().getTime
    val rollbackTimestamp = (timeStamp - timeStampRollback).toString

    Map(
      SessionKeys.sessionId -> sessionID,
      SessionKeys.userId -> "/auth/oid/1234567890",
      SessionKeys.token -> "token",
      SessionKeys.authProvider -> "GGW",
      SessionKeys.lastRequestTimestamp -> rollbackTimestamp
    ) ++ additionalData
  }

  def getSessionCookie(additionalData: Map[String, String] = Map(), timeStampRollback: Long = 0, sessionID: String = sessionId): String = {
    cookieValue(cookieData(additionalData, timeStampRollback, sessionID))
  }
}
