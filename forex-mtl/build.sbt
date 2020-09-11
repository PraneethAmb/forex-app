import Dependencies._

name := "forex"
version := "1.0.1"

scalaVersion := "2.13.2"

lazy val core = (project in file("."))
  .enablePlugins(JavaServerAppPackaging)
  .enablePlugins(DockerPlugin)
  .enablePlugins(AshScriptPlugin)
  .settings(
    name := "forex-app",
    packageName in Docker := "forex-app",
    resolvers +=
      "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-feature",
      "-language:existentials",
      "-language:experimental.macros",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-unchecked",
      "-Xfatal-warnings",
      "-Xlint",
      "-Ydelambdafy:method",
      "-Xlog-reflective-calls",
      "-Ywarn-dead-code",
      "-Ywarn-value-discard",
      "-Ymacro-annotations"
    ),
    scalafmtOnCompile := true,
    Defaults.itSettings,
    dockerBaseImage := "adoptopenjdk/openjdk14:jre-14.0.2_12-ubuntu",
    dockerExposedPorts ++= Seq(8080),
    makeBatScripts := Seq(),
    dockerUpdateLatest := true,
    libraryDependencies ++= Seq(
      compilerPlugin(Libraries.kindProjector cross CrossVersion.full),
      Libraries.cats,
      Libraries.catsEffect,
      Libraries.catsRetry,
      Libraries.circeCore,
      Libraries.circeGeneric,
      Libraries.circeGenericExt,
      Libraries.circeParser,
      Libraries.cirisCore,
      Libraries.cirisEnum,
      Libraries.cirisRefined,
      Libraries.fs2,
      Libraries.http4sDsl,
      Libraries.http4sServer,
      Libraries.http4sClient,
      Libraries.http4sCirce,
      Libraries.logback,
      Libraries.log4cats,
      Libraries.newtype,
      Libraries.pureConfig,
      Libraries.redis4catsEffects,
      Libraries.redis4catsLog4cats,
      Libraries.refinedCore,
      Libraries.refinedCats,
      Libraries.scalaTest      % Test,
      Libraries.scalaCheck     % Test,
      Libraries.catsScalaCheck % Test
    )
  )
