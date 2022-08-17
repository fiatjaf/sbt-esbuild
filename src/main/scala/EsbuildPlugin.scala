package sbtesbuild

import java.io.File
import java.nio.file.{Files, Paths}
import scala.annotation.nowarn
import scala.sys.process.{Process, ProcessLogger}
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import org.scalajs.sbtplugin.{ScalaJSPlugin, Stage}
import io.chrisdavenport.sbt.npmdependencies.sbtplugin.NpmDependenciesPlugin
import io.chrisdavenport.sbt.npmdependencies.sbtplugin.NpmDependenciesPlugin.autoImport._
import sbt._
import Keys._

object EsbuildPlugin extends AutoPlugin {
  override val requires = ScalaJSPlugin && NpmDependenciesPlugin
  override def trigger = allRequirements

  object autoImport {
    sealed trait PackageManager
    case object Yarn extends PackageManager { override def toString = "yarn" }
    case object Npm extends PackageManager { override def toString = "npm" }

    val jsPackageManager = settingKey[PackageManager](
      "Which package manager to use when installing dependencies."
    )
    val esbuildOptions = settingKey[Seq[String]](
      "Extra options to pass to esbuild CLI invocation."
    )
    val jsInstall =
      taskKey[Unit]("Install packages from npmDependencies.")
    val jsBundle = taskKey[Unit](
      "Transpile all the generated JavaScript and dependencies into a single file."
    )
  }

  import autoImport._
  override lazy val globalSettings: Seq[Setting[_]] = Seq(
    jsPackageManager := Yarn,
    esbuildOptions := Seq("--sourcemap")
  )

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    jsInstall := {
      val deps = (Compile / npmDependencies).value
      val install = jsPackageManager.value match {
        case Yarn => "yarn"
        case Npm  => "npm install"
      }

      Process("mkdir -p target/esbuild", baseDirectory.value).run().exitValue()

      val packageJsonContents =
        "{\"dependencies\":{" ++ (("esbuild" -> "latest") +: deps)
          .map({ case (name, version) =>
            "\"" ++ name ++ "\":\"" ++ version ++ "\""
          })
          .mkString(",") ++ "}}"
      Files.write(
        Paths.get(s"${baseDirectory.value}/target/esbuild/package.json"),
        packageJsonContents.getBytes()
      )

      println(s"> $install")
      Process(install, new File(baseDirectory.value, "/target/esbuild"))
        .run(
          ProcessLogger(msg => println(s"> $msg"), msg => println(s"> $msg"))
        )
        .exitValue()
    },
    perStageBundle(Stage.FastOpt),
    perStageBundle(Stage.FullOpt)
  )

  @nowarn
  def perStageBundle(stage: Stage): Setting[_] = {
    val (stageTask, stageSuffix, options) = stage match {
      case Stage.FastOpt => (fastLinkJS, "fastopt", "")
      case Stage.FullOpt => (fullLinkJS, "fullopt", "--minify")
    }

    stageTask / jsBundle := {
      jsInstall.value
      val filename =
        (Compile / stageTask).value.data.publicModules.head.jsFileName
      val entrypoint =
        s"${baseDirectory.value}/target/scala-${scalaVersion.value}/${name.value}-$stageSuffix/$filename"
      val command =
        s"${baseDirectory.value}/target/esbuild/node_modules/.bin/esbuild --bundle ${esbuildOptions.value
            .mkString(" ")} $options --outfile=${baseDirectory.value}/target/esbuild/bundle.js $entrypoint"
      println("> ")
      println(s"> $command")
      Process(command, new File(baseDirectory.value, "/target/esbuild"))
        .run(
          ProcessLogger(msg => println(s"> $msg"), msg => println(s"> $msg"))
        )
        .exitValue()
    }
  }
}
