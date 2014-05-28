import sbt._
import Keys._

import net.virtualvoid.sbt.graph.Plugin.graphSettings
import sbtassembly.Plugin._
import sbtassembly.Plugin.AssemblyKeys._
import com.typesafe.sbt.SbtScalariform._
import scalariform.formatter.preferences._
import sbtunidoc.Plugin._

object ServiceNetFunctionalTestsServerBuild extends Build {

  //////////////////////////////////////////////////////////////////////////////
  // PROJECT INFO
  //////////////////////////////////////////////////////////////////////////////

  val ORGANIZATION = "mesosphere.service-net"
  val PROJECT_NAME = "functional-tests-server"
  val PROJECT_VERSION = "0.1.0-SNAPSHOT"
  val SCALA_VERSION = "2.10.4"


  //////////////////////////////////////////////////////////////////////////////
  // DEPENDENCY VERSIONS
  //////////////////////////////////////////////////////////////////////////////

  val LOGBACK_VERSION         = "1.1.2"
  val SLF4J_VERSION           = "1.7.6"
  val UNFILTERED_VERSION      = "0.7.1"
  val TYPESAFE_CONFIG_VERSION = "1.2.0"
  val SCALATEST_VERSION       = "2.1.5"


  //////////////////////////////////////////////////////////////////////////////
  // PROJECTS
  //////////////////////////////////////////////////////////////////////////////

  lazy val root = Project(
    id = PROJECT_NAME,
    base = file("."),
    settings = commonSettings ++ Seq(
        aggregate in update := false,
        mainClass in (Compile, packageBin) :=
          Some("mesosphere.servicenet.tests.server.Server"),
        mainClass in (Compile, run) :=
          Some("mesosphere.servicenet.tests.server.Server"),
        libraryDependencies ++= Seq(
          "net.databinder"    %% "unfiltered-filter" % UNFILTERED_VERSION,
          "net.databinder"    %% "unfiltered-jetty"  % UNFILTERED_VERSION
        )
      ) ++
      assemblySettings ++
      graphSettings
  )

  //////////////////////////////////////////////////////////////////////////////
  // SHARED SETTINGS
  //////////////////////////////////////////////////////////////////////////////

  lazy val commonSettings =
    Project.defaultSettings ++ basicSettings ++ formatSettings ++ unidocSettings

  lazy val basicSettings = Seq(
    version := PROJECT_VERSION,
    organization := ORGANIZATION,
    scalaVersion := SCALA_VERSION,

    resolvers ++= Seq(
      "Mesosphere Repo"     at "http://downloads.mesosphere.io/maven",
      "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
    ),

    libraryDependencies ++= Seq(
      "com.typesafe"   % "config"    % TYPESAFE_CONFIG_VERSION,
      "org.scalatest" %% "scalatest" % SCALATEST_VERSION % "test"
    ),

    scalacOptions in Compile ++= Seq(
      "-unchecked",
      "-deprecation",
      "-feature"
    ),

    javacOptions in Compile ++= Seq(
      "-Xlint:unchecked",
      "-source", "1.7",
      "-target", "1.7"
    ),

    fork in Test := false
  )

  lazy val formatSettings = scalariformSettings ++ Seq(
    ScalariformKeys.preferences := FormattingPreferences()
      .setPreference(IndentWithTabs, false)
      .setPreference(IndentSpaces, 2)
      .setPreference(AlignParameters, true)
      .setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(MultilineScaladocCommentsStartOnFirstLine, false)
      .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, true)
      .setPreference(PreserveDanglingCloseParenthesis, true)
      .setPreference(CompactControlReadability, true)
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(PreserveSpaceBeforeArguments, true)
      .setPreference(SpaceBeforeColon, false)
      .setPreference(SpaceInsideBrackets, false)
      .setPreference(SpaceInsideParentheses, false)
      .setPreference(SpacesWithinPatternBinders, true)
      .setPreference(FormatXml, true)
    )

}

