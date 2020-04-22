
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
  .enablePlugins(Seq(play.sbt.PlayScala,SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory): _*)
  .settings(scalaSettings: _*)
  .settings(majorVersion := 0)
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
    Keys.fork                  in Test            := true,
    parallelExecution          in Test            := false,
    testGrouping               in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
    parallelExecution          in IntegrationTest := false,
    resolvers                                     += Resolver.bintrayRepo("hmrc", "releases"),
    resolvers                                     += Resolver.jcenterRepo,
    unmanagedSourceDirectories in IntegrationTest := (baseDirectory in IntegrationTest)(base => Seq(base / "it")).value,
    addTestReportOption(IntegrationTest, "int-test-reports")
  )

dependencyOverrides ++= Set(
  "com.typesafe.akka" %% "akka-actor" % "2.5.23",
  "com.typesafe.akka" %% "akka-protobuf" % "2.5.23",
  "com.typesafe.akka" %% "akka-slf4j" % "2.5.23",
  "com.typesafe.akka" %% "akka-stream" % "2.5.23"
)