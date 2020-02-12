import Options._


inThisBuild(Seq(
  version := "0.11.1-SNAPSHOT",

  organization := "io.github.outwatch",

  scalaVersion := "2.12.9",

  crossScalaVersions := Seq("2.11.12", "2.12.10", "2.13.0"),

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
)

lazy val outwatch = project
  .in(file("outwatch"))
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .settings(commonSettings)
  .settings(scalacOptions ++=
    CrossVersion.partialVersion(scalaVersion.value).map(v =>
      allOptionsForVersion(s"${v._1}.${v._2}", true)
    ).getOrElse(Nil)
  )
  .settings(scalacOptions in (Compile, console) ~= (_.diff(badConsoleFlags)))
  .settings(
    name := "OutWatch",
    normalizedName := "outwatch",

    libraryDependencies ++= Seq(
      "io.monix"      %%% "monix"       % "3.1.0",
      "org.scala-js"  %%% "scalajs-dom" % "1.0.0",
      "com.raquo"     %%% "domtypes" % "0.9.6",
      "org.typelevel" %%% "cats-core" % "2.0.0",
      "org.typelevel" %%% "cats-effect" % "2.0.0",

      "org.scalatest" %%% "scalatest" % "3.1.0" % Test,
      compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.4.4" cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % "1.4.4" % Provided cross CrossVersion.full,
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
  .dependsOn(outwatch)
  .settings(
    skip in publish := true,

    resolvers ++=
      ("jitpack" at "https://jitpack.io") ::
      Nil,
    libraryDependencies ++=
      "com.github.fdietze.bench" %%% "bench" % "555e14b" ::
      Nil,

    scalaJSStage in Compile := FullOptStage,
    scalacOptions ++= Seq ("-Xdisable-assertions"),
    /* scalaJSUseMainModuleInitializer := true, */

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
  .aggregate(outwatch, bench)
