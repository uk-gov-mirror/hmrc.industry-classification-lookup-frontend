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

package helpers.mocks

import config.AppConfig
import helpers.UnitTestSpec
import uk.gov.hmrc.play.config.{AssetsConfig, OptimizelyConfig}

trait MockAppConfig {
  self: UnitTestSpec =>

  implicit val mockAppConfig: AppConfig = new AppConfig {
    override val reportAProblemNonJSUrl    = ""
    override val reportAProblemPartialUrl  = ""
    override val whitelist                 = Seq()
    override val whitelistExcluded         = Seq()
    override val analyticsToken            = ""
    override val analyticsHost             = ""
    override val csrfBypassValue: String   = ""
    override val uriWhiteList: Set[String] = Set()
    override val assetsConfig: AssetsConfig = new AssetsConfig {
      override lazy val assetsUrl: String = ""
      override lazy val assetsVersion: String = ""
    }
    override val optimizelyConfig: OptimizelyConfig = new OptimizelyConfig {
      override def optimizelyBaseUrl: String = ""
      override def optimizelyProjectId: Option[String] = None
    }
  }
}
