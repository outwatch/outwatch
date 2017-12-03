enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)

name := "OutWatch"

normalizedName := "outwatch"

version := "0.11.1-SNAPSHOT"

organization := "io.github.outwatch"

scalaVersion := "2.12.4"

crossScalaVersions := Seq("2.11.12", "2.12.4")


libraryDependencies ++= Seq(
  "com.github.lukajcb" %%% "rxscala-js" % "0.15.2",
  "com.raquo" %%% "domtypes" % "0.4.2",
  "org.typelevel" %%% "cats-core" % "1.0.0-RC1",
  "org.typelevel" %%% "cats-effect" % "0.5",
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "org.scalatest" %%% "scalatest" % "3.0.4" % Test,
  "org.scalacheck" %%% "scalacheck" % "1.13.5" % Test
)

npmDependencies in Compile ++= Seq(
  "rxjs" -> "5.4.3",
  "snabbdom" -> "0.7.0"
)

scalacOptions += {
  val local = baseDirectory.value.toURI
  val remote = s"https://raw.githubusercontent.com/OutWatch/outwatch/${git.gitHeadCommit.value.get}/"
  s"-P:scalajs:mapSourceURI:$local->$remote"
}

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
  Nil

scalacOptions ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 12)) =>
      "-Ywarn-extra-implicit" ::
      "-Ywarn-unused:-explicits,-implicits,_" ::
      Nil
    case _             =>
      "-Ywarn-unused" ::
      Nil
  }
}

requiresDOM in Test := true
useYarn := true

publishMavenStyle := true

licenses += ("Apache 2", url("https://www.apache.org/licenses/LICENSE-2.0.txt"))

homepage := Some(url("https://outwatch.github.io/"))

scmInfo := Some(ScmInfo(
  url("https://github.com/OutWatch/outwatch"),
  "scm:git:git@github.com:OutWatch/outwatch.git",
  Some("scm:git:git@github.com:OutWatch/outwatch.git")))

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}


pomExtra :=
  <developers>
    <developer>
      <id>ltj</id>
      <name>Luka Jacobowitz</name>
      <url>https://github.com/LukaJCB</url>
    </developer>
  </developers>


pomIncludeRepository := { _ => false }
