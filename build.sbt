
import scoverage.ScoverageKeys
import TestPhases.oneForkedJvmPerTest
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName: String = "industry-classification-lookup-frontend"

lazy val scoverageSettings: Seq[Setting[_]] = Seq(
  ScoverageKeys.coverageExcludedPackages  := "<empty>;Reverse.*;models.*;models/.data/..*;view.*;config.*;.*(AuthService|BuildInfo|Routes).*",
  ScoverageKeys.coverageMinimum           := 80,
  ScoverageKeys.coverageFailOnMinimum     := false,
  ScoverageKeys.coverageHighlighting      := true
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(Seq(play.sbt.PlayScala,SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin): _*)
  .settings(scalaSettings: _*)
  .settings(PlayKeys.playDefaultPort := 9874)
  .settings(publishingSettings: _*)
  .settings(scoverageSettings: _*)
  .settings(defaultSettings(): _*)
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    scalaVersion                                  := "2.11.11",
    libraryDependencies                           ++= AppDependencies(),
    retrieveManaged                               := true,
    evictionWarningOptions     in update          := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    cancelable                 in Global          := true,
    Keys.fork                  in IntegrationTest := false,
    testGrouping               in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
    parallelExecution          in IntegrationTest := false,
    resolvers                                     += Resolver.bintrayRepo("hmrc", "releases"),
    resolvers                                     += Resolver.jcenterRepo,
    unmanagedSourceDirectories in IntegrationTest := (baseDirectory in IntegrationTest)(base => Seq(base / "it")).value,
    addTestReportOption(IntegrationTest, "int-test-reports")
  )
