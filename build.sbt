Global / onChangedBuildSource := ReloadOnSourceChanges

inThisBuild(
  Seq(
    organization       := "io.github.outwatch",
    scalaVersion       := crossScalaVersions.value.last,
    crossScalaVersions := Seq("2.13.10", "3.2.0"),
    licenses           += ("Apache 2", url("https://www.apache.org/licenses/LICENSE-2.0.txt")),
    homepage           := Some(url("https://outwatch.github.io/")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/outwatch/outwatch"),
        "scm:git:git@github.com:outwatch/outwatch.git",
        Some("scm:git:git@github.com:outwatch/outwatch.git"),
      ),
    ),
    pomExtra :=
      <developers>
        <developer>
        <id>jk</id>
        <name>Johannes Karoff</name>
        <url>https://github.com/cornerman</url>
        </developer>
        <developer>
        <id>fx</id>
        <name>Felix Dietze</name>
        <url>https://github.com/fdietze</url>
        </developer>
        <developer>
        <id>ltj</id>
        <name>Luka Jacobowitz</name>
        <url>https://github.com/LukaJCB</url>
        </developer>
    </developers>,
  ),
)

val jsdomVersion   = "13.2.0"
val colibriVersion = "0.7.0"

val isDotty = Def.setting(CrossVersion.partialVersion(scalaVersion.value).exists(_._1 == 3))
lazy val commonSettings = Seq(
  useYarn := true,
  libraryDependencies ++= Seq(
    "org.scalatest" %%% "scalatest" % "3.2.14" % Test,
  ),
  Test / scalacOptions --= Seq("-Xfatal-warnings"), // allow usage of deprecated calls in tests

  libraryDependencies ++= (if (isDotty.value) Nil
                           else
                             Seq(
                               compilerPlugin(("org.typelevel" %% "kind-projector" % "0.13.2").cross(CrossVersion.full)),
                             )),
)

lazy val outwatchUtil = project
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .dependsOn(outwatch)
  .in(file("util"))
  .settings(commonSettings)
  .settings(
    name := "outwatch-util",
  )

lazy val outwatchRepairDom = project
  .in(file("repairdom"))
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .dependsOn(outwatch)
  .settings(commonSettings)
  .settings(
    name := "outwatch-repairdom",
  )

lazy val outwatchSnabbdom = project
  .in(file("snabbdom"))
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .settings(commonSettings)
  .settings(
    name := "outwatch-snabbdom",
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "2.3.0",
    ),
    Compile / npmDependencies ++= Seq(
      "snabbdom" -> "github:outwatch/snabbdom.git#semver:0.7.5",
    ),
  )

lazy val outwatch = project
  .in(file("outwatch"))
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .dependsOn(outwatchSnabbdom)
  .settings(commonSettings)
  .settings(
    name := "outwatch",
    libraryDependencies ++= Seq(
      "com.raquo"            %%% "domtypes"      % "0.15.1",
      "com.github.cornerman" %%% "colibri"       % colibriVersion,
      "com.github.cornerman" %%% "colibri-jsdom" % colibriVersion,
    ),
  )

lazy val tests = project
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .dependsOn(outwatchUtil, outwatchRepairDom)
  .settings(commonSettings)
  .settings(
    publish / skip         := true,
    Test / requireJsDomEnv := true,
    installJsdom / version := jsdomVersion,
    libraryDependencies ++= Seq(
      "com.github.cornerman" %%% "colibri-reactive" % colibriVersion % Test,
    ),
  )

lazy val bench = project
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .dependsOn(outwatch)
  .settings(
    publish / skip                  := true,
    scalaJSUseMainModuleInitializer := true,
    resolvers                       += "jitpack" at "https://jitpack.io",
    libraryDependencies ++= Seq(
      "com.github.fdietze.bench" %%% "bench" % "d411db1",
    ),
    Compile / scalaJSStage := FullOptStage,
    useYarn                := true,
    Compile / npmDependencies ++= Seq(
      "jsdom" -> jsdomVersion,
    ),
  )

lazy val jsdocs = project
  .disablePlugins(TpolecatPlugin)
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
  .dependsOn(outwatch)
  .settings(
    webpackBundlingMode             := BundlingMode.LibraryOnly(),
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "org.scala-js"         %%% "scalajs-dom"          % "2.3.0",
      "com.github.cornerman" %%% "colibri-airstream"    % colibriVersion,
      "com.github.cornerman" %%% "colibri-zio"          % colibriVersion,
      "com.github.cornerman" %%% "colibri-fs2"          % colibriVersion,
      "io.github.cquiroz"    %%% "scala-java-time"      % "2.4.0-M1",
      "io.github.cquiroz"    %%% "scala-java-time-tzdb" % "2.4.0-M1",
    ),
    libraryDependencies ++= (if (isDotty.value) Nil
                             else Seq("com.github.cornerman" %%% "colibri-rx" % colibriVersion)),
    Compile / npmDependencies ++= Seq(
      "js-beautify" -> "1.14.0",
    ),
  )

lazy val docs = project
  .in(file("outwatch-docs")) // important: it must not be docs/
  .disablePlugins(TpolecatPlugin)
  .enablePlugins(MdocPlugin, DocusaurusPlugin)
  .settings(
    test / skip     := true,
    publish / skip  := true,
    moduleName      := "outwatch-docs",
    mdocJS          := Some(jsdocs),
    mdocJSLibraries := (jsdocs / Compile / fullOptJS / webpack).value,
    mdocVariables := Map(
      /* TODO: "SCALAJSVERSION" -> scalaJSVersions.current, */
      "VERSION"       -> version.value,
      "REPOURL"       -> "https://github.com/outwatch/outwatch/blob/master",
      "js-mount-node" -> "docPreview",
    ),
  )

lazy val root = project
  .in(file("."))
  .settings(
    name           := "outwatch-root",
    publish / skip := true,
  )
  .aggregate(outwatch, outwatchSnabbdom, outwatchUtil, outwatchRepairDom, tests, bench)
