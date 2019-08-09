# Changelog

# Unpublished

...

### August 2019
* Refactor code to use cats effect typeclasses instead of directly committing to IO (#135)
* The default IO implementation is still available, users only need to add the imports:
```scala
import outwatch.dom.io._  // Handler, OutWatch.render*
import outwatch.util.io._ // Store
```

### Pre 2019
* Add `OutwatchTracing.error: Observable[Throwable]` to get notified about errors in your reactive components.

* Make `VDomModifier` non-side effecting by default. No dom-access or subscriptions happen when constructing VNodes. This means that a modifier does not need be wrapped in an `IO`. Instead `div` just returns a `VNode` and `href := "meh"` just returns an `Attribute`. If you apply side effects via a handler, or from your own code, you will have an `IO` around your node: `Handler.create("text").map(div(_)): IO[VNode]`. You can now transparently embed the `IO` into your main logic or unwrap the IO to use with `Outwatch.renderInto`. You can also use `IO[VDomModifier]` in your code as before: `div(ioNode: IO[VNode], ioMod: IO[VDomModifier])`. These `IO` modifiers are represented as an `EffectModifier`. The whole construction is still referentially transparent, just much simpler and less intrusive.
* Use our own fork of snabbdom. It fixes bugs that are still not merged in snabbdom and allows us to use it more efficiently. For example, we can reuse VNodeProxies to have a very intelligent way of caching VNodes in outwatch. That allows for mixing Observables without recalculating the whole subtree on every update. I think the goal needs to be to write our own library like snabbdom or something similar!
* Fix leaking subscriptions in emitter builders and monix ops
* Faster code with less memory allocations for constructing `VNodeProxies`, a lot of mutability but just internally :)
* Remove implicit scheduler from builder dsl, we have a `SchedulerAction` modifier now, which gets the scheduler injected from the outer `Outwatch.render` call. We delay subscriptiuons in a `SchedulerAction` until a node is going to be inserted and use the same scheduler for the whole ui of the app.
* Child commands to send an `Observable[ChildCommand]` that can incrementally update a list of nodes. Gives major improvments in speed as opposed to rerendering the whole list of nodes like a `Observable[Seq[VNode]]`.
* Add thunk nodes. This is also useful for performance reasons, but you can keep writing your components as before with `Observable[Seq[VNode]]`. You can define a thunk node like this: `div.thunk("unique-component-name")(arg1, arg2)(VDomModifier(arg1, arg2, ...))`. Here `arg1` and `arg2` are the only dependencies of this `div`, so the `div` only needs to be rerendered if one of these dependencies changes. So thunk delays the execution of the `VDomModifier` in the last parameter list (call-by-name) and only computes a new node if one of the arguments has changed. If the arguments stay the same, it can just reuse the node from the previous run (identified by the "unique-component-name"). For perfomance this comes close to `ChildCommands`. It works similar to a React component with a `shouldRender` function, which compares the previous and the next state.
* Sync emitters so that `stopPropagation` / `preventDefault` actually work and are not behind an async boundary. Only async if needed.
* Add `mapTo` to emitter builder to get the value to be mapped by-name.
* Add `async` to enforce an async boundary in the emitter `Observable`.
* Add `useLatest` to emitter builder for combining two emitter builders: e.g. `onClick.useLatest(onDomMount.asHtml).foreach { elem => elem.blur() }
* Add `foreach` to emitter builder. `foreach` fits naturally with methods like `map`, `collect`, etc and gives a way to interact nicely with the dom: `onDomMount.asHtml.foreach { elem => elem.focus }`. The side effect factories are deprecated.
* Userful emitter builder factories: `EmitterBuilder.fromObservable(Observable)` or `emitter(Observable)` and `EmitterBuilder.ofModifier`.
* Less function allocations when handling hooks and emitters.
* Do not return `IO` in `Sink.create`. The sink itself is not a side effect. If there is a side effect and therefore a reason for `IO`, then it lies in the function. Now then the function should be in an `IO` beforehand. So, you will use it this way anyhow: `statefulAction.map(Sink.create _)`.
* Add DomPreUpdate hook: `onDomPreUpdate`
* Add Init hook, which you can use via `InitHook(f: VNodeProxy => Unit)`. Be aware, the element of the `VNodeProxy` will always be undefined in the `init` callback. This is why there is no `onSnabbdomInit: EmitterBuilder[dom.Element, VDomModifier]`.
* Add `Handler.unsafe` method to get a handler without an `IO`. This may be needed for global state and is easier to write, but shares the logic with `Handler.create`
* Do not enforce keys in Outwatch, we are able to track the identity of `VNode`s via their `_id` and do not need to enforce a key for streaming nodes. Letting the user do this on demand is much better and allows for improvements (that are not broken by automatic key generation in Outwatch).
* Add `managedAction`, `managedElement`, `managedElement.asHtml`, `managedElement.asSvg` as helper for binding things to the lifetime of an element. Allows easy integration of third-party libraries
* Remove syntax suger `=?`
* Add a minimalistic performance test to get an idea of the figures.
* Remove `dsl.attributes.lifecycle._`, you can use `dsl.attributes.outwatch._` already.
* More inlining of simple redirect functions. Specialized versions of functions for `VDomModifier` to not require a `AsVDomModifier`.
* Fix streaming of dom-mount-hooks and managed subscriptions. Now you can have `Observable(managed(subscription), onDomMount --> sink)` and it will work intuitively and trigger when streamed in and out accordingly.
* Update monix to experimental RC2 and cats-effect to 1.0 
* Do not use `h`-function of snabbdom. It is slow because it does a lot of checks that we do not need and it reiterates all svg children which we also do not need because we have this information at construction time from dom-types. Therefore we just construct a new `VNodeProxy` instead of calling the h-function.
* Add VNode.prepend for prepending modifiers to a Modifier, this is useful for providing defaults to a VNode from the call-side. 
* Add `HtmlVNode` and `SvgVNode` to differentiate between html and svg nodes.

* Make Handler.create return a BehaviourSubject if there is a seed, otherwise create a ReplaceSubject.createLimited(1).

* Introduce proper lifecycle hooks: `onDomMount` (called when a VNode is mounted into an element), `onDomUnmount` (called when a VNode is unmounted from an element), `onDomUpdate` (called when a VNode was updated in the same element). With only using snabbdom lifecycle hooks, we get incorrect behavior: onInsert and onDestroy are not sufficient to know when a node is inserted into the dom or removed from the dom. You also need to check onPostPatch and check whether you are patching against an older version of the same node or against a totally different node. For outwatch, this lead to leaking subscriptions because only onDestroy is handled, but a node could be removed via onPostPatch as well. The same was true for the managed subscription. Therefore, we added the new lifecycle events. They are built by putting a state into the VNodeProxy which can be accessed in onPostPatch hooks. So now, subscriptions do not leak anymore and application have proper hooks for accessing the dom element only when necessary. The snabbdom events were renamed to onSnabbdomInsert, onSnabbdomDestroy, etc. Old names were deprecated with a hint on using the new hooks.

* Introduce AsValueObservable and AsObserver typeclasses for convenience. This allows to hook custom streaming libraries into Outwatch. You can use any type `Stream[_]` as an observable when you define an Instance of `AsValueObservable[Stream]` and use it as a sink when you define an instance of `AsObserver[Stream]`. The ValueObservable class allows to provide an optional initial value, if your stream implementation can access that synchronously

* Fix leaking subscriptions when using custom snabbdom keys.

* Fix leaking managed subscriptions when nodes are patched.

* Faster patching and updating of streamed content. We now cache VNodes so they are not recalculated when they did not change. We fixed patches using outdated data. Furthermore, separation of VDomModifiers was made more efficient.

* Update snabbdom to version 0.7.2.

* Remove implicit scheduler from `Handler.create` methods.

* Add `AsVDomModifier` instances for `Long` and `Boolean`.

* Add `CustomEmitterBuilder` for returning an effectful action wrapped in `IO`.

* Add possibility to trace what is patched with snabbdom. You can get an `Observable[(VNodeProxy, VNodeProxy)]` for tracing the old and new proxies: `OutwatchTracing.patch`.

* Use monix types `Observable` and `Observer` instead of having our own type `Handler`. `Handler[T]` is now only a type alias for `Observable[T] with Observer[T]` and `ProHandler[I,O]` is an alias for `Observable[O] with Observer[I]`.

* Add event actions to EmitterBuilder, you can now do: `onClick.stopPropagation --> someSink` or `onFocus.preventDefault --> someSink`.

* More flexible streaming of `VDomModifier`. You can now stream any modifier.

* Support SVG elements.

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
