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

package services

import javax.inject.Inject

import connectors.ICLConnector
import models.{SearchResults, SicCode}
import play.api.Logger
import repositories.{SicStoreRepo, SicStoreRepository}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.{ExecutionContext, Future}

class SicSearchServiceImpl @Inject()(val iCLConnector: ICLConnector,
                                     sicStoreRepo: SicStoreRepo) extends SicSearchService {
  val sicStoreRepository: SicStoreRepository = sicStoreRepo.repo

}

trait SicSearchService {

  protected val iCLConnector: ICLConnector
  protected val sicStoreRepository: SicStoreRepository

  def search(sessionId: String, query: String, journey: String, dataSet: String, sector: Option[String] = None)(implicit hc: HeaderCarrier): Future[Int] = {
    if(isLookup(query)){
      lookupSicCode(sessionId, dataSet, query)
    } else {
      searchQuery(sessionId, query, journey, dataSet, sector)
    }
  }

  def retrieveSearchResults(sessionId: String)(implicit ec: ExecutionContext): Future[Option[SearchResults]] = {
    sicStoreRepository.retrieveSicStore(sessionId).map(_.flatMap(_.searchResults))
  }

  def retrieveChoices(sessionId: String)(implicit ec: ExecutionContext): Future[Option[List[SicCode]]] = {
    sicStoreRepository.retrieveSicStore(sessionId).map(_.flatMap(_.choices))
  }

  def insertChoice(sessionId: String, sicCode: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    sicStoreRepository.insertChoice(sessionId, sicCode)
  }

  def removeChoice(sessionId: String, sicCodeToRemove: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    sicStoreRepository.removeChoice(sessionId, sicCodeToRemove)
  }

  private[services] def lookupSicCode(sessionId: String, dataSet: String, sicCode: String)(implicit hc: HeaderCarrier): Future[Int] = {
    for {
      oSicCode      <- iCLConnector.lookup(sicCode, dataSet)
      searchResults = oSicCode.fold(SearchResults(sicCode, 0, Nil, Nil))(sic => SearchResults.fromSicCode(sic))
      res           <- sicStoreRepository.updateSearchResults(sessionId, searchResults) flatMap { successful =>
        if (successful && oSicCode.isDefined) insertChoice(sessionId, oSicCode.get.sicCode) map (_ => 1) else Future.successful(0)
      }
    } yield res
  }

  private[services] def searchQuery(sessionId: String, query: String, journey: String, dataSet: String, sector: Option[String] = None)(implicit hc: HeaderCarrier): Future[Int] = {
    (for {
      searchResults <- iCLConnector.search(query, journey, dataSet, sector)
      _             <- sicStoreRepository.updateSearchResults(sessionId, searchResults) flatMap { res =>
        if (searchResults.numFound == 1) insertChoice(sessionId, searchResults.results.head.sicCode) else Future.successful(res)
      }
    } yield searchResults.numFound) recover {
      case _ =>
        Logger.error("[SicSearchService] [searchQuery] Exception encountered when attempting to fetch results from ICL")
        0
    }
  }

  private[services] def isLookup(query: String): Boolean = query.trim.matches("^(\\d){5}$")
}
