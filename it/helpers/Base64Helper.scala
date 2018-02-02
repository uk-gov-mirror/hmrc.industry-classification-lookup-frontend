
package helpers

import java.util.Base64

object Base64Helper {
  implicit class StringToBase64(s: String){
    def toBase64: String = Base64.getEncoder.encodeToString(s.getBytes)
  }
}
