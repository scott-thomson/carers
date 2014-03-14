import com.typesafe.sbt.SbtStartScript

seq(SbtStartScript.startScriptForClassesSettings: _*)

name := "Tennis"

version := "1.0"

scalaVersion := "2.10.1"

EclipseKeys.withSource := true

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

libraryDependencies +=   "org.cddcore" %% "website" % "1.8.5.12"