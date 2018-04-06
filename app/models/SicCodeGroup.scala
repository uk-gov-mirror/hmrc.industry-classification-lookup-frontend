
package models


import play.api.libs.json.Json

case class SicCodeGroup(sicCode: SicCode, indexes: List[String])

object SicCodeGroup {
  implicit val format = Json.format[SicCodeGroup]
}