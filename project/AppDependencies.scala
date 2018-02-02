import sbt._
import play.core.PlayVersion

private object AppDependencies {
  val compile = Seq(
    "uk.gov.hmrc" %% "frontend-bootstrap"     % "8.17.0",
    "uk.gov.hmrc" %% "auth-client"            % "2.5.0",
    "uk.gov.hmrc" %% "play-whitelist-filter"  % "2.0.0",
    "uk.gov.hmrc" %% "play-reactivemongo"     % "6.2.0"
  )

  def test(scope: String = "test,it") = Seq(
    "uk.gov.hmrc"             %% "hmrctest"                     % "3.0.0"             % scope,
    "org.scalatestplus.play"  %% "scalatestplus-play"           % "2.0.0"             % scope,
    "com.github.tomakehurst"  %  "wiremock"                     % "2.11.0"            % scope,
    "org.jsoup"               %  "jsoup"                        % "1.11.1"            % scope,
    "org.mockito"             %  "mockito-core"                 % "2.13.0"            % scope,
    "org.scalamock"           %% "scalamock-scalatest-support"  % "3.6.0"             % scope,
    "uk.gov.hmrc"             %% "reactivemongo-test"           % "3.1.0"             % scope
  )

  def apply() = compile ++ test()
}
