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

package models

import helpers.UnitTestSpec

class JourneySpec extends UnitTestSpec {

  val journeyNames = Seq(Journey.QUERY_BUILDER, Journey.QUERY_PARSER)
  val dataSets     = Seq(Journey.HMRC_SIC_8, Journey.GDS, Journey.ONS)

  "Journey" should {
    "be valid" when {
      journeyNames.foreach { journey =>
        dataSets.foreach { set =>
          s"provided with a valid journey name ($journey) and data set ($set)" in {
            noException should be thrownBy Journey("testSessionId", journey, set)
          }
        }
      }
    }

    "throw an IllegalArgumentException" when {
      "provided with an invalid journey name" in {
        intercept[IllegalArgumentException](Journey("testSessionId", "INVALID_JOURNEY", Journey.HMRC_SIC_8))
      }

      "provided with an invalid data set" in {
        intercept[IllegalArgumentException](Journey("testSessionId", Journey.QUERY_PARSER, "INVALID_DATA_SET"))
      }
    }
  }
}
