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
    }

    import DependenciesVersion._

    val logbackClassic = "ch.qos.logback" % "logback-classic" % logbackClassicVersion
    val slf4jApi = "org.slf4j" % "slf4j-api" % slf4jVersion
    val scalacheck = "org.scalacheck" %% "scalacheck" % scalacheckVersion
    val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion
    val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion
  }

}