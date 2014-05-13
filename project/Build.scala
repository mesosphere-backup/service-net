import sbt._
import Keys._

import net.virtualvoid.sbt.graph.Plugin.graphSettings
import sbtassembly.Plugin._
import sbtassembly.Plugin.AssemblyKeys._
import com.typesafe.sbt.SbtScalariform._
import scalariform.formatter.preferences._


object ServiceNetBuild extends Build {

  //////////////////////////////////////////////////////////////////////////////
  // PROJECT INFO
  //////////////////////////////////////////////////////////////////////////////

  val ORGANIZATION = "mesosphere"
  val PROJECT_NAME = "service-net"
  val PROJECT_VERSION = "0.1.0-SNAPSHOT"
  val SCALA_VERSION = "2.10.4"


  //////////////////////////////////////////////////////////////////////////////
  // DEPENDENCY VERSIONS
  //////////////////////////////////////////////////////////////////////////////

  val DISPATCH_VERSION        = "0.11.1"
  val UNFILTERED_VERSION      = "0.7.1"
  val TYPESAFE_CONFIG_VERSION = "1.2.0"
  val SCALATEST_VERSION       = "2.1.5"


  //////////////////////////////////////////////////////////////////////////////
  // PROJECTS
  //////////////////////////////////////////////////////////////////////////////

  lazy val root = Project(
    id = PROJECT_NAME,
    base = file("."),
    settings = commonSettings ++
      Seq(
        aggregate in update := false,
        mainClass in (Compile, packageBin) :=
          Some("mesosphere.servicenet.http.HTTPServer"),
        mainClass in (Compile, run) :=
          Some("mesosphere.servicenet.http.HTTPServer")
      ) ++
      assemblySettings ++
      graphSettings
  )
    .dependsOn(dsl, http, patch)
    .aggregate(dsl, http, patch)

  def subproject(suffix: String) = s"${PROJECT_NAME}-$suffix"

  lazy val dsl = Project(
    id = subproject("dsl"),
    base = file("dsl"),
    settings = commonSettings
  )

  lazy val http = Project(
    id = subproject("http"),
    base = file("http"),
    settings = commonSettings
  ) dependsOn (dsl)

  lazy val patch = Project(
    id = subproject("patch"),
    base = file("patch"),
    settings = commonSettings
  ) dependsOn (dsl)

  //////////////////////////////////////////////////////////////////////////////
  // SHARED SETTINGS
  //////////////////////////////////////////////////////////////////////////////

  lazy val commonSettings =
    Project.defaultSettings ++ basicSettings ++ formatSettings

  lazy val basicSettings = Seq(
    version := PROJECT_VERSION,
    organization := ORGANIZATION,
    scalaVersion := SCALA_VERSION,

    libraryDependencies ++= Seq(
      "com.typesafe"    % "config"            % TYPESAFE_CONFIG_VERSION,
      "net.databinder" %% "unfiltered-filter" % UNFILTERED_VERSION,
      "net.databinder" %% "unfiltered-jetty"  % UNFILTERED_VERSION,
      "org.scalatest"  %% "scalatest"         % SCALATEST_VERSION % "test"
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