/*
 * Copyright 2020 HM Revenue & Customs
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

package views.pages

import forms.chooseactivity.ChooseMultipleActivitiesForm
import forms.sicsearch.SicSearchForm
import helpers.UnitTestSpec
import helpers.mocks.{MockAppConfig, MockMessages}
import models.{SearchResults, Sector, SicCode, SicSearch}
import org.jsoup.Jsoup
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.FakeRequest
import views.html.pages.{chooseactivity => ChooseActivityPage}

class ChooseActivityViewSpec extends UnitTestSpec with MockAppConfig with MockMessages with I18nSupport {
  implicit val request: FakeRequest[_] = FakeRequest()
  override def messagesApi: MessagesApi = MockMessages

  val query = "test query"

  val testSicCode = SicCode("12345", "Testing")

  val searchResults = SearchResults(query, 1, List(testSicCode), List(Sector("A", "Fake Sector", 1)))

  val journeyId = "testJourneyId"

  "The choose activity screen" should {
    lazy val view = ChooseActivityPage(journeyId, SicSearchForm.form.fill(SicSearch(query)), ChooseMultipleActivitiesForm.form, Some(searchResults))
    lazy val document = Jsoup.parse(view.body)

    "have the correct title" in {
      document.getElementById("page-title").text mustBe messagesApi("page.icl.chooseactivity.heading")
    }

    "have a back link" in {
      document.getElementById("back").text mustBe messagesApi("app.common.back")
    }

    "have the correct search terms displayed in the search bar" in {
      document.getElementById("sicSearch").attr("value") mustBe query
    }
  }
}
