resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.17")
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.6.0")
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.8.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "1.1")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")