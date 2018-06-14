# OutWatch - Functional and reactive Web-Frontend Library with Reactive Programming, VirtualDom and Scala [![Typelevel incubator](https://img.shields.io/badge/typelevel-incubator-F51C2B.svg)](http://typelevel.org) [![Build Status](https://travis-ci.org/OutWatch/outwatch.svg?branch=master)](https://travis-ci.org/OutWatch/outwatch) [![Scala.js](http://scala-js.org/assets/badges/scalajs-0.6.15.svg)](http://scala-js.org) [![Gitter chat](https://badges.gitter.im/gitterHQ/gitter.png)](https://gitter.im/OutWatch/Lobby)


```scala
import outwatch.dom._
import outwatch.dom.dsl._
import monix.execution.Scheduler.Implicits.global

OutWatch.renderInto("#app", h1("Hello World")).unsafeRunSync()
```

Syntax is almost exactly as in [ScalaTags](http://www.lihaoyi.com/scalatags/) combined with Observables from [Monix](https://monix.io/).

You can find more examples and features in the changelog: https://github.com/OutWatch/outwatch/blob/changelog/CHANGELOG.md

## Getting started

### g8 template
For a quick start, install `java`, `sbt`, `nodejs` and `yarn` and use the following g8 template:
```bash
sbt new outwatch/seed.g8
```

In your newly created project folder, run:
```bash
sbt dev
```

and point your browser to http://localhost:8080.

Changes to the code will trigger a recompile and automatically refresh the page in the browser.

### Manual usage
Install `java`, `sbt` and  `nodejs`, if you haven't already.
Create a new SBT project and add the ScalaJS and Scala-js-bundler plugin to your `plugins.sbt`:
```scala
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.23")
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.13.0")
```
Then add the outwatch dependency to your `build.sbt`.

```scala
libraryDependencies += "io.github.outwatch" %%% "outwatch" % "1.0.0-RC2"
```

If you are curious and want to try the state of the current `master` branch, add the following instead:

```scala
resolvers += "jitpack" at "https://jitpack.io"
libraryDependencies += "com.github.outwatch" % "outwatch" % "master-SNAPSHOT"
```

To configure hot reloading with webpack devserver, check out [build.sbt](https://github.com/OutWatch/seed.g8/blob/master/src/main/g8/build.sbt) and [webpack.config.dev.js](https://github.com/OutWatch/seed.g8/blob/master/src/main/g8/webpack.config.dev.js) from the [g8 template](https://github.com/OutWatch/seed.g8).

### Documentation
There is a PR with a corresponding changelog which also serves as documentation with examples: https://github.com/OutWatch/outwatch/blob/changelog/CHANGELOG.md

There is also the slightly outdated but conceptually still correct [documentation](https://outwatch.github.io/) -  [https://github.com/OutWatch/outwatch.github.io](contributions welcome).

## Three main goals of OutWatch

1. Updating DOM efficiently without sacrificing abstraction => Virtual DOM
2. Handling subscriptions automatically
3. Removing or restricting the need for Higher Order Observables


## Bugs and Feedback

For bugs, questions and discussions please use the [Github Issues](https://github.com/OutWatch/outwatch/issues).

## Community

We adopted the
[Typelevel Code of Conduct](http://typelevel.org/conduct.html). People are expected to follow it when
discussing OutWatch on the Github page, Gitter channel, or other venues.

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
