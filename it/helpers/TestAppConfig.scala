
package helpers

trait TestAppConfig {
  this: Wiremock =>

  val testAppConfig = Map(
    "microservice.services.auth.host" -> wiremockHost,
    "microservice.services.auth.port" -> wiremockPort,
    "microservice.services.auth.login_path" -> "/auth-login-path"
  )

  def testAppConfig(extraConfig: (String, Any)*): Map[String, Any] = testAppConfig ++ extraConfig
}
