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

package helpers.mocks

import models.setup.JourneySetup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import reactivemongo.api.commands.{DefaultWriteResult, WriteResult}
import repositories.JourneyDataRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait MockJourneyDataRepo extends MockitoSugar with BeforeAndAfterEach {
  self: PlaySpec =>

  val mockJourneyDataRepo = mock[JourneyDataRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockJourneyDataRepo)
  }

  def mockWR(success: Boolean) = DefaultWriteResult(success, 1, Seq.empty, None, None, None)

  def mockInitialiseJourney(success: Boolean): OngoingStubbing[Future[WriteResult]] = {
    when(mockJourneyDataRepo.initialiseJourney(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future(mockWR(success)))
  }

  def mockGetRedirectUrl(returnData: Future[String]): OngoingStubbing[Future[String]] = {
    when(mockJourneyDataRepo.getRedirectUrl(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(returnData)
  }

  def mockGetMessagesFor[T](returnData: Future[Option[T]]): OngoingStubbing[Future[Option[T]]] = {
    when(mockJourneyDataRepo.getMessagesFor[T](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(returnData)
  }

  def mockGetSetupDetails(returnData: Future[JourneySetup]): OngoingStubbing[Future[JourneySetup]] = {
    when(mockJourneyDataRepo.getSetupDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(returnData)
  }
}
