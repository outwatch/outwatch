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
