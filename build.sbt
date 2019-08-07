inThisBuild(Seq(
  version := "0.11.1-SNAPSHOT",

  organization := "io.github.outwatch",

  scalaVersion := "2.12.9",

  crossScalaVersions := Seq("2.11.12", "2.12.9", "2.13.0"),

  licenses += ("Apache 2", url("https://www.apache.org/licenses/LICENSE-2.0.txt")),

  homepage := Some(url("https://outwatch.github.io/")),

  scmInfo := Some(ScmInfo(
    url("https://github.com/OutWatch/outwatch"),
    "scm:git:git@github.com:OutWatch/outwatch.git",
    Some("scm:git:git@github.com:OutWatch/outwatch.git"))
  )
))

lazy val commonSettings = Seq(
  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3"),

  scalacOptions += {
    val local = baseDirectory.value.toURI
    val remote = s"https://raw.githubusercontent.com/OutWatch/outwatch/${git.gitHeadCommit.value.get}/"
    s"-P:scalajs:mapSourceURI:$local->$remote"
  },

  scalacOptions ++=
    "-encoding" :: "UTF-8" ::
    "-unchecked" ::
    "-deprecation" ::
    "-explaintypes" ::
    "-feature" ::
    "-language:_" ::
    "-Xfuture" ::
    "-Xlint" ::
    "-Ypartial-unification" ::
    "-Yno-adapted-args" ::
    "-Ywarn-infer-any" ::
    "-Ywarn-value-discard" ::
    "-Ywarn-nullary-override" ::
    "-Ywarn-nullary-unit" ::
    "-P:scalajs:sjsDefinedByDefault" ::
    Nil,

  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 12)) =>
        "-Ywarn-extra-implicit" ::
        "-Ywarn-unused:-explicits,-implicits,_" ::
        Nil
      case _             =>
        "-Ywarn-unused" ::
        "-Xexperimental" ::   // SAM conversion
        Nil
    }
  }
)

lazy val outwatch = project
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .settings(commonSettings)
  .settings(
    name := "OutWatch",
    normalizedName := "outwatch",

    libraryDependencies ++= Seq(
      "io.monix"      %%% "monix"       % "3.0.0-RC3",
      "org.scala-js"  %%% "scalajs-dom" % "0.9.7",
      "com.raquo"     %%% "domtypes" % "0.9.4",
      "org.typelevel" %%% "cats-core" % "1.6.0",
      "org.typelevel" %%% "cats-effect" % "1.3.0",

      "org.scalatest" %%% "scalatest" % "3.0.8" % Test
    ),

    npmDependencies in Compile ++= Seq(
      "snabbdom" -> "git://github.com/cornerman/snabbdom.git#semver:0.7.4"
    ),

    requireJsDomEnv in Test := true,
    useYarn := true,

    publishMavenStyle := true,

    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
          Some("snapshots" at nexus + "content/repositories/snapshots")
      else
          Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },

    pomExtra :=
      <developers>
          <developer>
          <id>ltj</id>
          <name>Luka Jacobowitz</name>
          <url>https://github.com/LukaJCB</url>
          </developer>
      </developers>,

    pomIncludeRepository := { _ => false }
  )

lazy val bench = project
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .settings(commonSettings)
  .dependsOn(outwatch)
  .settings(
    publish := {},
    publishLocal := {},

    scalaJSStage in Compile := FullOptStage,
    scalaJSUseMainModuleInitializer := true,

    useYarn := true,

    npmDependencies in Compile ++= Seq(
      "jsdom" -> "9.9.0"
    ),
  )


lazy val root = project
  .in(file("."))
  .settings(
    name := "outwatch-root",
    skip in publish := true,
  )
  .dependsOn(outwatch, bench)
  .aggregate(outwatch)
