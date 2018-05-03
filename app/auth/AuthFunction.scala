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

package auth

import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, _}
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

trait AuthFunction extends FrontendController with AuthorisedFunctions {

  val loginURL: String

  def userAuthorised(api: Boolean = false)(body: => Future[Result])(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
    authorised() {
      body
    }(hc, implicitly).recover(if(api) apiAuthErrorHandling() else authErrorHandling)
  }

  def authErrorHandling()(implicit request: Request[_]):PartialFunction[Throwable, Result] = {
    case _: NoActiveSession        => Redirect(loginURL)
    case e: AuthorisationException =>
      Logger.error("Unexpected auth exception ", e)
      InternalServerError
  }

  def apiAuthErrorHandling()(implicit request: Request[_]):PartialFunction[Throwable, Result] = {
    case _: NoActiveSession        => Forbidden
    case _: NotFoundException      => Forbidden
    case e: AuthorisationException =>
      Logger.error("Unexpected auth exception ", e)
      InternalServerError
  }
}
