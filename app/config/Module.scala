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

package config

import _root_.connectors.{ICLConnector, ICLConnectorImpl}
import com.google.inject.AbstractModule
import controllers._
import controllers.internal.{ApiController, ApiControllerImpl}
import controllers.test.{TestSetupController, TestSetupControllerImpl}
import services._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.whitelist.AkamaiWhitelistFilter

class Module extends AbstractModule {
  override def configure(): Unit = {
    bindOther()
    bindConnectors()
    bindServices()
    bindControllers()
  }

  private def bindOther(): Unit = {
    bind(classOf[ServicesConfig]).to(classOf[ICLConfig]).asEagerSingleton()
    bind(classOf[AkamaiWhitelistFilter]).to(classOf[WhitelistFilter]).asEagerSingleton()
    bind(classOf[AppConfig]).to(classOf[FrontendAppConfig]).asEagerSingleton()
  }

  private def bindControllers() {
    bind(classOf[ChooseActivityController]).to(classOf[ChooseActivityControllerImpl]).asEagerSingleton()
    bind(classOf[ConfirmationController]).to(classOf[ConfirmationControllerImpl]).asEagerSingleton()
    bind(classOf[SignInOutController]).to(classOf[SignInOutControllerImpl]).asEagerSingleton()
    bind(classOf[TestSetupController]).to(classOf[TestSetupControllerImpl]).asEagerSingleton()
    bind(classOf[RemoveSicCodeController]).to(classOf[RemoveSicCodeControllerImpl]).asEagerSingleton()
    bind(classOf[ApiController]).to(classOf[ApiControllerImpl]).asEagerSingleton()
  }

  private def bindServices() {
    bind(classOf[SicSearchService]).to(classOf[SicSearchServiceImpl]).asEagerSingleton()
    bind(classOf[JourneyService]).to(classOf[JourneyServiceImpl]).asEagerSingleton()
    bind(classOf[JourneySetupService]).to(classOf[JourneySetupServiceImpl]).asEagerSingleton()
  }

  private def bindConnectors(): Unit = {
    bind(classOf[ICLConnector]).to(classOf[ICLConnectorImpl]).asEagerSingleton()
  }
}
