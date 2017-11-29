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

package controllers.test

import javax.inject.Inject

import auth.SicSearchRegime
import config.FrontendAuthConnector
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.frontend.auth.Actions

import scala.concurrent.Future

object Journeys {
  val QUERY_BUILDER = "query-builder"
  val QUERY_PARSER = "query-parser"
}

class TestSetupControllerImpl @Inject()(val messagesApi: MessagesApi,
                                        val authConnector: FrontendAuthConnector) extends TestSetupController

trait TestSetupController extends Actions with I18nSupport {

  val form: Form[String] = Form(single("journey" -> nonEmptyText))

  val show: Action[AnyContent] = AuthorisedFor(taxRegime = new SicSearchRegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        Future.successful(Ok(views.html.test.SetupJourneyView(form)))
  }

  val submit: Action[AnyContent] = AuthorisedFor(taxRegime = new SicSearchRegime, pageVisibility = GGConfidence).async {
    implicit user =>
      implicit request =>
        form.bindFromRequest.fold(
          errors => Future.successful(BadRequest(views.html.test.SetupJourneyView(errors))),
          success => Future.successful(Ok(success))
        )
  }
}

