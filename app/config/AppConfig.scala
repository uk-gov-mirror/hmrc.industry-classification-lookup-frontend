/*
 * Copyright 2020 HM Revenue & Customs
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

package config

import java.nio.charset.Charset
import java.util.Base64

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.config.{AssetsConfig, OptimizelyConfig}

@Singleton
class AppConfig @Inject()(configuration: ServicesConfig,
                          assetsConfig: AssetsConfig,
                          optimizelyConfig: OptimizelyConfig) {

  def loadConfig(key: String): String = configuration.getString(key)

  lazy val industryClassificationLookupBackend: String = configuration.baseUrl("industry-classification-lookup")

  private lazy val contactHost = configuration.getString(s"contact-frontend.host")
  private val contactFormServiceIdentifier = "MyService"

  lazy val analyticsToken: String = loadConfig(s"google-analytics.token")
  lazy val analyticsHost: String = loadConfig(s"google-analytics.host")
  lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  lazy val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"

  private def whitelistConfig(key: String): Seq[String] =
    Some(new String(Base64.getDecoder.decode(loadConfig(key)), "UTF-8")).map(_.split(",")).getOrElse(Array.empty).toSeq

  private def loadStringConfigBase64(key: String): String = {
    new String(Base64.getDecoder.decode(configuration.getString(key)), Charset.forName("UTF-8"))
  }

  lazy val whitelist: Seq[String] = whitelistConfig("whitelist")
  lazy val whitelistExcluded: Seq[String] = whitelistConfig("whitelist-excluded")

  lazy val csrfBypassValue: String = loadStringConfigBase64("Csrf-Bypass-value")
  lazy val uriWhiteList: Set[String] = configuration.getString("csrfexceptions.whitelist").split(",").toSet
}