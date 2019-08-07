/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers

import play.api.libs.json._
import play.api.mvc.{Request, Result}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait BasicController {

  def withSessionId(f: => String => Future[Result])(implicit req: Request[_]): Future[Result] = {
    hc(req).sessionId.fold[Future[Result]](Future.successful(BadRequest("SessionId is missing from request")))(sessionId => f(sessionId.value))
  }

  def withJsBody[T](reads: Reads[T])(f: T => Future[Result])(implicit request: Request[JsValue]): Future[Result] = {
    Try(request.body.validate[T](reads)) match {
      case Success(JsSuccess(payload, _)) => f(payload)
      case Success(JsError(errs))         => Future(BadRequest(Json.prettyPrint(JsError.toJson(errs))))
      case Failure(e)                     => Future(BadRequest(s"Could not parse body due to ${e.getMessage}"))
    }
  }
}
