# Outwatch
The Functional and Reactive Web-Frontend Library for ScalaJS

[![Typelevel incubator](https://img.shields.io/badge/typelevel-incubator-F51C2B.svg)](http://typelevel.org) [![Scala.js](http://www.scala-js.org/assets/badges/scalajs-1.0.0.svg)](http://scala-js.org) [![Gitter chat](https://badges.gitter.im/gitterHQ/gitter.png)](https://gitter.im/Outwatch/Lobby)


```scala
import outwatch._
import outwatch.dsl._
import colibri._
import cats.effect.IO

object Main {
  def main(args: Array[String]): Unit = {
    val counter = Subject.behavior(0)
    val myComponent = div(
      button("+", onClick(counter.map(_ + 1)) --> counter),
      counter,
    )

    Outwatch.renderReplace[IO]("#app", myComponent).unsafeRunSync()
  }
}
```
In Outwatch, you can describe your whole web application without doing any side effect - you only run your application when rendering it.

* Write UI-components using pure functions
* Manage state in a referentially transparent way using [cats-effect](https://github.com/typelevel/cats-effect)
* Built-in lightweight `Observable` and `Subject` types from [colibri](http://github.com/cornerman/colibri)
* Seamlessly works with existing reactive programming libraries: [ZIO](https://github.com/zio/zio), [fs2](https://github.com/typelevel/fs2), [Airstream](https://github.com/raquo/airstream), [scala.rx](https://github.com/lihaoyi/scala.rx)
* Low-boilerplate, many convenient helper functions
* Built on top of [snabbdom](https://github.com/snabbdom/snabbdom), a virtual dom library

You will find interactive examples and explanations in our [documentation](https://outwatch.github.io/docs/readme.html).

## [Documentation](https://outwatch.github.io/docs/readme.html)

## Quickstart

### Template

The quickest way to play with outwatch is to use our template.

Make sure that `java`, `sbt`, `nodejs` and `yarn` are installed.

```bash
sbt new outwatch/seed.g8
```

In your newly created project directory, run:

```bash
sbt dev
```

and point your browser to http://localhost:12345.

### Manual setup

Add the `scalajs` and `scalajs-bundler` plugins to your `project/plugins.sbt`:
```scala
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.x.x")
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "x.x.x")
```

Add the outwatch dependencies to your `build.sbt`:
```scala
enablePlugins(ScalaJSPlugin)
enablePlugins(ScalaJSBundlerPlugin)

resolvers += "jitpack" at "https://jitpack.io"
val outwatchVersion = "<latest outwatch version>"
libraryDependencies ++= Seq(
  "io.github.outwatch"   %%% "outwatch"          % outwatchVersion,
  // optional dependencies:
  "com.github.cornerman" %%% "colibri-zio"       % "0.3.2", // ZIO
  "com.github.cornerman" %%% "colibri-airstream" % "0.3.2", // Airstream
  "com.github.cornerman" %%% "colibri-rx"        % "0.3.2", // Scala.rx
  "com.github.cornerman" %%% "colibri-router"    % "0.3.2", // Url Router
)

```

## Bugs and Feedback
For bugs, questions and discussions please use [GitHub Issues](https://github.com/outwatch/outwatch/issues).


## Community
We adopted the [Scala Code of Conduct](https://www.scala-lang.org/conduct/). People are expected to follow it when discussing Outwatch on the Github page, Gitter channel, or other venues.

## LICENSE

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<https://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
