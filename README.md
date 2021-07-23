# OutWatch
Functional and reactive Web-Frontend Library for ScalaJS

[![Typelevel incubator](https://img.shields.io/badge/typelevel-incubator-F51C2B.svg)](http://typelevel.org) [![Build Status](https://travis-ci.org/OutWatch/outwatch.svg?branch=master)](https://travis-ci.org/OutWatch/outwatch) [![Scala.js](http://www.scala-js.org/assets/badges/scalajs-1.0.0.svg)](http://scala-js.org) [![Gitter chat](https://badges.gitter.im/gitterHQ/gitter.png)](https://gitter.im/OutWatch/Lobby)

Syntax is almost exactly as in [ScalaTags](http://www.lihaoyi.com/scalatags/). The UI can be made reactive and allows for easy integration of third-party FRP libraries (for example [Monix](https://monix.io/), [scala.rx](https://github.com/lihaoyi/scala.rx) or [airstream](https://github.com/raquo/airstream)). We integrate tightly with [cats](https://github.com/typelevel/cats) and [cats-effect](https://github.com/typelevel/cats-effect) to build safe web applications. In OutWatch, you can describe your whole web application without doing any side effect - you only run your application when rendering it.

```scala
import outwatch._
import outwatch.dsl._

val hello = h1("Hello World")

val app = OutWatch.renderInto[IO]("#app", hello)

// Nothing happend yet, you just described your web application.
// Now it's time to push the start button:
app.unsafeRunSync()
```

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
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.1.1")
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.18.0")
```

Then add the outwatch dependency to your `build.sbt`, you can depend on the current `master` branch:
```scala
resolvers += "jitpack" at "https://jitpack.io"
libraryDependencies += "com.github.outwatch.outwatch" %%% "outwatch" % "master-SNAPSHOT"
```

If you want to use the latest stable release, you can use:
```scala
libraryDependencies += "io.github.outwatch" %%% "outwatch" % "1.0.0-RC2"
```

And enable the `scalajs-bundler` plugin:
```scala
enablePlugins(ScalaJSBundlerPlugin)
```

If you want to use utilities for `Store`, `WebSocket` or `Http`, add the following:
```scala
libraryDependencies += "com.github.outwatch.outwatch" %%% "outwatch-util" % "master-SNAPSHOT"
```

If you want support for Monix in OutWatch, you need to add the following dependency as well:
```scala
libraryDependencies += "com.github.cornerman.colibri" %%% "colibri-monix" % "master-SNAPSHOT"
libraryDependencies += "com.github.outwatch.outwatch" %%% "outwatch-monix" % "master-SNAPSHOT" // for handler factories
```

If you want support for scala.rx in OutWatch, you need to add the following dependency as well:
```scala
libraryDependencies += "com.github.cornerman.colibri" %%% "colibri-rx" % "master-SNAPSHOT"
```

When using [JitPack](https://jitpack.io), it is often more useful to point to a specific commit, to make your builds reproducible:

```scala
libraryDependencies += "com.github.outwatch.outwatch" %%% "outwatch" % "f07849c81"
```

Like that you can try the latest features from specific commits on `master`, other branches or PRs.

To configure hot reloading with webpack devserver, check out [build.sbt](https://github.com/OutWatch/seed.g8/blob/master/src/main/g8/build.sbt) and [webpack.config.dev.js](https://github.com/OutWatch/seed.g8/blob/master/src/main/g8/webpack.config.dev.js) from the [g8 template](https://github.com/OutWatch/seed.g8).

If anything is not working, cross-check how things are done in the template.

### Use common javascript libraries with OutWatch

We have prepared helpers for some javascript libraries. You can find them in the [OutWatch-libs](https://github.com/outwatch/outwatch-libs) Repository.

## Bugs and Feedback
For bugs, questions and discussions please use [GitHub Issues](https://github.com/OutWatch/outwatch/issues).


## Community
We adopted the [Scala Code of Conduct](https://www.scala-lang.org/conduct/). People are expected to follow it when discussing OutWatch on the Github page, Gitter channel, or other venues.


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

```scala
import outwatch._
import outwatch.dsl._

object Main {
  def main(args: Array[String]): Unit = {
    
    val myComponent = div("Hello World")

    OutWatch.renderReplace[IO]("#app", myComponent).unsafeRunSync()
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
```scala
div("Hello ", "World")
// <div>Hello World</div>
```


#### Nesting
```scala
div(span("Hey ", b("you"), "!"))
// <div><span>Hey <b>you</b>!</span></div>
```


#### Primitives
```scala
div(true, 0, 1000L, 3.0)
// <div>true010003.0</div>
```


#### Attributes
Attributes are put inside the tag.

```scala
div(idAttr := "test")
// <div id="test"></div>
```

The order of content and attributes does not matter.

```scala
div("How ", idAttr := "test", "are", title := "cool", " you?")
// <div id="test" title="cool">How are you?</div>
```


#### Styles
Styles are also written into the tag. All style properties have to be written in *camelCase*.

```scala
div(color := "tomato", "Hello")
// <div style="color: tomato">Hello</div>
```

Multiple styles will me merged to one style attribute:

```scala
div(backgroundColor := "powderblue", border := "2px solid #222", "Hello")
// <div style="background-color: powderblue; border: 2px solid #222">Hello</div>
```

Again, the order of styles, attributes and inner tags does not matter:

```scala
div(h1("Welcome to my website"), backgroundColor := "powderblue", idAttr := "header")
// <div style="background-color: powderblue" id="header">Welcome to my website</div>
```

Some styles have type safe values:

```scala
div(cursor.pointer, fontWeight.bold, display.flex)
// <div style="cursor: pointer; font-weight: bold; display: flex;"></div>
```

If you are missing more type safe values, please contribute to [Scala Dom Types](https://github.com/raquo/scala-dom-types). Example implementation: [fontWeight](https://github.com/raquo/scala-dom-types/blob/master/shared/src/main/scala/com/raquo/domtypes/generic/defs/styles/Styles.scala#L1711)


#### Attributes, which are scala keywords
There are some attributes and styles which are reserved scala keywords. You can use them with backticks:

```scala
div(`class` := "item", "My Item")
// <div class="item">My Item</div>

label(`for` := "inputid")
// <label for="inputid" />
```

There is also a shortcut for the class atrribute:

```scala
div(cls := "myclass")
// <div class="myclass"></div>
```


#### Overriding attributes
Attributes and styles with the same name will be overwritten. Last wins.

```scala
div(color := "blue", color := "green")
// <div style="color: green"></div>
```

#### Class accumulation
Classes are not overwritten, they accumulate.

```scala
div(cls := "tiny", cls := "button")
// <div class="tiny button"></div>
```

#### Custom attributes, styles and tags
All the tags, attributes and styles available in outwatch come from [Scala Dom Types](https://github.com/raquo/scala-dom-types).
If you want to use something not available in Scala Dom Types, you can use custom builders:

```scala
htmlTag("app")(style("user-select") := "none", attr("everything") := "possible")
// <app style="user-select: none" everything="possible"></div>
```

You can also define the accumulation behavior of custom attributes:
```scala
div(
  attr("everything").accum("-") := "is",
  attr("everything").accum("-") := "possible",
)
// <div everything="is-possible"></div>
```

If you think there is something missing in Scala Dom Types, please open a PR or Issue. Usually it's just one line of code.

Source Code: [DomTypes.scala](outwatch/src/main/scala/outwatch/definitions/DomTypes.scala)


#### Data and Aria attributes
Data and aria attributes make use of [`scala.Dynamic`](https://www.scala-lang.org/api/current/scala/Dynamic.html), so you can write things like:

```scala
div(
    data.payload := "17",
    data.`consent-required` := "Are you sure?",
    data.message.success := "Message sent!",
    aria.hidden := "true",
)
// <div 
//    data-payload="17"
//    data-consent-required="Are you sure?"
//    data-message-success="Message sent!"
//    aria-hidden="true"
//    >
// </div>
```

Source Code: [OutwatchAttributes.scala](outwatch/src/main/scala/outwatch/definitions/OutwatchAttributes.scala#L75), [Builder.scala](outwatch/src/main/scala/outwatch/helpers/Builder.scala#L35)


#### SVG
SVG tags and attributes are available via an extra import. Namespacing is automatically handled for you.

```scala
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

```scala
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

#### Types
The important types we were using in the examples above are `VNode` and `VDomModifier`:

```scala
val vnode: VNode = div()
val modifiers: List[VDomModifier] = List("Hello", idAttr := "main", color := "tomato", vnode)
```

Every `VNode` contains a sequence of `VDomModifier`. A `VNode` is a `VDomModifier` itself.

There are implicits for converting primitives, `Option[VDomModifier]`, `Seq[VDomModifier]` to `VDomModifier`.

Source Code: [Render.scala](outwatch/src/main/scala/outwatch/Render.scala)

#### Grouping Modifiers
To make a set of modifiers reusable you can group them to become one `VDomModifier`.

```scala
val bigFont = VDomModifier(fontSize := "40px", fontWeight.bold)
div("Argh!", bigFont)
// <div style="font-size: 40px; font-weight: bold;">Argh!</div>
```

If you want to reuse the `bigFont`, but want to overwrite one of its properties, you can combine two `VDomModifier`. Here the latter `fontSize` will overwrite the one from `bigFont`:
```scala
val bigFont2 = VDomModifier(bigFont, fontSize := "99px")
```

You can also use a `Seq[VDomModifier]` directly instead of using `apply` defined in the [VDomModifier](outwatch/src/main/scala/outwatch/package.scala) object.


#### Components
Outwatch does not have the concept of a component itself. You can just pass the `VNode`s and `VDomModifier`s around and build your own abstractions using functions and classes. When we are talking about components in this documentation, we are usually referring to a `VNode` or a function returning a `VNode`.

```scala
def fancyHeadLine(content: String) = h1(borderBottom := "1px dashed tomato", content)
fancyHeadLine("I like tomatoes.")
// <h1 style="border-bottom: 1px dashed tomato;">I like tomatoes.</h1>
```


#### Transforming Components
Components are immutable, we can only modify them by creating a changed copy. Like you may know from Scalatags, you can call `.apply(...)` on any `VNode`, *append* more modifiers and get a new `VNode` with the applied changes back.

```scala
val a = div("dog")
a(title := "the dog")
// <div title="the dog">dog</div>
```

This can be useful for reusing html snippets.

```scala
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

```scala
val box = div(width := "100px", height := "100px")
box(backgroundColor := "mediumseagreen", width := "200px")
// <div style="width: 200px; height: 100px; background-color: mediumseagreen;"></div>
```

You can also *prepend* modifiers. This can be useful to provide defaults retroactively.

```scala
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

Source Code: [VDomModifier.scala](outwatch/src/main/scala/outwatch/VDomModifier.scala#L92)


##### Use-Case: Flexbox
When working with [Flexbox](https://css-tricks.com/snippets/css/a-guide-to-flexbox/), you can set styles for the **container** and **children**. With `VNode.apply()` you can have all flexbox-related styles in one place. The child-components don't have to know anything about flexbox, even though they get specific styles assigned.

```scala
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

#### Rendering Async Effects

You can render any `cats.effect.Effect` type like `cats.effect.IO` or `monix.eval.Task`. The effect will be run async whenever an element is rendered with this modifier. So you can do:
```scala
div(
  IO {
    doSomething
  }
)
```

If you have an effect that is synchronous in nature, you should consider using a sync effect type instead for performance reasons. A sync effect can be rendered in one go, whereas an async effect might need to patch the dom after it is finished.

#### Rendering Sync Effects

You can render synchronous effects like `cats.effect.SyncIO` or `monix.eval.Coeval` as well (we currently use our own typeclass `colibri.effect.RunSyncEffect` and will port to `cats.effect.SyncEffect` as soon as it is available). The effect will be run sync whenever an element is rendered with this modifier. Example:
```scala
div(
  SyncIO {
    doSomething
  }
)
```

Alternatively you can do the following to achieve the same effect:
```scala
div(
  VDomModifier.delay {
    doSomething
  }
)
```

#### Rendering Futures

You can render Futures as well:
```scala
div(
  Future {
    doSomething
  }
)
```

#### Rendering Custom Types

You can render any custom type! Say you have the following type:

```scala
case class Person(name: String, age: Int)
```

And now you want to be able to render a `Person` just like a normal modifier. This can be done by providing an instance of the `Render` typeclass:
```scala
object Person {

  implicit object PersonRender extends Render[Person] {
    def render(person: Person): VDomModifier = div(
        b(person.name), span(person.age, marginLeft := "5px")
    )
  }

}
```

Thats it, now you can just use `Person` in your dom definitions:
```scala
val person = Person("Hans", age = 48)

div(person)
```

Source Code: [Render.scala](outwatch/src/main/scala/outwatch/Render.scala)

### Dynamic Content

Dynamic content can be displayed as well. OutWatch comes with its own *minimal* reactive library that you can just start using. Additionally we have first-class support for third-party streaming libraries.

You can use observables, streams and reactive variables as if they were a `VDomModifier`.


```scala
import outwatch._
import outwatch.dsl._

import colibri.Observable

import concurrent.duration._

object Main {
  def main(args: Array[String]): Unit = {

    val counter = Observable.interval(1 second)
    val counterComponent = div("count: ", counter)

    OutWatch.renderReplace[IO]("#app", counterComponent).unsafeRunSync()
  }
}
```

You can write the same application with monix:

```scala
import outwatch._
import outwatch.dsl._

import monix.reactive.Observable
import monix.execution.Scheduler.Implicits.global
import colibri.ext.monix._

import concurrent.duration._

object Main {
  def main(args: Array[String]): Unit = {

    val counter = Observable.interval(1 second) // using monix observable
    val counterComponent = div("count: ", counter)

    OutWatch.renderReplace[IO]("#app", counterComponent).unsafeRunSync()
  }
}
```

**Important:** In your application, `OutWatch.renderReplace` should be called only once at the end of the main method. To create dynamic content, you will design your data-flow with `Obvervable`, `Subject` and/or `Handler` and then instantiate it only once with this method call. Before that, no `Observable` subscription will happen.

#### Dynamic attributes
Attributes can also take dynamic values.

```scala
val color = Observable.interval(1 second).map(i => if(i % 2 == 0) "deepskyblue" else "gold")
div(width := "100px", height := "100px", backgroundColor <-- color)
```

#### Streaming Modifiers and VNodes
You can stream arbitrary `VDomModifiers`.

```scala
val dynamicSize: Observable[VDomModifier] = Observable.interval(1 second).map(i => fontSize := s"${i}px")
div("Hello!", dynamicSize)
```

```scala
val nodeStream: Observable[VNode] = Observable(div("I am delayed!")).delay(5.seconds)
div("Hello ", nodeStream)
```

#### Events and EmitterBuilders
We are working with dom elements and want to react to events of these dom elements. For this, there is [EmitterBuilder](outwatch/src/main/scala/outwatch/EmitterBuilder.scala). For example, take the `click` event for which `onClick` is an `EmitterBuilder`:

```scala
button(onClick.foreach { println("Element was clicked!") })
```

You can further `combine`, `map`, `collect`, `filter` and `transform` `EmitterBuilder`:

```scala
button(EmitterBuilder.combine(onMouseUp.use(false), onMouseDown.use(true)).foreach { isDown => println("Button is down? " + isDown })
// this is the same as: button(EmitterBuilder.combine(onMouseUp.map(_ => false), onMouseDown.map(_ => true)).foreach { isDown => println("Button is down? " + isDown })
```

Furthermore, you can create EmitterBuilders from streams with `emitter(stream)` (`EmitterBuilder.ofSource`) or create custom EmitterBuilders with `EmitterBuilder.ofModifier`, `EmitterBuilder.ofNode` or `EmitterBuilder.apply`.

##### Global events

There are helpers for getting a stream of global `document` or `window` events, where you get a `colibri.Observable` for these events. For example:

```scala
import outwatch.dsl._

events.document.onClick.foreach { event => console.log("Click Event", event) }

events.window.onStorage.foreach { event => console.log("Storage Event", event) }

div(emitter(events.document.onClick) --> sink)
```

#### Managing dynamic state

We have seen how to render dynamic content. But how to manage dynamic state? You want to have a reactive variable to hold a variable for you, so you can write into it and stream values from it.

##### Example: Counter

Let's implement a counter in a button. OutWatch provides `Handler`, which is a factory to create a reactive variable (more details later):

```scala
import outwatch.reactive.handler._

// Handler.create returns `SyncIO`
val counter: SyncIO[VNode] = Handler.create[Int](0).map { handler =>
    button(onClick(handler.map(_ + 1)) --> handler, "Counter: ", handler)
}
```

Alternative version of a `counter`:
```scala
val counter = button(
    onClick.useScan0(0)(_ + 1).handled { source =>
        VDomModifier("Counter: ", source)
    }
)
```

##### Example: Input Field

A more involved example is an input field where you want to capture the current value:
```scala
  def render: SyncIO[VNode] = for {
    handler <- Handler.create[String]("Initial")
  } yield {
    div(
      label( "Example!" ),
      input(
        value <-- handler, // by writing into the `handler`, you could change the input value
        onChange.value --> handler
      ),
      div("Current Value:", handler)
    )
  }
```

##### Handler factories

Methods like `Handler.create` are available for different streaming libarries (e.g. our own `colibri` and `monix`):

```scala
import outwatch.reactive.handler._ // or outwatch.reactive.handlers.monix._

val handler: SyncIO[Handler[Int]] = Handler.create[Int](1)
val source: HandlerSource[Int] = HandlerSource[Int](3)
val sink: HandlerSink[Int] = HandlerSink.create(onNext, onError)
```

You typically just want one of these Environments in scope, the types would name-clash overwise. And the handler environment is totally optional, you can create your Handlers of the library of your choice yourself and everything will work out of the box without the environment. The environment is just to have a vocabulary of how to create sources, sinks, handlers without using concrete types names from third-party libraries. This way you can switch a whole file to another streaming library by merely changing an import from `outwatch.reactive.handler._` to `outwatch.reactive.handlers.monix._`. Of course, this will never cover all complex details that might be needed, but is target for basic use-cases and should be enough for most things. You have the streaming typeclasses as well to abstract further over your types.

##### Referential transparency

The factory `Handler.create` returns a `SyncIO` to be referential transparent. Take the counter example:

```scala

def getCounterComponent: SyncIO[VNode] = Handler.create(0).map { handler => // alternative: Handler.createF[IO]
    button(onClick(handler.map(_ + 1)) --> handler, handler)
}

def getUnsafeCounterComponent: VNode = {
    val handler = Handler.unsafe(0)
    button(onClick(handler.map(_ + 1)) --> handler, handler)
}

// We can test whether the above functions are referential transparent

// The first one is referential transparent, because component1 and component2 are equivalent
val counter = getCounterComponent
val component1 = div(counter, counter) // two counter buttons with separate state
val component2 = div(getCounterComponent, getCounterComponent) // two counter buttons with separate state

// The second one is not, because component1 and component2 behave differently
val counter = getUnsafeCounterComponent
val component1 = div(counter, counter) // two counter buttons which share the same state
val component2 = div(getUnsafeCounterComponent, getUnsafeCounterComponent) // two counter buttons with separate state

// For the first one, you can share the state explicitly
val component = counter.map { counter => div(counter, counter) }
```

Why should we care? Because referential transparent functions can easily be refactored without affecting the meaning of the program and it is easier to reason about them.
If you do not care about this, you could use `Handler.unsafe` or create the `Handler` yourself with the streaming library of your choice.

#### Lifecycle Mangement

OutWatch automatically handles subscriptions of streams and observables in your components. You desribe your component with static and dynamic content (handlers, event emitters, `-->` and `<--`). When using these components, the needed subscriptions will be tight to the lifecycle of the respective dom elements and are managed for you. So whenever an element is mounted the subscriptions are run and whenever it is unmounted the subscriptions are killed.

If you ever need to manually subscribe to a stream, you can let OutWatch manage them for you:
```scala
div(
  managed(SyncIO { observable.subscribe(...) }) // this subscription is now bound to the lifetime of the outer div element
)
```

### Advanced

#### Accessing the DOM Element

Sometimes you need to access the underlying DOM element of a component. But a VNode in OutWatch is just a description of a dom element and we can create multiple different elements from one VNode. Therefore, there is no static element attached to a component. Though, you can get access to the dom element via hooks (callbacks):
```scala
div(
    onDomMount.foreach { element => // the element into which this node is mounted
        ???
    },
    onDomUnmount.foreach { element => // the element from which this node is unmounted
        ???
    }
)
```

We have a higher-level API to work with these kinds of callbacks, called `managedElement`, which can be used like this:
```scala
div(
    managedElement { element => // the element into which this node is mounted
        ??? // do something with the dom element
        cancelable(() => ???) // cleanup when the dom element is unmounted
    }
)
```

You can also get the current element when handling dom events, for example onClick:
```scala
div(
    onClick.asElement.foreach { element =>
        ???
    } // this is the same as onClick.map(_.currentTarget)
)
```

If the emitter does not emit events or elements, but you still want to access the current element, you can combine it with another emitter. For example:
```scala
div(
    emitter(someObservable).useLatestEmitter(onDomMount).foreach { element =>
        ???
    }
)
```

If you need an HTML or SVG Element instead of just an Element, you can do:
```scala
onDomMount.asHtml --> ???
onDomMount.asSvg --> ???
onClick.asHtml --> ???
onClick.asSvg --> ???
managedElement.asHtml { ??? }
managedElement.asSvg { ??? }
```

#### Using other streaming libraries

We use the library [`colibri`](https://github.com/cornerman/colibri) for a minimal reactive library and for typeclasses around streaming. These typeclasses like `Source` and `Sink` allow to integrate third-party libraries for streaming easily.

For using outwatch with monix:
```scala
import colibri.ext.monix._
import outwatch.reactive.handlers.monix._ // for handler factories
```

For using outwatch with scala.rx:
```scala
import colibri.ext.rx._
```

### Debugging

Source Code: [OutwatchTracing.scala](outwatch/src/main/scala/outwatch/helpers/OutwatchTracing.scala)

#### Tracing snabbdom patches

Show what patches snabbdom emits:
```scala
helpers.OutwatchTracing.patch.zipWithIndex.foreach { case (proxy, index) =>
  org.scalajs.dom.console.log(s"Snabbdom patch ($index)!", JSON.parse(JSON.stringify(proxy)), proxy)
}
```

#### Tracing exceptions in your components

Dynamic components with `Observables` can have errors. This is if `onError` is called on the underlying `Observer`. You can trace them in OutWatch with:
```scala
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
