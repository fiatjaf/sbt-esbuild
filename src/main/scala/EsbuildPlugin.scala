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
    case object Npm extends PackageManager { override def toString = "npm" }
    case object Yarn extends PackageManager { override def toString = "yarn" }
    case object Bun extends PackageManager { override def toString = "bun" }

    val esPackageManager = settingKey[PackageManager](
      "Which package manager to use when installing dependencies."
    )
    val esbuildOptions = settingKey[Seq[String]](
      "Extra options to pass to esbuild CLI invocation."
    )
    val esInstall =
      taskKey[Unit](
        "Install packages from npmDependencies and npmTransitiveDependencies."
      )
    val esBuild = taskKey[Unit](
      "Transpile all the generated JavaScript and dependencies into a single file."
    )
  }

  import autoImport._
  override lazy val globalSettings: Seq[Setting[_]] = Seq(
    esPackageManager := Npm,
    esbuildOptions := Seq("--sourcemap")
  )

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    esInstall := {
      val install = esPackageManager.value match {
        case Yarn => "yarn"
        case Npm  => "npm install"
        case Bun  => "bun install"
      }

      exec(
        baseDirectory.value.absolutePath,
        s"mkdir -p ${target.value}/esbuild"
      )

      val deps =
        ("esbuild" -> "latest") +: (
          (Compile / npmTransitiveDependencies).value.map(_._2).flatten ++
            (Compile / npmTransitiveDependencies).value.map(_._2).flatten
        ).toSet.toList

      val packageJsonContents =
        "{\"dependencies\":{" ++ (deps)
          .map({ case (name, version) =>
            "\"" ++ name ++ "\":\"" ++ version ++ "\""
          })
          .mkString(",") ++ "}}"

      Files.write(
        Paths.get(s"${target.value}/esbuild/package.json"),
        packageJsonContents.getBytes()
      )

      exec(s"${target.value}/esbuild", install)
    },
    perStageBundle(Stage.FastOpt),
    perStageBundle(Stage.FullOpt)
  )

  @nowarn
  def perStageBundle(stage: Stage): Setting[_] = {
    val (stageTask, options) = stage match {
      case Stage.FastOpt => (fastLinkJS, "")
      case Stage.FullOpt => (fullLinkJS, "--minify")
    }

    stageTask / esBuild := {
      esInstall.value

      val extra = esbuildOptions.value.mkString(" ")
      val base = s"${target.value}/esbuild"
      val buildDir = (Compile / stageTask / scalaJSLinkerOutputDirectory).value
      val filename =
        (Compile / stageTask).value.data.publicModules.head.jsFileName

      exec(
        base,
        s"ln -f ${buildDir}/$filename ./"
      )

      (Compile / stageTask).value.data.publicModules.head.sourceMapName
        .foreach { f =>
          exec(
            base,
            s"ln -f ${buildDir}/$f ./"
          )
        }

      exec(
        base,
        s"node_modules/.bin/esbuild --bundle --color $options $extra --outfile=$base/bundle.js $filename"
      )
    }
  }

  def exec(base: String, command: String): Unit = {
    println("~> ")
    println(s"~> $command")
    val code = Process(command, new File(base))
      .run(
        ProcessLogger(
          msg => System.out.println(s" > $msg"),
          msg => System.err.println(s"2> $msg")
        )
      )
      .exitValue()

    if (code != 0) throw new RuntimeException(s"code $code")
  }
}
