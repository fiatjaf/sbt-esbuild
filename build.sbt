ThisBuild / tlBaseVersion := "0.1"

ThisBuild / organization := "com.fiatjaf"
ThisBuild / organizationName := "nbd"
ThisBuild / startYear := Some(2022)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers := List(tlGitHubDev("fiatjaf", "fiatjaf"))

ThisBuild / tlSonatypeUseLegacyHost := false

lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-esbuild",
    pluginCrossBuild / sbtVersion := {
      scalaBinaryVersion.value match {
        case "2.12" => "1.2.8"
      }
    },
    addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.10.1"),
    addSbtPlugin("io.chrisdavenport" % "sbt-npm-dependencies" % "0.0.1")
  )
