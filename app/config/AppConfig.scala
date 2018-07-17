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

package config

import java.nio.charset.Charset
import java.util.Base64

import javax.inject.Inject
import play.api.Configuration
import uk.gov.hmrc.play.config.{AssetsConfig, OptimizelyConfig}

class FrontendAppConfig @Inject()(configuration: Configuration) extends AppConfig {

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  private val contactHost                  = configuration.getString(s"contact-frontend.host").getOrElse("")
  private val contactFormServiceIdentifier = "MyService"

  override lazy val analyticsToken            = loadConfig(s"google-analytics.token")
  override lazy val analyticsHost             = loadConfig(s"google-analytics.host")
  override lazy val reportAProblemPartialUrl  = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  override lazy val reportAProblemNonJSUrl    = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"

  private def whitelistConfig(key: String): Seq[String] =
    Some(new String(Base64.getDecoder.decode(loadConfig(key)), "UTF-8")).map(_.split(",")).getOrElse(Array.empty).toSeq

  private def loadStringConfigBase64(key : String) : String = {
    new String(Base64.getDecoder.decode(configuration.getString(key).getOrElse("")), Charset.forName("UTF-8"))
  }

  lazy val whitelist         = whitelistConfig("whitelist")
  lazy val whitelistExcluded = whitelistConfig("whitelist-excluded")

  lazy val csrfBypassValue   = loadStringConfigBase64("Csrf-Bypass-value")
  lazy val uriWhiteList      = configuration.getStringSeq("csrfexceptions.whitelist").getOrElse(Seq.empty).toSet

  override lazy val assetsConfig: AssetsConfig = new AssetsConfig {
    override lazy val assetsUrl: String = loadConfig("assets.url")
    override lazy val assetsVersion: String = loadConfig("assets.version")
    override lazy val assetsPrefix: String = assetsUrl + assetsVersion
  }

  override lazy val optimizelyConfig: OptimizelyConfig = new OptimizelyConfig {
    override def optimizelyBaseUrl: String = configuration.getString("optimizely.url").getOrElse("")
    override def optimizelyProjectId: Option[String] = configuration.getString("optimizely.projectId")
  }
}

trait AppConfig {
  val analyticsToken: String
  val analyticsHost: String
  val reportAProblemPartialUrl: String
  val reportAProblemNonJSUrl: String

  val whitelist: Seq[String]
  val whitelistExcluded: Seq[String]

  val csrfBypassValue: String
  val uriWhiteList: Set[String]

  val assetsConfig: AssetsConfig
  val optimizelyConfig: OptimizelyConfig
}
