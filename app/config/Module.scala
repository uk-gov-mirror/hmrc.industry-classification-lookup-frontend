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

package config

import com.google.inject.AbstractModule
import connectors.{ICLConnector, ICLConnectorImpl}
import controllers._
import controllers.test.{TestSetupController, TestSetupControllerImpl}
import services.{JourneyService, JourneyServiceImpl, SicSearchService, SicSearchServiceImpl}
import uk.gov.hmrc.play.config.inject.{DefaultServicesConfig, ServicesConfig}

class Module extends AbstractModule {
  override def configure(): Unit = {

    bindControllers()
    bindServices()

    bind(classOf[ServicesConfig]) to classOf[DefaultServicesConfig]
    bind(classOf[ICLConnector]) to classOf[ICLConnectorImpl]
    bind(classOf[FrontendAuthConnector]) to classOf[FrontendAuthConnectorImpl]
  }

  def bindControllers() {
    bind(classOf[SicSearchController]) to classOf[SicSearchControllerImpl]
    bind(classOf[ChooseActivityController]) to classOf[ChooseActivityControllerImpl]
    bind(classOf[ConfirmationController]) to classOf[ConfirmationControllerImpl]
    bind(classOf[SignInOutController]) to classOf[SignInOutControllerImpl]
    bind(classOf[TestSetupController]) to classOf[TestSetupControllerImpl]
  }

  def bindServices() {
    bind(classOf[SicSearchService]) to classOf[SicSearchServiceImpl]
    bind(classOf[JourneyService]) to classOf[JourneyServiceImpl]
  }
}
