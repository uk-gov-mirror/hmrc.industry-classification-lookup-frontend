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

package controllers

import models.setup.{Identifiers, JourneyData}
import play.api.Logger
import play.api.mvc.{Request, Result}
import services.JourneyService

import scala.concurrent.Future

trait JourneyManager {

  val journeyService: JourneyService

  def hasJourney(identifiers: Identifiers)(f: => JourneyData => Future[Result])(implicit req: Request[_]): Future[Result] = {
    journeyService.getJourney(identifiers) flatMap { journeyData =>
      f(journeyData)
    } recoverWith {
      case err =>
        Logger.error(s"[hasJourney] - msg: ${err.getMessage}")
        throw err
    }
  }
}