import Dependencies.autoImport.{logbackClassic, slf4jApi}

name := "iot-data"

ThisBuild / scalaVersion := "2.12.8"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "alexeyn"
ThisBuild / organizationName := "alexeyn"

lazy val commonLibraries = Seq(
  "co.fs2" %% "fs2-core" % "1.0.4",
  "co.fs2" %% "fs2-io" % "1.0.4",
)

lazy val root = (project in file("."))
  .aggregate(simulator, processor)
  .settings(publishArtifact := false)

lazy val `simulator` = project.settings(
  libraryDependencies ++=  Seq(
    "eu.timepit" %% "fs2-cron-core" % "0.1.0",
    "com.ovoenergy" %% "fs2-kafka" % "0.19.4",
    scalaLogging,
    logbackClassic,
    slf4jApi,
    scalacheck,
    scalaTest % Test
  )
)

lazy val `processor` = project.settings(libraryDependencies ++= commonLibraries)

scalacOptions += "-Ypartial-unification"
