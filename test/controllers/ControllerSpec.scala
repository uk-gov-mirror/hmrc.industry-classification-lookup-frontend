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

package controllers

import akka.actor.Cancellable
import akka.stream.{Attributes, ClosedShape, Graph, Materializer}
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito.{reset, when}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.stubbing.OngoingStubbing
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.{AnyContent, RequestHeader}
import play.api.test.FakeRequest
import services.{JourneyService, SicSearchService}
import uk.gov.hmrc.http.{HeaderNames, SessionKeys}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration.FiniteDuration

trait ControllerSpec extends UnitSpec with MockitoSugar with NoMaterializer with WithFakeApplication {

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockSicSearchService: SicSearchService = mock[SicSearchService]
  val mockJourneyService: JourneyService = mock[JourneyService]
  val mockMessagesAPI: MessagesApi = mock[MessagesApi]

  def mockWithJourney(sessionId: String, journey: Option[String]): OngoingStubbing[Future[Option[String]]] = {
    when(mockJourneyService.retrieveJourney(eqTo(sessionId))(any()))
      .thenReturn(Future.successful(journey))
  }

  def resetMocks() {
    reset(mockAuthConnector, mockSicSearchService, mockJourneyService)
  }

  implicit class FakeRequestImps[T <: AnyContent](fakeRequest: FakeRequest[T]) {
    def withSessionId(sessionId: String): FakeRequest[T] = {
      fakeRequest.withHeaders(SessionKeys.sessionId -> sessionId, HeaderNames.xSessionId -> sessionId)
    }
  }
}

trait MockMessages {

  val mockMessagesAPI: MessagesApi

  val lang = Lang("en")
  val messages = Messages(lang, mockMessagesAPI)

  val MOCKED_MESSAGE = "mocked message"

  def mockAllMessages: OngoingStubbing[String] = {
    when(mockMessagesAPI.preferred(any[RequestHeader]()))
      .thenReturn(messages)

    when(mockMessagesAPI.apply(any[String](), any())(any()))
      .thenReturn(MOCKED_MESSAGE)
  }

  def mockMessage(key: String): OngoingStubbing[String] = {
    when(mockMessagesAPI.preferred(any[RequestHeader]()))
      .thenReturn(messages)

    when(mockMessagesAPI.apply(eqTo(key), any())(any()))
      .thenReturn(MOCKED_MESSAGE)
  }
}

trait NoMaterializer {
  /**
    * Taken from play.api.test.Helpers (private)
    * Avoids using an Application to instantiate a materializer
    **/
  implicit val noMaterializer: Materializer = new Materializer {
    override def withNamePrefix(name: String): Materializer =
      throw new UnsupportedOperationException("NoMaterializer cannot be named")
    override def materialize[Mat](runnable: Graph[ClosedShape, Mat]): Mat =
      throw new UnsupportedOperationException("NoMaterializer cannot materialize")
    override def materialize[Mat](runnable: Graph[ClosedShape, Mat], initialAttributes: Attributes): Mat =
      throw new UnsupportedOperationException("NoMaterializer cannot materialize")
    override def executionContext: ExecutionContextExecutor =
      throw new UnsupportedOperationException("NoMaterializer does not provide an ExecutionContext")
    def scheduleOnce(delay: FiniteDuration, task: Runnable): Cancellable =
      throw new UnsupportedOperationException("NoMaterializer cannot schedule a single event")
    def schedulePeriodically(initialDelay: FiniteDuration, interval: FiniteDuration, task: Runnable): Cancellable =
      throw new UnsupportedOperationException("NoMaterializer cannot schedule a repeated event")
  }
}


