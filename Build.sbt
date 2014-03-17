import com.typesafe.sbt.SbtStartScript

seq(SbtStartScript.startScriptForClassesSettings: _*)

name := "Carers"

version := "1.0"

scalaVersion := "2.10.1"

EclipseKeys.withSource := true

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

libraryDependencies +=  "org.cddcore" %% "website" % "1.8.5.12"

libraryDependencies +=  "com.novocode" % "junit-interface" % "0.10-M2" % "test"

libraryDependencies += "org.eclipse.jetty" % "jetty-webapp" % "8.0.0.M0"

testFrameworks := Seq(TestFrameworks.JUnit, TestFrameworks.ScalaCheck, TestFrameworks.ScalaTest, TestFrameworks.Specs2)