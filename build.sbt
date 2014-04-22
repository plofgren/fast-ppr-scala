import com.typesafe.sbt.SbtStartScript

name := "fast-ppr"

version := "1.0"

libraryDependencies += "com.twitter" %% "cassovary" % "3.2.0"

libraryDependencies += "org.scalatest" % "scalatest_2.10" % "2.0" % "test"


//seq(SbtStartScript.startScriptForClassesSettings: _*)

settings = SbtStartScript.startScriptForClassesSettings