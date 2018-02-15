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

package config

import helpers.ClientSpec
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import helpers.Base64Helper._

class WhitelistFilterISpec extends ClientSpec {

  val extraConfig: Map[String, Any] = Map(
    "play.http.filters" -> "config.ProductionFilters",
    "whitelist-excluded" -> "/ping/ping,/healthcheck".toBase64,
    "whitelist" -> "11.22.33.44".toBase64
  )

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(testAppConfig ++ extraConfig)
    .build()

  "FrontendAppConfig must return a valid config item" when {
    lazy val frontendAppConfig = app.injector.instanceOf[FrontendAppConfig]


    "the whitelist exclusion paths are requested" in {
      frontendAppConfig.whitelistExcluded mustBe Seq("/ping/ping", "/healthcheck")
    }

    "the whitelist IPs are requested" in {
      frontendAppConfig.whitelist mustBe Seq("11.22.33.44")
    }
  }


  "ProductionFrontendGlobal" must {

    "allow requests through the whitelist" when {

      "the request is sent from a whitelisted ip address" in {
        val client = buildClient("/sic-search/enter-keywords")
          .withTrueClientIPHeader("11.22.33.44")
          .withSessionIdHeader()

        mockAuthorise()

        val response = await(client.get())
        response.status mustBe 303
        response.redirectLocation mustBe Some("/setup-journey")
      }

      "the request is not sent from a white-listed IP but the requested Url is a white-listed Url" in {
        val client = buildClient("/ping/ping")
          .withTrueClientIPHeader("11.22.33.44")
          .withSessionIdHeader()

        mockAuthorise()

        val response = await(client.get())
        response.status mustBe 200
      }
    }

    "redirect the request to the outage page" when {

      "the request is not sent from a white-listed IP and the requested Url is not a white-listed Url" in {
        val client = buildClient("/sic-search/enter-keywords")
          .withTrueClientIPHeader("00.00.00.01")
          .withSessionIdHeader()

        mockAuthorise()

        val response = await(client.get())
        response.status mustBe 303
        response.redirectLocation mustBe Some("https://www.tax.service.gov.uk/outage-sic-search")
      }
    }

    "return a 501 when the request is missing a TrueClientIp header" in {
      val client = buildClient("/sic-search/enter-keywords")
        .withSessionIdHeader()

      mockAuthorise()

      val response = await(client.get())
      response.status mustBe 501
    }
  }
}
