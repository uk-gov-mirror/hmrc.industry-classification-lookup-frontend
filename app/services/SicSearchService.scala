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
import models._
import play.api.Logger
import repositories.{SicStoreMongoRepository, SicStoreRepo}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.{ExecutionContext, Future}

class SicSearchServiceImpl @Inject()(val iCLConnector: ICLConnector,
                                     sicStoreRepo: SicStoreRepo) extends SicSearchService {
  val sicStoreRepository: SicStoreMongoRepository = sicStoreRepo.repo
}

trait SicSearchService {

  protected val iCLConnector: ICLConnector
  protected val sicStoreRepository: SicStoreMongoRepository

  def search(sessionId: String, query: String, journey: String, dataSet: String, sector: Option[String] = None)(implicit hc: HeaderCarrier): Future[Int] = {
    if(isLookup(query)){
      lookupSicCodes(sessionId, List(SicCode(query,"")))
    } else {
      searchQuery(sessionId, query, journey, dataSet, sector)
    }
  }

  def retrieveSearchResults(sessionId: String)(implicit ec: ExecutionContext): Future[Option[SearchResults]] = {
    sicStoreRepository.retrieveSicStore(sessionId).map(_.flatMap(_.searchResults))
  }

  def retrieveChoices(sessionId: String)(implicit ec: ExecutionContext): Future[Option[List[SicCodeChoice]]] = {
    sicStoreRepository.retrieveSicStore(sessionId).map(_.flatMap(_.choices))
  }

  def insertChoices(sessionId: String, sicCodes: List[SicCodeChoice])(implicit ec: ExecutionContext): Future[Boolean] =
    sicStoreRepository.insertChoices(sessionId, sicCodes)

  def removeChoice(sessionId: String, sicCodeToRemove: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    sicStoreRepository.removeChoice(sessionId, sicCodeToRemove)
  }

  def lookupSicCodes(sessionId: String, selectedCodes: List[SicCode])(implicit hc: HeaderCarrier): Future[Int] = {
    for {
      oSicCode      <- iCLConnector.lookup(getCommaSeparatedCodes(selectedCodes))
      groups        = selectedCodes.groupBy(_.sicCode)
      codes         = oSicCode map { sic =>
        SicCodeChoice(sic, groups.get(sic.sicCode).fold(List.empty[String])(nSicCodes =>
          nSicCodes.filterNot(sicCode => sicCode == sic || sicCode.description.isEmpty).map(_.description))
        )
      }
      res           <-
        if (oSicCode.nonEmpty) {
          insertChoices(sessionId, codes) map (_ => 1)
        } else {
          sicStoreRepository.updateSearchResults(sessionId, SearchResults(selectedCodes.head.sicCode, 0, Nil, Nil)) map (_ => 0)
        }
    } yield res
  }

  private[services] def getCommaSeparatedCodes(sicCodes: List[SicCode]): String = {
    sicCodes.groupBy(_.sicCode).keys.mkString(",")
  }

  private[services] def searchQuery(sessionId: String, query: String, journey: String, dataSet: String, sector: Option[String] = None)(implicit hc: HeaderCarrier): Future[Int] = {
    (for {
      oSearchResults  <- iCLConnector.search(query, journey, dataSet, sector)
      sectorObject    = sector.flatMap(sicCode => oSearchResults.sectors.find(_.code == sicCode))
      searchResults   = sectorObject.fold(oSearchResults)(s => oSearchResults.copy(currentSector = Some(s)))
      _               <- sicStoreRepository.updateSearchResults(sessionId, searchResults) flatMap { res =>
        if (searchResults.numFound == 1) lookupSicCodes(sessionId, searchResults.results) else Future.successful(res)
      }
    } yield searchResults.numFound) recover {
      case _ =>
        Logger.error("[SicSearchService] [searchQuery] Exception encountered when attempting to fetch results from ICL")
        0
    }
  }

  private[services] def isLookup(query: String): Boolean = query.trim.matches("^(\\d){5}$")

  def upsertJourney(journey: Journey)(implicit ec: ExecutionContext): Future[SicStore] = sicStoreRepository.upsertJourney(journey)

  def retrieveJourney(sessionId: String)(implicit ec: ExecutionContext): Future[Option[(String, String)]] = {
    sicStoreRepository.retrieveSicStore(sessionId).map(_.map(x => (x.journey, x.dataSet)))
  }
}
