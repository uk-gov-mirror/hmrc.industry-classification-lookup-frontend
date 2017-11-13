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

import play.api.mvc.{Request, Result}
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

package object controllers extends FrontendController {

  def withSessionId(f: => String => Future[Result])(implicit req: Request[_], ec: ExecutionContext): Future[Result] = {
    hc.sessionId match {
      case Some(sessionId) => f(sessionId.value)
      case None => Future.successful(throw new RuntimeException("No session id found in request"))
    }
  }
}
