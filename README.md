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
