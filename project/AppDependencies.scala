import sbt._

private object AppDependencies {
  val compile = Seq(
    "uk.gov.hmrc" %% "bootstrap-play-26" % "1.7.0",
    "uk.gov.hmrc" %% "govuk-template" % "5.54.0-play-26",
    "uk.gov.hmrc" %% "play-ui" % "8.8.0-play-26",
    "uk.gov.hmrc" %% "auth-client" % "3.0.0-play-26",
    "uk.gov.hmrc" %% "play-allowlist-filter" % "0.1.0-play-26",
    "uk.gov.hmrc" %% "simple-reactivemongo" % "7.30.0-play-26",
    "uk.gov.hmrc" %% "play-conditional-form-mapping" % "1.2.0-play-26"
  )

  def test(scope: String = "test,it") = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.3" % scope,
    "com.github.tomakehurst" % "wiremock-jre8" % "2.26.3" % scope,
    "org.jsoup" % "jsoup" % "1.13.1" % scope,
    "org.mockito" % "mockito-core" % "3.3.3" % scope,
    "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0" % scope,
    "uk.gov.hmrc" %% "reactivemongo-test" % "4.21.0-play-26" % scope
  )

  def apply() = compile ++ test()
}
