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

import javax.inject.Inject

import models.{Journey, SicStore}
import repositories.{SicStoreMongoRepository, SicStoreRepo}

import scala.concurrent.{ExecutionContext, Future}

class JourneyServiceImpl @Inject()(sicStoreRepo: SicStoreRepo) extends JourneyService {
  val sicStore: SicStoreMongoRepository = sicStoreRepo.repo
}

trait JourneyService {

  protected val sicStore: SicStoreMongoRepository

  def upsertJourney(journey: Journey)(implicit ec: ExecutionContext): Future[SicStore] = sicStore.upsertJourney(journey)

  def retrieveJourney(sessionId: String)(implicit ec: ExecutionContext): Future[Option[String]] = {
    sicStore.retrieveSicStore(sessionId).map(_.map(_.journey))
  }
}
