import sbt._

object Dependencies {

  object Versions {
    val cats           = "2.1.1"
    val catsEffect     = "2.1.4"
    val catsRetry      = "1.1.1"
    val catsScalaCheck = "0.2.0"
    val circe          = "0.13.0"
    val ciris          = "1.2.0"
    val fs2            = "2.4.4"
    val http4s         = "0.21.7"
    val kindProjector  = "0.11.0"
    val logback        = "1.2.3"
    val log4cats       = "1.1.1"
    val newtype        = "0.4.3"
    val pureConfig     = "0.12.1"
    val redis4cats     = "0.10.2"
    val refined        = "0.9.15"
    val scalaCheck     = "1.14.3"
    val scalaTest      = "3.2.2"
  }

  object Libraries {
    def circe(artifact: String): ModuleID  = "io.circe"   %% artifact % Versions.circe
    def ciris(artifact: String): ModuleID  = "is.cir"     %% artifact % Versions.ciris
    def http4s(artifact: String): ModuleID = "org.http4s" %% artifact % Versions.http4s

    lazy val cats            = "org.typelevel" %% "cats-core" % Versions.cats
    lazy val catsEffect      = "org.typelevel" %% "cats-effect" % Versions.catsEffect
    lazy val catsRetry       = "com.github.cb372" %% "cats-retry" % Versions.catsRetry
    lazy val catsScalaCheck  = "io.chrisdavenport" %% "cats-scalacheck" % Versions.catsScalaCheck
    lazy val circeCore       = circe("circe-core")
    lazy val circeGeneric    = circe("circe-generic")
    lazy val circeGenericExt = circe("circe-generic-extras")
    lazy val circeParser     = circe("circe-parser")
    lazy val cirisCore       = ciris("ciris")
    lazy val cirisEnum       = ciris("ciris-enumeratum")
    lazy val cirisRefined    = ciris("ciris-refined")
    lazy val fs2             = "co.fs2" %% "fs2-core" % Versions.fs2
    lazy val http4sDsl       = http4s("http4s-dsl")
    lazy val http4sServer    = http4s("http4s-blaze-server")
    lazy val http4sClient    = http4s("http4s-blaze-client")
    lazy val http4sCirce     = http4s("http4s-circe")
    // Compiler plugins
    lazy val kindProjector = "org.typelevel" % "kind-projector" % Versions.kindProjector
    // Runtime
    lazy val logback            = "ch.qos.logback"        % "logback-classic"      % Versions.logback
    lazy val log4cats           = "io.chrisdavenport"     %% "log4cats-slf4j"      % Versions.log4cats
    lazy val newtype            = "io.estatico"           %% "newtype"             % Versions.newtype
    lazy val pureConfig         = "com.github.pureconfig" %% "pureconfig"          % Versions.pureConfig
    lazy val redis4catsEffects  = "dev.profunktor"        %% "redis4cats-effects"  % Versions.redis4cats
    lazy val redis4catsLog4cats = "dev.profunktor"        %% "redis4cats-log4cats" % Versions.redis4cats
    lazy val refinedCore        = "eu.timepit"            %% "refined"             % Versions.refined
    lazy val refinedCats        = "eu.timepit"            %% "refined-cats"        % Versions.refined
    // Test
    lazy val scalaTest  = "org.scalatest"  %% "scalatest"  % Versions.scalaTest
    lazy val scalaCheck = "org.scalacheck" %% "scalacheck" % Versions.scalaCheck
  }

}
