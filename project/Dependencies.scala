import sbt._

object Dependencies extends AutoPlugin {

  override def trigger: PluginTrigger = allRequirements

  object autoImport {

    object DependenciesVersion {
      val logbackClassicVersion = "1.2.3"
      val scalaTestVersion = "3.0.5"
      val slf4jVersion = "1.7.25"
      val scalacheckVersion = "1.13.4"
      val scalaLoggingVersion = "3.9.0"
      val upickleVersion = "0.6.7"
    }

    import DependenciesVersion._

    val logbackClassic = "ch.qos.logback" % "logback-classic" % logbackClassicVersion
    val slf4jApi = "org.slf4j" % "slf4j-api" % slf4jVersion
    val scalacheck = "org.scalacheck" %% "scalacheck" % scalacheckVersion
    val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion
    val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion
    val upickle = "com.lihaoyi" %% "upickle" % upickleVersion
    val ujson = "com.lihaoyi" %% "ujson" % upickleVersion
    val fs2Cron = "eu.timepit" %% "fs2-cron-core" % "0.1.0"
    val fs2Kafka = "com.ovoenergy" %% "fs2-kafka" % "0.19.4"
  }

}