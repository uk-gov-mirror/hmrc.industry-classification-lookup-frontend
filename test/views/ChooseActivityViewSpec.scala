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

package views

import forms.chooseactivity.ChooseActivityForm
import models.{SearchResults, Sector, SicCode}
import org.jsoup.Jsoup
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import views.html.pages.{chooseactivity => ChooseActivityPage}

class ChooseActivityViewSpec extends UnitSpec with I18nSupport with MockitoSugar with WithFakeApplication {
  implicit val request: FakeRequest[_] = FakeRequest()
  implicit val messagesApi : MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]

  val query = "test query"

  val testSicCode = SicCode("12345678", "Testing")

  val searchResults = SearchResults(query, 1, List(testSicCode), List(Sector("A", "Fake Sector", 1)))

  "The choose activity screen" should {
    lazy val view = ChooseActivityPage(ChooseActivityForm.form, searchResults)
    lazy val document = Jsoup.parse(view.body)

    "have the correct title" in {
      document.getElementById("page-title").text shouldBe messagesApi("page.icl.chooseactivity.heading")
    }

    "have a back link" in {
      document.getElementById("back").text shouldBe messagesApi("app.common.back")
    }

    "have the correct sic code displayed as your choice" in {
      document.getElementById("search-term").text shouldBe s"'$query'"
    }

  }

}

