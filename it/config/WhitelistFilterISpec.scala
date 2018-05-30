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
import reactivemongo.api.commands.WriteResult
import repositories.{JourneyDataMongoRepository, JourneyDataRepo}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WhitelistFilterISpec extends ClientSpec {

  trait Setup {
    val repo: JourneyDataMongoRepository = fakeApplication.injector.instanceOf[JourneyDataRepo].store

    await(repo.drop)
    await(repo.ensureIndexes)

    def insertIntoDb(journeyData: JourneyData): Future[WriteResult] = repo.insert(journeyData)
  }

  val extraConfig: Map[String, Any] = Map(
    "play.http.filters" -> "config.ProductionFilters",
    "whitelist-excluded" -> "/ping/ping,/healthcheck".toBase64,
    "whitelist" -> "whitelistIP".toBase64
  )

  val searchUri = "/sic-search/testJourneyId/search-standard-industry-classification-codes"

  val journeyData = JourneyData(Identifiers("testJourneyId", "test-session-id"), "redirectUrl", None, JourneySetup(), LocalDateTime.now())

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(testAppConfig ++ extraConfig)
    .build()

  "FrontendAppConfig must return a valid config item" when {
    lazy val frontendAppConfig = app.injector.instanceOf[FrontendAppConfig]


    "the whitelist exclusion paths are requested" in {
      frontendAppConfig.whitelistExcluded mustBe Seq("/ping/ping", "/healthcheck")
    }

    "the whitelist IPs are requested" in {
      frontendAppConfig.whitelist mustBe Seq("whitelistIP")
    }
  }


  "ProductionFrontendGlobal" must {

    "allow requests through the whitelist" when {

      "the request is sent from a whitelisted ip address" in new Setup {
        val client = buildClient(searchUri)
          .withTrueClientIPHeader("whitelistIP")
          .withSessionIdHeader()

        mockAuthorise()

        await(insertIntoDb(journeyData))

        val response = await(client.get())
        response.status mustBe 303
        response.redirectLocation mustBe Some("/testJourneyId/setup-journey")
      }

      "the request is not sent from a white-listed IP but the requested Url is a white-listed Url" in {
        val client = buildClient("/ping/ping")
          .withTrueClientIPHeader("whitelistIP")
          .withSessionIdHeader()

        mockAuthorise()

        val response = await(client.get())
        response.status mustBe 200
      }
    }

    "redirect the request to the outage page" when {

      "the request is not sent from a white-listed IP and the requested Url is not a white-listed Url" in {
        val client = buildClient(searchUri)
          .withTrueClientIPHeader("nonWhitelistIP")
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
