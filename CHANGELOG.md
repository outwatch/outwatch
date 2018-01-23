# Changelog

# Unpublished

...
* `1051c3c` `2018-01-21` Add RC to readme 
* `fc71430` `2018-01-17` Handle all VDomModifier implicit conversions though the AsVDomModifier type class 
* `cc6bd29` `2018-01-17` Simplify node version declaration in travis 
* `1e15dc1` `2018-01-16` refactor code for storage handler factories 
* `ffc5813` `2018-01-16` add additional storage handler without storage events 
* `eb05898` `2018-01-16` Added return type and removed some warnings 
* `37840bd` `2018-01-16` Implement LocalStorage Handler which reacts on StorageEvent 
* `0546f88` `2018-01-17` Implement typed Elements (.asHtml, .asSvg) on Hooks 

* The Storage Handler reacts on change events from other tabs/windows, can be used with Local- and SessionStorage and reads and writes Options ([LocalStorage.scala](https://github.com/OutWatch/outwatch/blob/master/src/main/scala/outwatch/util/LocalStorage.scala)):
  ```scala
  val key = "banana"
  val storageHandler = util.LocalStorage.handler(key).unsafeRunSync()
  storageHandler.unsafeOnNext(Some("joe"))
  ```
  
* There are helpers to cast the Event types from Hooks ([EmitterOps.scala](https://github.com/OutWatch/outwatch/blob/master/src/main/scala/outwatch/dom/helpers/EmitterOps.scala)):
  ```scala
  textArea(
    onInsert.asSVG --> SVGElementHandler
  )
  ```
  

# 1.0.0-RC2

* To use tags, attributes in scope you need to import the DSL:
  ```scala
  import outwatch.dom._
  import outwatch.dom.dsl._
  ```
  
* Outwatch now takes all its tags, attributes and styles from [scala-dom-types](https://github.com/raquo/scala-dom-types) ([DomTypes.scala](https://github.com/OutWatch/outwatch/blob/6dd72a31cb48bd67547a08482209f0a2480b0e9c/src/main/scala/outwatch/dom/DomTypes.scala)) - Thanks @raquo! Therefore we have a more complete collection of tags/attributes and it is now possible to use style-sheets in outwatch, like you may know from [scalatags](https://github.com/lihaoyi/scalatags).
  ```scala
  div(border := "1px solid black")
  ```
* Custom tags, attribues, properties and styles can also be used ([OutwatchAttributes.scala](https://github.com/OutWatch/outwatch/blob/6dd72a31cb48bd67547a08482209f0a2480b0e9c/src/main/scala/outwatch/dom/OutwatchAttributes.scala#L63)):
  ```scala
  div(
      tag("svg")(
        tag("svg:path")(
          attr("d") := "M 0,-3 L 10,-0.5 L 10,0.5 L0,3",
          style("fill") := "#666"
        )
      )
  )
  ```
  (SVG support doesn't exist in scala-dom-types yet: https://github.com/raquo/scala-dom-types/issues/20)


* Referential transparent API using [cats-effect](https://github.com/typelevel/cats-effect):
    * `Handler.create[T]()` and `Sink.create[T]()` now return `IO[Handler[T]]` and `IO[Sink[T]]`.
    ```scala
    OutWatch.renderReplace("#container", vNode).unsafeRunSync()
    ```
    @LukaJCB, @mariusmuja please describe a bit more...

* Outwatch now exclusively uses [Monix](https://monix.io) instead of [rxscala-js](https://github.com/LukaJCB/rxscala-js). Be sure to provide a [Scheduler](https://monix.io/docs/2x/execution/scheduler.html).

* Introducing managed subscriptions. When subscribing to Observables in your code outside of dom-elements, you have to handle the lifetime of subscriptions yourself. With `managed` subscriptions, you can instead bind it to the lifetime of a dom-element:
  ```scala
    val handler: Handler[Int] = ???
    val sink: Sink[Int] = ???
    div( managed(sink <-- handler) )
  ```

  or:

  ```scala
    val observable: Observable[Int] = ???
    div( managed(IO(observable.foreach(i => println(s"debug: $i")))) )
  ```

  In both cases, outwatch will only run the `IO[Subscription]` when the element is rendered in the dom and will unsubscribe when the element is removed from the dom.

* `Outwatch.render*` is now explicit whether it replaces or inserts into the given dom element ([OutWatch.scala](https://github.com/OutWatch/outwatch/blob/master/src/main/scala/outwatch/dom/OutWatch.scala)):
  ```scala
  val vNode = div()
  val node = document.querySelector("#container")
  OutWatch.renderReplace(node, vNode).unsafeRunSync()
  OutWatch.renderReplace("#container", vNode).unsafeRunSync()
  OutWatch.renderInto(node, vNode).unsafeRunSync()
  OutWatch.renderInto("#container", vNode).unsafeRunSync()
  ```

* Handlers can be transformed using new methods including isomorphisms and lenses ([package.scala](https://github.com/OutWatch/outwatch/blob/master/src/main/scala/outwatch/package.scala)):
  ```scala
  val handler = Handler.create[Int].unsafeRunSync()
  val mapped = handler.imap[Int](_ - 1)(_ + 1)

  handler.unsafeOnNext(12)
  // mapped will be triggered with 13

  mapped.unsafeOnNext(15)
  // handler will be triggered with 16


  val handler = Handler.create[(String, Int)].unsafeRunSync()
  val zoomed = handler.lens[Int](("x", 0))(_._2)((tuple, num) => (tuple._1, num))

  handler.unsafeOnNext(("y", 1))
  // zoomed will be triggered with 1

  zoomed.unsafeOnNext(2)
  // handler will be triggered with ("y", 2)
  ```
  @mariusmuja: please explain Pipes

* You can now manually write values into Sinks:
  ```scala
  val handler = Handler.create[Int].unsafeRunSync()
  handler.unsafeOnNext(5)
  ```

* You can call apply on already created tags to add additional content (also inspired by [scalatags](https://github.com/lihaoyi/scalatags)):
  ```scala
  val hello = div("Hello World")
  val helloInBlue = hello(color := "blue")
  ```

* Event handlers and life cycle hooks now have an `on`-prefix like `onClick` and `onInsert`.

* Event handlers and life cycle hooks can now be consistently transformed ([EmitterBuilder.scal](https://github.com/OutWatch/outwatch/blob/master/src/main/scala/outwatch/dom/helpers/EmitterBuilder.scala#L10)):
  ```scala
  div(
    onClick(true) --> sink,
    onUpdate.map(_._2) --> eventSink,
    onKeyDown.collect { case e if e.keyCode == KeyCode.Enter && !e.shiftKey => e.preventDefault(); e }.value.filter(_.nonEmpty) --> userInputHandler
  )
  ```
  
* And there are helpers to cast the Event types from Emitters ([EmitterOps.scala](https://github.com/OutWatch/outwatch/blob/master/src/main/scala/outwatch/dom/helpers/EmitterOps.scala)):
  ```scala
  textArea(
    onInput.value --> stringHandler
  )
  ```

* There is a helper called `sideEffect` which constructs a `Sink` for in-place side-effect usage ([SideEffects.scala](https://github.com/OutWatch/outwatch/blob/master/src/main/scala/outwatch/SideEffects.scala)):
  ```scala
  div(
    onClick --> sideEffect( e => console.log("your click event: ", e) ),
    onClick --> sideEffect{ println("you clicked!") }
  )
  ```

* Additional [life cycle hooks from Snabbdom](https://github.com/snabbdom/snabbdom#hooks) have been added ([VDomModifier.scala](https://github.com/OutWatch/outwatch/blob/master/src/main/scala/outwatch/dom/VDomModifier.scala#L131):) 
  ```scala
  textArea(
    onPostPatch --> sideEffect(_.asInstanceOf[dom.Element].focus())
  )
  ```
  
* Global events can be used via provided observables:
  ```scala
  events.window.onResize.foreach(_ => println("window resized"))
  ```

* Boolean and enumerated attributes are now properly handled. Quoting from the [HTML5 spec](https://www.w3.org/TR/html5/infrastructure.html#boolean-attributes): "A number of attributes are boolean attributes. The presence of a boolean attribute on an element represents the true value, and the absence of the attribute represents the false value." Previously, attributes like `draggable` or `disabled` were wrongly rendered. Using scala-dom-types and [a new release of snabbdom](https://github.com/snabbdom/snabbdom/releases/tag/v0.7.0), we now adhere to the HTML spec.

* It is possible to group modifiers with `CompositeModifier`:
  ```scala
  div(
    CompositeModifier(
      Seq(
        div(),
        color := "blue"
      )
    )
  )
  ```
  @mariusmuja, please describe more

* Extended styles for easy and composable CSS animations, example @mariusmuja

* Accumulated Attributes @mariusmuja

* Source maps should now point to the correct commit and file on github ([build.sbt](https://github.com/OutWatch/outwatch/blob/master/build.sbt#L30)).


# 0.11
