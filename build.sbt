enablePlugins(ScalaJSPlugin)

name := "OutWatch"

normalizedName := "outwatch"

version := "0.7.1"

organization := "io.github.outwatch"

scalaVersion := "2.12.1"

crossScalaVersions := Seq("2.11.8", "2.12.1")

libraryDependencies ++= Seq(
  "com.github.lukajcb" %%% "rxscala-js" % "0.13.2",
  "org.scalatest" %%% "scalatest" % "3.0.1" % "test",
  "org.scalacheck" %%% "scalacheck" % "1.13.4" % "test"
)

jsDependencies ++= Seq(
  "org.webjars.npm" % "rxjs" % "5.1.0" / "bundles/Rx.min.js" commonJSName "Rx",
  "org.webjars.npm" % "snabbdom" % "0.5.4" / "dist/snabbdom.js",
  "org.webjars.npm" % "snabbdom" % "0.5.4" / "dist/h.js",
  "org.webjars.npm" % "snabbdom" % "0.5.4" / "dist/snabbdom_class.js",
  "org.webjars.npm" % "snabbdom" % "0.5.4" / "dist/snabbdom_eventlisteners.js",
  "org.webjars.npm" % "snabbdom" % "0.5.4" / "dist/snabbdom_props.js",
  "org.webjars.npm" % "snabbdom" % "0.5.4" / "dist/snabbdom_style.js",
  "org.webjars.npm" % "snabbdom" % "0.5.4" / "dist/snabbdom_attributes.js"
)

jsEnv := PhantomJSEnv().value


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
