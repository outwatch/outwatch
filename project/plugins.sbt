resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.19")
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.8.0")
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.0")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.0-M1")
