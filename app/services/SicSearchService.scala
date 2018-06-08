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

import connectors.ICLConnector
import javax.inject.Inject
import models._
import models.setup.{JourneyData, JourneySetup}
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

  def search(journeyData: JourneyData, query: String, sector: Option[String] = None)(implicit hc: HeaderCarrier): Future[Int] = {
    if(isLookup(query)){
      lookupSicCodes(journeyData, List(SicCode(query,"")))
    } else {
      searchQuery(journeyData, query, sector)
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

  def lookupSicCodes(journeyData: JourneyData, selectedCodes: List[SicCode])(implicit hc: HeaderCarrier): Future[Int] = {
     def fiteredListOfSicCodeChoice(sicCodesUnfiltered: List[SicCode], groups: Map[String, List[SicCode]] ): List[SicCodeChoice] = {
      sicCodesUnfiltered map { sic =>
        SicCodeChoice(sic, groups.get(sic.sicCode).fold(List.empty[String])(nSicCodes =>
          nSicCodes.filterNot(sicCode => sicCode == sic || sicCode.description.isEmpty).map(_.description))
        )
      }
    }

    for {
      oSicCode            <- iCLConnector.lookup(getCommaSeparatedCodes(selectedCodes))
      groups              =  selectedCodes.groupBy(_.sicCode)
      filteredCodes       =  fiteredListOfSicCodeChoice(oSicCode, groups)
      res             <- if (oSicCode.nonEmpty) {
        insertChoices(journeyData.identifiers.sessionId, filteredCodes) map (_ => 1)
      } else {
        sicStoreRepository.upsertSearchResults(journeyData.identifiers.sessionId, SearchResults(selectedCodes.head.sicCode, 0, Nil, Nil)) map (_ => 0)
      }
    } yield res
  }

  private[services] def getCommaSeparatedCodes(sicCodes: List[SicCode]): String = {
    sicCodes.groupBy(_.sicCode).keys.mkString(",")
  }

  private[services] def searchQuery(journeyData: JourneyData, query: String, sector: Option[String] = None)(implicit hc: HeaderCarrier): Future[Int] = {
    (for {
      oSearchResults  <- iCLConnector.search(query, journeyData.journeySetupDetails, sector)
      sectorObject    = sector.flatMap(sicCode => oSearchResults.sectors.find(_.code == sicCode))
      searchResults   = sectorObject.fold(oSearchResults)(s => oSearchResults.copy(currentSector = Some(s)))
      _               <- sicStoreRepository.upsertSearchResults(journeyData.identifiers.sessionId, searchResults) flatMap { res =>
        if (searchResults.numFound == 1) lookupSicCodes(journeyData, searchResults.results) else Future.successful(res)
      }
    } yield searchResults.numFound) recover {
      case e =>
        Logger.error(s"[SicSearchService] [searchQuery] Exception encountered when attempting to fetch results from ICL ${e.getMessage}")
        0
    }
  }

  private[services] def isLookup(query: String): Boolean = query.trim.matches("^(\\d){5}$")
}