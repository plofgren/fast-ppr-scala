
name := "fast-ppr"

version := "1.0"

scalacOptions ++= Seq("-feature")

libraryDependencies += "com.twitter" %% "cassovary" % "3.2.0"

libraryDependencies += "org.scalatest" % "scalatest_2.10" % "2.0" % "test"

libraryDependencies += "org.clapper" % "grizzled-slf4j_2.10" % "1.0.1"

//mainClass in (Compile, run) := Some("soal.fastppr.experiments.AccuracyExperiments")



