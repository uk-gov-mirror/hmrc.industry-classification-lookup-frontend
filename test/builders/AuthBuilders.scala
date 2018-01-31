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

package builders

import controllers.ControllerSpec
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.mvc._
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.{AuthConnector, MissingBearerToken}

import scala.concurrent.Future

object AuthBuilders extends AuthBuilders with ControllerSpec

trait AuthBuilders extends SessionBuilder {
  self: ControllerSpec =>

  val userId: String
  val mockAuthConnector: AuthConnector

  def mockAuthorisedUser(future: Future[Unit]) {
    when(mockAuthConnector.authorise[Unit](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn {
      future
    }
  }

  def showWithUnauthorisedUser(action: Action[AnyContent])(test: Future[Result] => Any) {
    mockAuthorisedUser(Future.failed(MissingBearerToken("")))
    val result = action.apply()(FakeRequest())
    test(result)
  }

  def showWithAuthorisedUser(action: Action[AnyContent], mockAuthConnector: AuthConnector)(test: Future[Result] => Any) {
    mockAuthorisedUser(Future.successful({}))
    val result = action.apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def showWithAuthorisedUser(action: Action[AnyContent], mockAuthConnector: AuthConnector, request: Request[_])(test: Future[Result] => Any) {
    mockAuthorisedUser(Future.successful({}))
    val result = action(request).run
    test(result)
  }

  def submitWithUnauthorisedUser(action: Action[AnyContent], request: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    mockAuthorisedUser(Future.failed(MissingBearerToken("")))
    val result = action.apply(SessionBuilder.updateRequestFormWithSession(request, ""))
    test(result)
  }

  def submitWithAuthorisedUser(action: Action[AnyContent], mockAuthConnector: AuthConnector, request: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    mockAuthorisedUser(Future.successful({}))
    val result = action.apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }

  def requestWithAuthorisedUser[T <: AnyContent](action: Action[AnyContent], request: FakeRequest[T], mockAuthConn: AuthConnector)
                                                (test: Future[Result] => Any) {
    mockAuthorisedUser(Future.successful({}))
    val result = action(updateRequestWithSession(request))
    test(result)
  }

  def requestWithAuthorisedUser[T <: AnyContent](action: Action[AnyContent], request: FakeRequest[T])
                                                (test: Future[Result] => Any) {
    mockAuthorisedUser(Future.successful({}))
    val result = action(updateRequestWithSession(request))
    test(result)
  }

}
