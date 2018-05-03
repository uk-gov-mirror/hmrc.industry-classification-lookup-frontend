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

package helpers

import connectors.ICLConnector
import org.scalatest.mockito.MockitoSugar
import repositories.SicStoreMongoRepository
import services.{JourneyService, JourneySetupService, SicSearchService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.http.HttpClient

trait MockedComponents {
  self: MockitoSugar =>

  val mockWSHttp           = mock[HttpClient]

  //Connector mocks
  val mockAuthConnector    = mock[AuthConnector]
  val mockAuditConnector   = mock[AuditConnector]
  val mockICLConnector     = mock[ICLConnector]

  //Service mocks
  val mockJourneyService   = mock[JourneyService]
  val mockSicSearchService = mock[SicSearchService]
  val mockJourneySetupService = mock[JourneySetupService]

  //Repo mocks
  val mockSicStoreRepo     = mock[SicStoreMongoRepository]
}
