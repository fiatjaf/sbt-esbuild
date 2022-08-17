enablePlugins(EsbuildPlugin)

version := "0.0.1"
scalaVersion := "3.1.3"
name := "minimal-example"
scalaJSUseMainModuleInitializer := true

Compile / npmDependencies := List("node-fetch" -> "latest")
