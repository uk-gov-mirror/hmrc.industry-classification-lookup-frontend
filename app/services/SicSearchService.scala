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
import models.{SearchResults, SicCode, SicStore}
import play.api.Logger
import repositories.{SicStoreRepo, SicStoreRepository}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

@Singleton
class SicSearchServiceImpl @Inject()(val iCLConnector: ICLConnector,
                                     sicStoreRepo: SicStoreRepo) extends SicSearchService {
  val sicStoreRepository: SicStoreRepository = sicStoreRepo.repo

}

trait SicSearchService {
  protected val iCLConnector: ICLConnector
  protected val sicStoreRepository: SicStoreRepository

  def search(sessionId: String, query: String)(implicit hc: HeaderCarrier): Future[Int] = {
    if(isLookup(query)){
      lookupSicCode(sessionId, query)
    } else {
      searchQuery(sessionId, query)
    }
  }

  def retrieveSearchResults(sessionId: String)(implicit ec: ExecutionContext): Future[Option[SearchResults]] = {
    retrieveSicStore(sessionId).map(_.map(_.searchResults))
  }

  def retrieveChoices(sessionId: String)(implicit ec: ExecutionContext): Future[Option[List[SicCode]]] = {
    retrieveSicStore(sessionId).map(_.flatMap(_.choices))
  }

  def insertChoice(sessionId: String, sicCode: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    sicStoreRepository.insertChoice(sessionId, sicCode)
  }

  def removeChoice(sessionId: String, sicCodeToRemove: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    sicStoreRepository.removeChoice(sessionId, sicCodeToRemove)
  }

  private[services] def lookupSicCode(sessionId: String, sicCode: String)(implicit hc: HeaderCarrier): Future[Int] = {
    iCLConnector.lookup(sicCode) flatMap {
      case Some(sic) =>
        updateSearchResults(sessionId, SearchResults.fromSicCode(sic)) flatMap ( successful =>
          if(successful) insertChoice(sessionId, sic.sicCode) map (_ => 1) else Future.successful(0)
        )
      case None => Future.successful(0)
    }
  }

  private[services] def searchQuery(sessionId: String, query: String)(implicit hc: HeaderCarrier): Future[Int] = {
    iCLConnector.search(query) flatMap ( sic => {
        val store = sic.numFound match {
          case 1 =>
              updateSearchResults(sessionId, SearchResults.fromSicCode(sic.results.head)) flatMap { _ =>
                insertChoice(sessionId, sic.results.head.sicCode)
              }
          case 0 => Future.successful(false)
          case _ => updateSearchResults(sessionId, sic)
        }

        store map (_ => sic.numFound)
      }
    ) recover {
      case e =>
        Logger.error("[SicSearchService] [searchQuery] Exception encountered when attempting to fetch results from ICL")
        0
    }
  }

  private[services] def updateSearchResults(sessionId: String, searchResult: SearchResults)(implicit ec: ExecutionContext) : Future[Boolean] = {
    sicStoreRepository.updateSearchResults(sessionId, searchResult)
  }

  private[services] def retrieveSicStore(sessionId: String)(implicit ec: ExecutionContext) : Future[Option[SicStore]] = {
    sicStoreRepository.retrieveSicStore(sessionId)
  }

  private[services] def isLookup(query: String): Boolean = query.trim.matches("^(\\d){8}$")
}
