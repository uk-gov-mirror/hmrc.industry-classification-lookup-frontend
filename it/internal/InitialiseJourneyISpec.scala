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

package internal

import helpers.ClientSpec
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.test.FakeApplication
import play.api.test.Helpers._

class InitialiseJourneyISpec extends ClientSpec {

  override implicit lazy val app = FakeApplication(additionalConfiguration = Map(
    "play.filters.csrf.header.bypassHeaders.X-Requested-With" -> "*",
    "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck",
    "auditing.consumer.baseUri.host" -> s"$wiremockHost",
    "auditing.consumer.baseUri.port" -> s"$wiremockPort",
    "microservice.services.cachable.session-cache.host" -> s"$wiremockHost",
    "microservice.services.cachable.session-cache.port" -> s"$wiremockPort",
    "microservice.services.cachable.session-cache.domain" -> "keystore",
    "microservice.services.cachable.short-lived-cache.host" -> s"$wiremockHost",
    "microservice.services.cachable.short-lived-cache.port" -> s"$wiremockPort",
    "microservice.services.cachable.short-lived-cache.domain" -> "save4later",
    "microservice.services.auth.host" -> s"$wiremockHost",
    "microservice.services.auth.port" -> s"$wiremockPort"
  ))

  val initialiseJourneyUrl = "/internal/initialise-journey"

  val setupJson = Json.parse(
    """
      |{
      |   "redirectUrl" : "/test/uri"
      |}
    """.stripMargin
  )

  "/internal/initialise-journey" should {
    "return an OK" when {
      "the json has been validated and the journey has been setup" in {
        setupSimpleAuthMocks()

        assertFutureResponse(buildClient(initialiseJourneyUrl).withHeaders(HeaderNames.COOKIE -> getSessionCookie()).post(setupJson)) { res =>
          res.status mustBe OK
          assert(res.json.\("journeyStartUri").as[String].contains("/test/uri/search-standard-industry-classification-codes?journey="))
          assert(res.json.\("fetchResultsUri").as[String].contains("fetch-results"))
        }
      }
    }

    "return a Bad Request" when {
      "there was a problem validating the input json" in {
        setupSimpleAuthMocks()

        assertFutureResponse(buildClient(initialiseJourneyUrl).withHeaders(HeaderNames.COOKIE -> getSessionCookie()).post(Json.parse("""{"abc" : "xyz"}"""))) {
          _.status mustBe BAD_REQUEST
        }
      }
    }

    "return a Precondition failed" when {
      "no session id could be found in request" in {
        setupSimpleAuthMocks()

        assertFutureResponse(buildClient(initialiseJourneyUrl).post(Json.parse("""{"abc" : "xyz"}"""))) { res =>
          res.status mustBe PRECONDITION_FAILED
        }
      }
    }

    "return a Forbidden" when {
      "the user is not authorised" in {
        setupUnauthorised()

        assertFutureResponse(buildClient(initialiseJourneyUrl).post(setupJson)) {
          _.status mustBe FORBIDDEN
        }
      }
    }
  }
}
