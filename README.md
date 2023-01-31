# Outwatch
The Functional and Reactive Web-Frontend Library for Scala.js

[![Typelevel incubator](https://img.shields.io/badge/typelevel-incubator-F51C2B.svg)](http://typelevel.org)
[![codecov](https://codecov.io/gh/outwatch/outwatch/branch/master/graph/badge.svg?token=TGCcxBnOHi)](https://codecov.io/gh/outwatch/outwatch)
[![Discord](https://img.shields.io/discord/632277896739946517.svg?label=&logo=discord&logoColor=ffffff&color=404244&labelColor=6A7EC2)](https://discord.gg/9V8FZTVZ9R)
[![outwatch Scala version support](https://index.scala-lang.org/outwatch/outwatch/outwatch/latest-by-scala-version.svg?platform=sjs1)](https://index.scala-lang.org/outwatch/outwatch/outwatch)


```scala
import outwatch._
import outwatch.dsl._
import colibri._
import cats.effect.{IO, IOApp}

object Main extends IOApp.Simple {
  override def run = {
    val counter = Subject.behavior(0)
    val myComponent = div(
      button("+", onClick(counter.map(_ + 1)) --> counter),
      counter,
    )

    Outwatch.renderReplace[IO]("#app", myComponent)
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

Make sure that `java`, `sbt`, `nodejs`, `yarn` and `github-cli` (optionally) are installed.

```shell
# create new repo on github based on this template
gh repo create my-first-outwatch-project --template outwatch/example --public --clone

# if you want to just get the template locally without creating a github repo:
git clone --depth 1 https://github.com/outwatch/example my-first-outwatch-project

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
  "com.github.cornerman" %%% "colibri-zio"       % "0.7.6", // zio support
  "com.github.cornerman" %%% "colibri-fs2"       % "0.7.6", // fs2 support
  "com.github.cornerman" %%% "colibri-airstream" % "0.7.6", // sirstream support
  "com.github.cornerman" %%% "colibri-rx"        % "0.7.6", // scala.rx support
  "com.github.cornerman" %%% "colibri-router"    % "0.7.6", // Url Router support
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
