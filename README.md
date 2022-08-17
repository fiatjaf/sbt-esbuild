# sbt-esbuild [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.fiatjaf/sbt-esbuild_2.12_1.0/badge.svg)](https://repo1.maven.org/maven2/com/fiatjaf/sbt-esbuild_2.12_1.0/)

A very simple plugin that bundles your generated ScalaJS code with its JavaScript dependencies _and the JavaScript_ dependencies of its dependencies.

Uses [sbt-npm-dependencies](https://github.com/davenverse/sbt-npm-dependencies) to gather the list of the dependencies, installs them with [npm](https://www.npmjs.com) or [yarn](https://yarnpkg.com) and them bundles everything with [esbuild](https://esbuild.github.io/).

## Installation

Add this to your `project/plugins.sbt`:

```scala
addSbtPlugin("com.fiatjaf" %% "sbt-esbuild" % "0.1.0")
```

## Usage

In your code, when [writing facades](http://www.scala-js.org/doc/interoperability/facade-types.html), use `@JSImport` to refer to the JavaScript modules you're importing (they will get translated to `require()` or `import` calls and later transpiled by `esbuild`).

In your build.sbt, enable the plugin, include your dependencies and optionally set the linker to use ES modules:

```diff
-enablePlugins(ScalaJSPlugin)
+enablePlugins(ScalaJSPlugin, EsbuildPlugin)

+Compile / npmDependencies ++= Seq(
+  "left-pad" -> "latest",
+  "garbage" -> "0.0.0"
+)
+
+scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) }
```

If you are importing another Scala library that has declared `npmDependencies` using [sbt-npm-dependencies](https://github.com/davenverse/sbt-npm-dependencies) (for example, [scoin](https://github.com/fiatjaf/scoin)) these dependencies will all be fetched automatically and the `@JSImport` annotations from that library will all work.

On `sbt`, call `fastLinkJS / esBuild` to install dependencies, build and bundle. Or `~fastLinkJS / esBuild` to build continuously. The resulting JS bundle will be written to `target/esbuild/bundle.js` (same with `fullLinkJS`).

You can also:
  - set `esPackageManager` to `Npm` or `Yarn` for what to use when installing dependencies (defaults to `Yarn`);
  - call `esInstall` to just install the JS modules; or
  - set `esBuildOptions` (defaults to `Seq("--sourcemap")`) to pass extra options to [esbuild](https://esbuild.github.io/api/#simple-options) (it is great since most things work either out-of-the-box or as command-line arguments with no need for bloated configuration files).
