import Dependencies.autoImport.{logbackClassic, slf4jApi}

name := "iot-data"

ThisBuild / scalaVersion := "2.12.8"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "alexeyn"
ThisBuild / organizationName := "alexeyn"

lazy val root = (project in file("."))
  .aggregate(`simulator`, `processor`)
  .settings(publishArtifact := false)

lazy val `simulator` = project.settings(
  libraryDependencies ++= Seq(
    fs2Cron,
    fs2Kafka,
    upickle,
    ujson,
    scalaLogging,
    logbackClassic,
    slf4jApi,
    scalacheck
  )
)

lazy val `processor` = project.settings(
  libraryDependencies ++= Seq(
    fs2Kafka,
    scalaLogging,
    logbackClassic,
    slf4jApi
  )
)

scalacOptions += "-Ypartial-unification"
