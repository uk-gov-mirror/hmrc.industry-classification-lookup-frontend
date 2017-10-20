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

package services

import javax.inject.{Inject, Singleton}

import connectors.ICLConnector
import repositories.{SicStoreRepo, SicStoreRepository}
import repositories.models.{SicCode, SicStore}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class SicSearchServiceImpl @Inject()(val iCLConnector: ICLConnector,
                                     sicStoreRepo: SicStoreRepo) extends SicSearchService {
  val sicStoreRepository: SicStoreRepository = sicStoreRepo.repo
}

trait SicSearchService {

  protected val iCLConnector: ICLConnector
  protected val sicStoreRepository: SicStoreRepository

  def lookupSicCode(sicCode: String)(implicit hc: HeaderCarrier): Future[Option[SicCode]] = iCLConnector.lookupSicCode(sicCode)

  def updateSearchResults(sessionId: String, searchResult: SicCode): Future[Option[SicCode]] = {
    sicStoreRepository.upsertSearchCode(sessionId, searchResult)
  }

  def retrieveSicStore(sessionId: String) : Future[Option[SicStore]] = {
    sicStoreRepository.retrieveSicStore(sessionId)
  }

  def insertChoice(sessionId: String): Future[Option[SicCode]] = {
    sicStoreRepository.insertChoice(sessionId)
  }

  def removeChoice(sessionId: String, sicCodeToRemove: String): Future[Option[SicCode]] = {
    sicStoreRepository.removeChoice(sessionId, sicCodeToRemove)
  }

}
