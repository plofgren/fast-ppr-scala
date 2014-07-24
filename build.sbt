name := "fast-ppr"

version := "1.0"

libraryDependencies += "com.twitter" %% "cassovary" % "3.2.0"

libraryDependencies += "org.scalatest" % "scalatest_2.10" % "2.0" % "test"

// from https://github.com/earldouglas/xsbt-web-plugin/tree/0.9

jetty()

libraryDependencies += "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided"

libraryDependencies ++= Seq( // test
    "org.eclipse.jetty" % "jetty-webapp" % "9.1.0.v20131115" % "test"
  , "org.eclipse.jetty" % "jetty-plus" % "9.1.0.v20131115" % "test"
  , "javax.servlet" % "javax.servlet-api" % "3.1.0" % "test"
  , "org.scalatest" %% "scalatest" % "1.9.1" % "test"
)

ScoverageSbtPlugin.instrumentSettings

CoverallsPlugin.coverallsSettings

val linkWar = taskKey[Unit]("Symlink the packaged .war file")

linkWar := {
  val (art, pkg) = packagedArtifact.in(Compile, packageWar).value
  import java.nio.file.Files
  val link = (target.value / (art.name + "." + art.extension))
  link.delete
  Files.createSymbolicLink(link.toPath, pkg.toPath)
}

javaOptions += "java.util.logging.config.file=logging_config.txt"