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

package connectors

import javax.inject.Inject

import config.WSHttp
import play.api.Logger
import repositories.models.SicCode
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, NotFoundException}
import uk.gov.hmrc.play.config.inject.ServicesConfig

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ICLConnectorImpl @Inject()(config : ServicesConfig) extends ICLConnector {
  val http: WSHttp = WSHttp
  val ICLUrl: String = config.baseUrl("industry-classification-lookup")
}

trait ICLConnector {
  val http: HttpGet
  val ICLUrl: String

  def lookupSicCode(sicCode: String)(implicit hc: HeaderCarrier): Future[Option[SicCode]] = {
    http.GET[SicCode](s"$ICLUrl/industry-classification-lookup/lookup/$sicCode") map Some.apply recover {
      case _: NotFoundException =>
        Logger.error(s"[Lookup] Looking up sic code : $sicCode returned a 404")
        None
    }
  }
}
