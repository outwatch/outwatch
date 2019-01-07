# OutWatch - Functional and reactive Web-Frontend Library with Reactive Programming, VirtualDom and Scala [![Typelevel incubator](https://img.shields.io/badge/typelevel-incubator-F51C2B.svg)](http://typelevel.org) [![Build Status](https://travis-ci.org/OutWatch/outwatch.svg?branch=master)](https://travis-ci.org/OutWatch/outwatch) [![Scala.js](http://scala-js.org/assets/badges/scalajs-0.6.15.svg)](http://scala-js.org) [![Gitter chat](https://badges.gitter.im/gitterHQ/gitter.png)](https://gitter.im/OutWatch/Lobby)

```scala mdoc:js
import outwatch.dom._
import outwatch.dom.dsl._
import monix.execution.Scheduler.Implicits.global

OutWatch.renderInto("#app", h1("Hello World")).unsafeRunSync()
```

Syntax is almost exactly as in [ScalaTags](http://www.lihaoyi.com/scalatags/). The UI is made reactive with [Monix](https://monix.io/).

You can find more examples and features at the end of this readme.


## Getting started
### Start with a template
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


### Use in an already existing project
Install `java`, `sbt` and  `nodejs`, if you haven't already.
Create a new SBT project and add the ScalaJS and Scala-js-bundler plugin to your `plugins.sbt`:
```scala
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.26")
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.14.0")
```
Then add the outwatch dependency to your `build.sbt`.

```scala
libraryDependencies += "io.github.outwatch" %%% "outwatch" % "@VERSION@"
```

And enable the `scalajs-bundler` plugin:
```scala
enablePlugins(ScalaJSBundlerPlugin)
```

If you are curious and want to try the state of the current `master` branch, add the following instead:

```scala
resolvers += "jitpack" at "https://jitpack.io"
libraryDependencies += "com.github.outwatch" % "outwatch" % "master-SNAPSHOT"
```

When using [JitPack](https://jitpack.io), it is often more useful to point to a specific commit, to make your builds reproducible:

```scala
libraryDependencies += "com.github.outwatch" % "outwatch" % "@HEADCOMMIT@"
```

Like that you can try the latest features from specific commits on `master`, other branches or PRs.

To configure hot reloading with webpack devserver, check out [build.sbt](https://github.com/OutWatch/seed.g8/blob/master/src/main/g8/build.sbt) and [webpack.config.dev.js](https://github.com/OutWatch/seed.g8/blob/master/src/main/g8/webpack.config.dev.js) from the [g8 template](https://github.com/OutWatch/seed.g8).

If anything is not working, cross-check how things are done in the template.

## Bugs and Feedback
For bugs, questions and discussions please use [Github Issues](https://github.com/OutWatch/outwatch/issues).


## Community
We adopted the [Typelevel Code of Conduct](http://typelevel.org/conduct.html). People are expected to follow it when discussing OutWatch on the Github page, Gitter channel, or other venues.


## Documentation and Examples
Outwatch is a web frontend UI framework written in ScalaJS.

If you find any error in the examples, please open an issue on GitHub.

There is a changelog which contains examples of the latest changes: [CHANGELOG.md](CHANGELOG.md)

There is also the outdated but conceptually still correct [documentation](https://outwatch.github.io/) -  [contributions welcome](https://github.com/OutWatch/outwatch.github.io).

### Hello World
In your html file, create an element, which you want to replace by dynamic content:

```html
...
<body>
    <div id="app"></div>
    <!-- your compiled javascript should be imported here -->
</body>
...
```

To render html content with outwatch, create a component and render it into the given element:

```scala mdoc:js
import outwatch.dom._
import outwatch.dom.dsl._
import monix.execution.Scheduler.Implicits.global

object Main {
  def main(args: Array[String]): Unit = {
    
    val myComponent = div("Hello World")

    OutWatch.renderReplace("#app", myComponent).unsafeRunSync()
  }
}
```

Running `Main` will replace `<div id="app"></div>` with the content defined in `myComponent`:

```html
...
<body>
    <div id="app">Hello World</div>
    ...
</body>
...
```

**Important:** In your application, `OutWatch.renderReplace` should be called only once at the end of the main method. To create dynamic content, you will design your data-flow with `Obvervable`, `Subject` and/or `Handler` and then instantiate it only once with this method call. Before that, no `Observable` subscription will happen.

### Static Content
First, we will focus on creating immutable/static content that will not change over time. The following examples illustrate to construct and transform HTML/SVG tags, attributes and inline stylesheets.

#### Concatenating Strings
```scala mdoc:js:shared
import outwatch.dom._
import outwatch.dom.dsl._
```

```scala mdoc:js
div("Hello ", "World")
// <div>Hello World</div>
```


#### Nesting
```scala mdoc:js
div(span("Hey ", b("you"), "!"))
// <div>Hey <b>you</b>!</div>
```


#### Primitives
```scala mdoc:js
div(true, 0, 1000L, 3.0)
// <div>true010003.0</div>
```


#### Attributes
Attributes are put inside the tag.

```scala mdoc:js
div(id := "test")
// <div id="test"></div>
```

The order of content and attributes does not matter.

```scala mdoc:js
div("How ", id := "test", "are", title := "cool", " you?")
// <div id="test" title="cool">How are you?</div>
```


#### Styles
Styles are also written into the tag. All style properties have to be written in *camelCase*.

```scala mdoc:js
div(color := "tomato", "Hello")
// <div style="color: blue">Hello</div>
```

Multiple styles will me merged to one style attribute:

```scala mdoc:js
div(backgroundColor := "powderblue", border := "2px solid #222", "Hello")
// <div style="background-color: powderblue; border: 2px solid #222">Hello</div>
```

Again, the order of styles, attributes and inner tags does not matter:

```scala mdoc:js
div(h1("Welcome to my website"), backgroundColor := "powderblue", id := "header")
// <div style="background-color: powderblue" id="header">Welcome to my website</div>
```

Some styles have type safe values:

```scala mdoc:js
div(cursor.pointer, fontWeight.bold, display.flex)
// <div style="cursor: pointer; font-weight: bold; display: flex;"></div>
```

If you are missing more type safe values, please contribute to [Scala Dom Types](https://github.com/raquo/scala-dom-types). Example implementation: [fontWeight](https://github.com/raquo/scala-dom-types/blob/master/shared/src/main/scala/com/raquo/domtypes/generic/defs/styles/Styles.scala#L1711)


#### Attributes, which are scala keywords
There are some attributes and styles which are reserved scala keywords. You can use them with backticks:

```scala mdoc:js
div(`class` := "item", "My Item")
// <div class="item">My Item</div>

label(`for` := "inputid")
// <label for="inputid" />
```

There is also a shortcut for the class atrribute:

```scala mdoc:js
div(cls := "myclass")
// <div class="myclass"></div>
```

Source Code: [OutwatchAttributes.scala](src/main/scala/outwatch/dom/OutwatchAttributes.scala#L65)


#### Overriding attributes
Attributes and styles with the same name will be overwritten. Last wins.

```scala mdoc:js
div(color := "blue", color := "green")
// <div style="color: green"></div>
```

Source Code: [DomUtils.scala](src/main/scala/outwatch/dom/helpers/DomUtils.scala#L8)

#### Class accumulation
Classes are not overwritten, they accumulate.

```scala mdoc:js
div(cls := "tiny", cls := "button")
// <div class="tiny button"></div>
```

#### Custom attributes, styles and tags
All the tags, attributes and styles available in outwatch come from [Scala Dom Types](https://github.com/raquo/scala-dom-types).
If you want to use something not available in Scala Dom Types, you can use custom builders:

```scala mdoc:js
htmlTag("app")(style("user-select") := "none", attr("everything") := "possible")
// <app style="user-select: none" everything="possible"></div>
```

You can also define the accumulation behavior of custom attributes:
```scala mdoc:js
div(
  attr("everything").accum("-") := "is",
  attr("everything").accum("-") := "possible",
)
// <div everything="is-possible"></div>
```

If you think there is something missing in Scala Dom Types, please open a PR or Issue. Usually it's just one line of code.

Source Code: [DomTypes.scala](src/main/scala/outwatch/dom/DomTypes.scala)


#### Data attributes
Data attributes make use of [`scala.Dynamic`](https://www.scala-lang.org/api/current/scala/Dynamic.html), so you can write things like:

```scala mdoc:js
div(data.payload := "17")
// <div data-payload="17"></div>
```

Source Code: [OutwatchAttributes.scala](src/main/scala/outwatch/dom/OutwatchAttributes.scala#L70), [Builder.scala](src/main/scala/outwatch/dom/helpers/Builder.scala#L39)


#### SVG
SVG tags and attributes are available via an extra import. Namespacing is automatically handled for you.

```scala mdoc:js
val graphic = {
 import svg._
 svg(
   viewBox := "0 0 10 10",
   g(
     transform := "matrix(.096584 0 0 .096584 -.0071925 -18.66)",
     path(d := "M10 10 C 20 20, 40 20, 50 10", fill := "mistyrose")
   )
 )
}

// <svg viewBox="0 0 10 10"><g transform="matrix(.096584 0 0 .096584 -.0071925 -18.66)"><path d="M10 10 C 20 20, 40 20, 50 10" fill="mistyrose"></path></g></svg>
```


#### Option and Seq
Outwatch tries hard to render everything you throw at it. Combine `Option` and `Seq` to fit your needs. Note, that outwatch does not accept `Set`, since the order is undefined.

```scala mdoc:js
div(
  Some("thing"),
  Some(color := "steelblue"),
  fontSize :=? Some("70px"),
  None,
  Seq("Hey", "How are you?"),
  List("a", "b", "c").map(span(_)),
  Some(Seq("x")),
)
// <div style="color: steelblue; font-size: 70px;">
//   thing
//   Hey
//   How are you?
//   <span>a</span>
//   <span>b</span>
//   <span>c</span>
//   x
// </div>
```

Source Code: [AsVDomModifier.scala](src/main/scala/outwatch/AsVDomModifier.scala)



#### Types
The important types we were using in the examples above are `VNode` and `VDomModifier`:

```scala mdoc:js
val vnode: VNode = div()
val modifiers: List[VDomModifier] = List("Hello", id := "main", color := "tomato", vnode)
```

Every `VNode` contains a sequence of `VDomModifier`. A `VNode` is a `VDomModifier` itself.

There are implicits for converting primitives, `Option[VDomModifier]`, `Seq[VDomModifier]` to `VDomModifier`.


#### Grouping Modifiers
To make a set of modifiers reusable you can group them to become one `VDomModifier`.

```scala mdoc:js
val bigFont = VDomModifier(fontSize := "40px", fontWeight.bold)
div("Argh!", bigFont)
// <div style="font-size: 40px; font-weight: bold;">Argh!</div>
```

You can also use a `Seq[VDomModifier]` directly instead of using `apply` defined in the [VDomModifier](src/main/scala/outwatch/dom/package.scala) object.


#### Components
Outwatch does not have the concept of a component itself. You can just pass the `VNode`s and `VDomModifier`s around and build your own abstractions using functions and classes. When we are talking about components in this documentation, we are usually referring to a `VNode` or a function returning a `VNode`.

```scala mdoc:js
def fancyHeadLine(content: String) = h1(borderBottom := "1px dashed tomato", content)
fancyHeadLine("I like tomatoes.")
// <h1 style="border-bottom: 1px dashed tomato;">I like tomatoes.</h1>
```


#### Transforming Components
Components are immutable, we can only modify them by creating a changed copy. Like you may know from Scalatags, you can call `.apply(...)` on any `VNode`, *append* more modifiers and get a new `VNode` with the applied changes back.

```scala mdoc:js
val a = div("dog")
a(title := "the dog")
// <div title="the dog">dog</div>
```

This can be useful for reusing html snippets.

```scala mdoc:js
val box = div(width := "100px", height := "100px")

div(
  box(backgroundColor := "powderblue"),
  box(backgroundColor := "mediumseagreen"),
)

// <div>
//  <div style="width: 100px; height: 100px; background-color: powderblue;"> </div>
//  <div style="width: 100px; height: 100px; background-color: mediumseagreen;"></div>
// </div>
```

Since modifiers are *appended*, they can overwrite existing ones. This is useful to adjust existing components to your needs.

```scala mdoc:js
val box = div(width := "100px", height := "100px")
box(backgroundColor := "mediumseagreen", width := "200px")
// <div style="width: 200px; height: 100px; background-color: mediumseagreen;"></div>
```

You can also *prepend* modifiers. This can be useful to provide defaults retroactively.

```scala mdoc:js
def withBorderIfNotProvided(vnode: VNode) = vnode.prepend(border := "3px solid coral")
div(
  withBorderIfNotProvided(div("hello", border := "7px solid moccasin")),
  withBorderIfNotProvided(div("hello")),
)
// <div>
//   <div style="border: 7px solid moccasin;">hello</div>
//   <div style="border: 3px solid coral;">hello</div>
// </div>
```

Source Code: [VDomModifier.scala](src/main/scala/outwatch/dom/VDomModifier.scala#L146)


#### Use-Case: Flexbox
When working with [Flexbox](https://css-tricks.com/snippets/css/a-guide-to-flexbox/), you can set styles for the **container** and **children**. With `VNode.apply()` you can have all flexbox-related styles in one place. The child-components don't have to know anything about flexbox, even though they get specific styles assigned.

```scala mdoc:js
val itemA = div("A", backgroundColor := "mediumseagreen")
val itemB = div("B", backgroundColor := "tomato")

div(
  height := "100px",
  border := "1px solid black",

  display.flex,

  itemA(flexBasis := "50px"),
  itemB(alignSelf.center),
)
// <div style="height: 100px; border: 1px solid black; display: flex;">
//   <div style="background-color: mediumseagreen; flex-basis: 50px;">A</div>
//   <div style="background-color: tomato; align-self: center;">B</div>
// </div>
```


### Dynamic Content
To visualize updates, use an `Observable[VDomModifier]` as if it was a `VDomModifier`.

```scala mdoc:js:shared
import monix.reactive.Observable
import monix.execution.Scheduler.Implicits.global
```

```scala mdoc:js:shared
import concurrent.duration._ // needed for the following examples
```

```scala mdoc:js
object Main {
  def main(args: Array[String]): Unit = {

    val counter = Observable.interval(1 second)
    val counterComponent = div("count: ", counter)

    OutWatch.renderReplace("#app", counterComponent).unsafeRunSync()
  }
}
```

**Important:** In your application, `OutWatch.renderReplace` should be called only once at the end of the main method. To create dynamic content, you will design your data-flow with `Obvervable`, `Subject` and/or `Handler` and then instantiate it only once with this method call. Before that, no `Observable` subscription will happen.


#### Dynamic attributes
Attributes can also take dynamic values.

```scala mdoc:js
val color = Observable.interval(1 second).map(i => if(i % 2 == 0) "deepskyblue" else "gold")
div(width := "100px", height := "100px", backgroundColor <-- color)
```

#### Streaming Modifiers and VNodes
You can stream arbitrary `VDomModifiers`.

```scala mdoc:js
val dynamicSize:Observable[VDomModifier] = Observable.interval(1 second).map(i => fontSize := s"${i}px")
div("Hello!", dynamicSize)
```

```scala mdoc:js
val nodeStream:Observable[VNode] = Observable.interval(1 second).map(i => div(s"Number $i"))
div("Hello ", nodeStream)
```


### Advanced

### Using other streaming libraries than Monix

We have prepared the two typeclasses `AsValueObservable` and `AsObserver` to work with arbitrary streaming libraries. `AsValueObservable` is for the reading part of the stream, and `AsObserver` is for the writing part.

Example: To use outwatch with [scala.rx](https://github.com/lihaoyi/scala.rx):

```scala mdoc:js:shared
import rx._
import monix.reactive._
import monix.execution._

implicit object RxAsValueObservable extends AsValueObservable[Rx] {
  override def as[T](stream: Rx[T]): ValueObservable[T] = new ValueObservable[T]{
    def value = Option(stream.now)
    def observable = Observable.create[T](OverflowStrategy.Unbounded) { observer =>
      implicit val ctx = Ctx.Owner.Unsafe
      val obs = stream.triggerLater(observer.onNext(_))
      Cancelable(() => obs.kill())
    }
  }
}

implicit object VarAsObserver extends AsObserver[Var] {
  override def as[T](stream: Var[_ >: T]): Observer[T] = new Observer.Sync[T] {
    override def onNext(elem: T): Ack = {
      stream() = elem
      Ack.Continue
    }
    override def onError(ex: Throwable): Unit = throw ex
    override def onComplete(): Unit = ()
  }
}

// if you want to use managed()
implicit def obsToCancelable(obs: Obs): Cancelable = {
  Cancelable(() => obs.kill())
}
```

```scala mdoc:js:reset
implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
val showMessage = Var(false)
val component = div(
  Rx{
    if( showMessage() )
        div("Hello scala.rx")
    else
        button("show", onClick(true) --> showMessage)
  },
  managed(() => showMessage.foreach(println))
)

OutWatch.renderReplace(node, div("yooo")).unsafeRunSync()
```

### debugging snabbdom patches

```scala mdoc:js
import scala.scalajs.js.JSON

helpers.OutwatchTracing.patch.zipWithIndex.foreach { case (proxy, index) =>
  org.scalajs.dom.console.log(s"Snabbdom patch ($index)!", JSON.parse(JSON.stringify(proxy)), proxy)
}
```

### tracing exceptions in your components

Dynamic components with `Observables` can have errors. This is if `onError` is called on the underlying `Observer`. You can trace them in OutWatch with:`
```scala mdoc:js
helpers.OutwatchTracing.error.foreach { case throwable =>
  org.scalajs.dom.console.log(s"Exception while patching an Outwatch compontent: ${throwable.getMessage}")
}
```



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
