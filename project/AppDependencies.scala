import sbt._
import play.core.PlayVersion

private object AppDependencies {
  val compile = Seq(
    "uk.gov.hmrc" %% "frontend-bootstrap"    % "12.9.0",
    "uk.gov.hmrc" %% "bootstrap-play-25"     % "5.1.0",
    "uk.gov.hmrc" %% "govuk-template"        % "5.22.0",
    "uk.gov.hmrc" %% "play-ui"               % "8.7.0-play-25",
    "uk.gov.hmrc" %% "auth-client"           % "2.17.0-play-25",
    "uk.gov.hmrc" %% "play-whitelist-filter" % "3.1.0-play-25",
    "uk.gov.hmrc" %% "simple-reactivemongo"  % "7.22.0-play-25",
    "uk.gov.hmrc" %% "play-conditional-form-mapping" % "1.2.0-play-25"
  )

  def test(scope: String = "test,it") = Seq(
    "uk.gov.hmrc"             %% "hmrctest"                     % "3.9.0-play-25"     % scope,
    "org.scalatestplus.play"  %% "scalatestplus-play"           % "2.0.0"             % scope,
    "com.github.tomakehurst"  %  "wiremock"                     % "2.11.0"            % scope,
    "org.jsoup"               %  "jsoup"                        % "1.11.1"            % scope,
    "org.mockito"             %  "mockito-core"                 % "3.2.4"             % scope,
    "org.scalamock"           %% "scalamock-scalatest-support"  % "3.6.0"             % scope,
    "uk.gov.hmrc"             %% "reactivemongo-test"           % "4.16.0-play-25"    % scope
  )

  def apply() = compile ++ test()
}
