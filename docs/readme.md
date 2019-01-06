# Outwatch

To install my project
```scala
libraryDependencies += "com" % "lib" % "@VERSION@"
```

```scala mdoc:js
import outwatch.dom._
import outwatch.dom.dsl._
import monix.execution.Scheduler.Implicits.global

OutWatch.renderInto(node, h1("Hello World")).unsafeRunSync()
```

```scala mdoc:js
import outwatch.dom._
import outwatch.dom.dsl._

import monix.reactive.Observable
import monix.execution.Scheduler.Implicits.global

import concurrent.duration._

val counter = Observable.interval(1 second)
val counterComponent = div("count: ", counter)

OutWatch.renderReplace(node, counterComponent).unsafeRunSync()
```
