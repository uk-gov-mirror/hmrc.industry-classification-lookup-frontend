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

import java.time.LocalDateTime

import helpers.Base64Helper._
import helpers.ClientSpec
import models.setup.{Identifiers, JourneyData, JourneySetup}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.crypto.DefaultCookieSigner
import play.api.libs.ws.{WSRequest, WSResponse}
import reactivemongo.api.commands.WriteResult
import repositories.JourneyDataRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AllowlistFilterISpec extends ClientSpec {

  override lazy val cookieSigner: DefaultCookieSigner = app.injector.instanceOf[DefaultCookieSigner]

  trait Setup {
    val repo: JourneyDataRepository = app.injector.instanceOf[JourneyDataRepository]

    await(repo.drop)
    await(repo.ensureIndexes)

    def insertIntoDb(journeyData: JourneyData): Future[WriteResult] = repo.insert(journeyData)
  }

  val extraConfig: Map[String, Any] = Map(
    "play.http.filters" -> "config.ProductionFilters",
    "allowlist-excluded" -> "/ping/ping,/healthcheck".toBase64,
    "allowlist" -> "allowlistIP".toBase64
  )

  val searchUri = "/sic-search/testJourneyId/search-standard-industry-classification-codes"

  val journeyData = JourneyData(Identifiers("testJourneyId", "test-session-id"), "redirectUrl", JourneySetup(queryBooster = Some(true)), LocalDateTime.now())

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(testAppConfig ++ extraConfig)
    .build()

  "FrontendAppConfig must return a valid config item" when {
    lazy val appConfig = app.injector.instanceOf[AppConfig]


    "the allowlist exclusion paths are requested" in {
      appConfig.allowlistExcluded mustBe Seq("/ping/ping", "/healthcheck")
    }

    "the allowlist IPs are requested" in {
      appConfig.allowlist mustBe Seq("allowlistIP")
    }
  }


  "ProductionFrontendGlobal" must {

    "allow requests through the allowlist" when {

      "the request is sent from a allowlisted ip address" in new Setup {
        val client: WSRequest = buildClient(searchUri)
          .withTrueClientIPHeader("allowlistIP")
          .withSessionIdHeader()

        mockAuthorise()

        await(insertIntoDb(journeyData))

        val response: WSResponse = await(client.get())
        response.status mustBe 200
      }

      "the request is not sent from a allow-listed IP but the requested Url is a allow-listed Url" in {
        val client = buildClient("/ping/ping")
          .withTrueClientIPHeader("allowlistIP")
          .withSessionIdHeader()

        mockAuthorise()

        val response = await(client.get())
        response.status mustBe 200
      }
    }

    "redirect the request to the outage page" when {

      "the request is not sent from a allow-listed IP and the requested Url is not a allow-listed Url" in {
        val client = buildClient(searchUri)
          .withTrueClientIPHeader("nonAllowlistIP")
          .withSessionIdHeader()

        mockAuthorise()

        val response = await(client.get())
        response.status mustBe 303
        response.redirectLocation mustBe Some("https://www.tax.service.gov.uk/outage-sic-search")
      }
    }

    "return a 501 when the request is missing a TrueClientIp header" in {
      val client = buildClient(searchUri)
        .withSessionIdHeader()

      mockAuthorise()

      val response = await(client.get())
      response.status mustBe 501
    }
  }
}