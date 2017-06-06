enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)

name := "OutWatch"

normalizedName := "outwatch"

version := "0.9.5"

organization := "io.github.outwatch"

scalaVersion := "2.12.2"

crossScalaVersions := Seq("2.11.11", "2.12.2")


libraryDependencies ++= Seq(
  "com.github.lukajcb" %%% "rxscala-js" % "0.14.0",
  "org.scalatest" %%% "scalatest" % "3.0.1" % "test",
  "org.scalacheck" %%% "scalacheck" % "1.13.4" % "test"
)

npmDependencies in Compile ++= Seq(
  "rxjs" -> "5.4.0",
  "snabbdom" -> "0.6.9"
)

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
