/*
 * Copyright 2021 HM Revenue & Customs
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

import akka.stream.Materializer
import javax.inject.Inject
import play.api.http.HttpVerbs.POST
import play.api.mvc.{Filter, RequestHeader, Result}

import scala.concurrent.Future

class CSRFExceptionsFilter @Inject()(appConfig: AppConfig, implicit val mat: Materializer) extends Filter {

  private lazy val allowlist: Set[String] = appConfig.uriAllowList

  private def internalRoutesBypass(rh: RequestHeader): RequestHeader = {
    (rh.method, allowlist.exists(rh.path.matches(_))) match {
      case (POST, true) => applyHeaders(rh)
      case _ => removeHeaders(rh)
    }
  }

  private def applyHeaders(rh: RequestHeader): RequestHeader = rh.copy(headers = rh.headers.add("Csrf-Bypass" -> appConfig.csrfBypassValue))

  private def removeHeaders(rh: RequestHeader): RequestHeader = rh.copy(headers = rh.headers.remove("Csrf-Bypass"))

  override def apply(f: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {
    f(internalRoutesBypass(rh))
  }
}