version := "0.1.0-SNAPSHOT"
organization := "com.fiatjaf"
homepage := Some(url("https://github.com/fiatjaf/sbt-esbuild"))

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
