val pluginVersion = sys.props.getOrElse(
  "plugin.version",
  sys.error("'plugin.version' environment variable is not set")
)

addSbtPlugin("com.fiatjaf" % "sbt-esbuild" % pluginVersion changing())
