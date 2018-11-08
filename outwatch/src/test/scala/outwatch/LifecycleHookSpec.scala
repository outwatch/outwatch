package outwatch

import cats.effect.IO
import monix.execution.Ack.Continue
import monix.reactive.Observable
import monix.reactive.subjects.PublishSubject
import org.scalajs.dom._
import outwatch.dom._
import outwatch.dom.dsl._

import scala.collection.mutable

class LifecycleHookSpec extends JSDomSpec {

  "Insertion hooks" should "be called correctly" in {

    var switch = false
    val sink = Sink.create{(_: Element) =>
      switch = true
      Continue
    }

    val node = div(onSnabbdomInsert --> sink)

    switch shouldBe false

    OutWatch.renderInto("#app", node).map { _ =>
      switch shouldBe true
    }
  }

  it should "be called correctly on merged nodes" in {
    var switch = false
    val sink = Sink.create{(_: Element) =>
      switch = true
      Continue
    }
    var switch2 = false
    val sink2 = Sink.create{(_: Element) =>
      switch2 = true
      Continue
    }

    val node = div(onSnabbdomInsert --> sink)(onSnabbdomInsert --> sink2)

    switch shouldBe false
    switch2 shouldBe false

    OutWatch.renderInto("#app", node).map { _ =>
      switch shouldBe true
      switch2 shouldBe true
    }
  }


  "Destruction hooks"  should "be called correctly" in {

    var switch = false
    val sink = Sink.create{(_: Element) =>
      switch = true
      Continue
    }

    val node = div(Observable(span(onSnabbdomDestroy --> sink), div("Hasdasd")))

    switch shouldBe false

    OutWatch.renderInto("#app", node).map { _ =>
      switch shouldBe true
    }
  }

  it should "be called correctly on merged nodes" in {

    var switch = false
    val sink = Sink.create{(_: Element) =>
      switch = true
      Continue
    }
    var switch2 = false
    val sink2 = Sink.create{(_: Element) =>
      switch2 = true
      Continue
    }

    val node = div(Observable(span(onSnabbdomDestroy --> sink)(onSnabbdomDestroy --> sink2), div("Hasdasd")))

    switch shouldBe false
    switch2 shouldBe false

    OutWatch.renderInto("#app", node).map { _ =>
      switch shouldBe true
      switch2 shouldBe true
    }
  }

  "Update hooks" should "be called correctly on merged nodes" in {
    var switch1 = false
    val sink1 = Sink.create{(_: (Element, Element)) =>
      switch1 = true
      Continue
    }
    var switch2 = false
    val sink2 = Sink.create{(_: (Element, Element)) =>
      switch2 = true
      Continue
    }

    val message = PublishSubject[String]
    val node = div(message, onSnabbdomUpdate --> sink1)(onSnabbdomUpdate --> sink2)

    OutWatch.renderInto("#app", node).map { _ =>

      switch1 shouldBe false
      switch2 shouldBe false

      message.onNext("wursi")
      switch1 shouldBe true
      switch2 shouldBe true
    }
  }


  it should "be called correctly" in {

    var switch = false
    val sink = Sink.create{(_: (Element, Element)) =>
      switch = true
      Continue
    }

    val node = div(Observable(span(onSnabbdomUpdate --> sink, "Hello"), span(onSnabbdomUpdate --> sink, "Hey")))

    switch shouldBe false

    OutWatch.renderInto("#app", node).map { _ =>
      switch shouldBe true
    }
  }

  "Prepatch hooks" should "be called" in {

    var switch = false
    val sink = Sink.create{(_: (Option[Element], Option[Element])) =>
      switch = true
      Continue
    }

    val prepatchNode = span(attributes.key := "1", onSnabbdomPrePatch --> sink, "Hey")
    val node = div(Observable(span("Hello")), Observable(prepatchNode, prepatchNode))

    switch shouldBe false

    OutWatch.renderInto("#app", node).map { _ =>
      switch shouldBe true
    }
  }

  it should "be called correctly on merged nodes" in {
    var switch1 = false
    val sink1 = Sink.create{(_: (Option[Element], Option[Element])) =>
      switch1 = true
      Continue
    }
    var switch2 = false
    val sink2 = Sink.create{(_: (Option[Element], Option[Element])) =>
      switch2 = true
      Continue
    }
    val message = PublishSubject[String]()
    val node = div(message, onSnabbdomPrePatch --> sink1)(onSnabbdomPrePatch --> sink2)

    OutWatch.renderInto("#app", node).map { _ =>
      switch1 shouldBe false
      switch2 shouldBe false

      message.onNext("wursi")

      switch1 shouldBe true
      switch2 shouldBe true
    }
  }

  "Postpatch hooks" should "be called" in {

    var switch = false
    val sink = Sink.create{(_: (Element, Element)) =>
      switch = true
      Continue
    }

    val node = div(Observable.pure("message"), onSnabbdomPostPatch --> sink, "Hey")

    switch shouldBe false

    OutWatch.renderInto("#app", node).map { _ =>
      switch shouldBe true
    }
  }


  it should "be called correctly on merged nodes" in {
    var switch1 = false
    val sink1 = Sink.create{(_: (Element, Element)) =>
      switch1 = true
      Continue
    }
    var switch2 = false
    val sink2 = Sink.create{(_: (Element, Element)) =>
      switch2 = true
      Continue
    }
    val message = PublishSubject[String]()
    val node = div(message, onSnabbdomPostPatch --> sink1)(onSnabbdomPostPatch --> sink2)

    OutWatch.renderInto("#app", node).map { _ =>
      switch1 shouldBe false
      switch2 shouldBe false

      message.onNext("wursi")

      switch1 shouldBe true
      switch2 shouldBe true
    }
  }


  "Hooks" should "be called in the correct order for modified node" in {
    val hooks = mutable.ArrayBuffer.empty[String]
    val insertSink = Sink.create { (_: Element) =>
      hooks += "insert"
      Continue
    }
    val prepatchSink = Sink.create { (_: (Option[Element], Option[Element])) =>
      hooks += "prepatch"
      Continue
    }
    val updateSink = Sink.create { (_: (Element, Element)) =>
      hooks += "update"
      Continue
    }
    val postpatchSink = Sink.create { (_: (Element, Element)) =>
      hooks += "postpatch"
      Continue

    }
    val destroySink = Sink.create { (_: Element) =>
      hooks += "destroy"
      Continue
    }

    val message = PublishSubject[String]()
    val node = div(message,
      onSnabbdomInsert --> insertSink,
      onSnabbdomPrePatch --> prepatchSink,
      onSnabbdomUpdate --> updateSink,
      onSnabbdomPostPatch --> postpatchSink,
      onSnabbdomDestroy --> destroySink
    )

    hooks shouldBe empty

    OutWatch.renderInto("#app", node).map { _ =>
      hooks.toList shouldBe List("insert")

      message.onNext("next")

      hooks.toList shouldBe List("insert", "prepatch", "update", "postpatch")
    }
  }

  "Empty single children receiver" should "not trigger node update on render" in {
    val hooks = mutable.ArrayBuffer.empty[String]
    val insertSink = Sink.create { (_: Element) =>
      hooks += "insert"
      Continue
    }
    val updateSink = Sink.create { (_: (Element, Element)) =>
      hooks += "update"
      Continue
    }

    val messageList = PublishSubject[Seq[String]]()
    val node = div("Hello", messageList.map(_.map(span(_))),
      onSnabbdomInsert --> insertSink,
      onSnabbdomUpdate --> updateSink
    )

    hooks shouldBe empty

    OutWatch.renderInto("#app", node).map { _ =>
      hooks.toList shouldBe  List("insert")
    }
  }

  "Static child nodes" should "not be destroyed and inserted when child stream emits" in {
    val hooks = mutable.ArrayBuffer.empty[String]
    val insertSink = Sink.create { (_: Element) =>
      hooks += "insert"
      Continue
    }
    val updateSink = Sink.create { (_: (Element, Element)) =>
      hooks += "update"
      Continue
    }
    val destroySink = Sink.create { (_: Element) =>
      hooks += "destroy"
      Continue
    }

    val message = PublishSubject[String]()
    val node = div(span("Hello", onSnabbdomInsert --> insertSink, onSnabbdomUpdate --> updateSink, onSnabbdomDestroy --> destroySink),
      message.map(span(_))
    )

    hooks shouldBe empty

    OutWatch.renderInto("#app", node).map { _ =>
      message.onNext("next")

      hooks.contains("destroy") shouldBe false
    }
  }

  it should "be only inserted once when children stream emits" in {
    val hooks = mutable.ArrayBuffer.empty[String]
    val insertSink = Sink.create { (_: Element) =>
      hooks += "insert"
      Continue
    }
    val updateSink = Sink.create { (_: (Element, Element)) =>
      hooks += "update"
      Continue
    }
    val destroySink = Sink.create { (_: Element) =>
      hooks += "destroy"
      Continue
    }

    val messageList = PublishSubject[Seq[String]]()
    val node = div(messageList.map(_.map(span(_))),
      span("Hello", onSnabbdomInsert --> insertSink, onSnabbdomUpdate --> updateSink, onSnabbdomDestroy --> destroySink)
    )

    hooks shouldBe empty

    OutWatch.renderInto("#app", node).map { _ =>
      messageList.onNext(Seq("one"))

      messageList.onNext(Seq("one", "two"))

      hooks.count(_ == "insert") shouldBe 1
    }
  }


  "Managed subscriptions" should "subscribe on insert and unsubscribe on destroy" in {

    val nodes = PublishSubject[VNode]

    var latest = ""
    val sink = Sink.create { (elem: String) =>
      latest = elem
      Continue
    }

    val sub = PublishSubject[String]

    val node = div(nodes.startWith(Seq(
      span(managed { () => sub subscribe sink })
    )))

    sub.onNext("pre")
    latest shouldBe ""

    OutWatch.renderInto("#app", node).map { _ =>
      sub.onNext("first")
      latest shouldBe "first"

      nodes.onNext(div()) // this triggers child destroy and subscription cancelation

      sub.onNext("second")
      latest shouldBe "first"
    }
  }

  it should "work with emitter(observable)" in {

    val nodes = PublishSubject[VNode]

    var latest = ""
    val sink = Sink.create { (elem: String) =>
      latest = elem
      Continue
    }

    val sub = PublishSubject[String]

    val node = div(nodes.startWith(Seq(
      span(emitter(sub) --> sink)
    )))

    sub.onNext("pre")
    latest shouldBe ""

    OutWatch.renderInto("#app", node).map { _ =>
      sub.onNext("first")
      latest shouldBe "first"

      nodes.onNext(div()) // this triggers child destroy and subscription cancelation

      sub.onNext("second")
      latest shouldBe "first"
    }
  }

  "DomHook" should "be called on static nodes" in {

    val modHandler = PublishSubject[VDomModifier]

    val node = div(modHandler)

    OutWatch.renderInto("#app", node).map { _ =>
      var domHooks = List.empty[String]

      modHandler.onNext(div(onDomMount foreach { domHooks :+= "mount" }, p(onDomUnmount foreach { domHooks :+= "unmount" })))
      domHooks shouldBe List("mount")

      modHandler.onNext(div("meh", p(onDomMount foreach { domHooks :+= "mount2" }), onDomPreUpdate foreach { domHooks :+= "preupdate2" }, onDomUpdate foreach { domHooks :+= "update2" }, onDomUnmount foreach { domHooks :+= "unmount2" }, Observable.now("distract")))
      domHooks shouldBe List("mount", "unmount", "mount2", "preupdate2", "update2")

      modHandler.onNext(span("muh", onDomMount foreach { domHooks :+= "mount3" }, onDomPreUpdate foreach { domHooks :+= "preupdate3" }, onDomUpdate foreach { domHooks :+= "update3" }, onDomUnmount foreach { domHooks :+= "unmount3" }))
      domHooks shouldBe List("mount", "unmount", "mount2", "preupdate2", "update2", "unmount2", "mount3")

      modHandler.onNext(VDomModifier.empty)
      domHooks shouldBe List("mount", "unmount", "mount2", "preupdate2", "update2", "unmount2", "mount3", "unmount3")
    }
  }

  it should "be called on nested streaming" in {

    val modHandler = PublishSubject[VDomModifier]()
    val innerHandler = PublishSubject[VDomModifier]()
    val node = div(modHandler)

    OutWatch.renderInto("#app", node).map { _ =>
      var domHooks = List.empty[String]

      modHandler.onNext(VDomModifier(innerHandler))
      domHooks shouldBe List()

      innerHandler.onNext("inner")
      domHooks shouldBe List()

      innerHandler.onNext(onDomMount foreach { domHooks :+= "inner-mount" })
      domHooks shouldBe List("inner-mount")

      innerHandler.onNext(onDomUnmount foreach { domHooks :+= "inner-unmount" })
      domHooks shouldBe List("inner-mount")

      innerHandler.onNext(onDomUnmount foreach { domHooks :+= "inner-unmount2" })
      domHooks shouldBe List("inner-mount", "inner-unmount")

      modHandler.onNext(VDomModifier.empty)
      domHooks shouldBe List("inner-mount", "inner-unmount", "inner-unmount2")
    }
  }

  it should "be called on streaming in and streaming out" in {

    val modHandler = PublishSubject[VDomModifier]()
    val otherHandler = PublishSubject[VDomModifier]()
    val innerHandler = PublishSubject[VDomModifier]()
    val node = div(modHandler, otherHandler)

    OutWatch.renderInto("#app", node).map { _ =>
      var domHooks = List.empty[String]

      modHandler.onNext(VDomModifier(onDomMount foreach { domHooks :+= "mount" }, onDomPreUpdate foreach { domHooks :+= "preupdate" }, onDomUpdate foreach { domHooks :+= "update" }, onDomUnmount foreach { domHooks :+= "unmount" }, innerHandler))
      domHooks shouldBe List("mount")

      otherHandler.onNext("other")
      domHooks shouldBe List("mount", "preupdate", "update")

      innerHandler.onNext("inner")
      domHooks shouldBe List("mount", "preupdate", "update", "preupdate", "update")

      innerHandler.onNext(VDomModifier(onDomMount foreach { domHooks :+= "inner-mount" }, onDomPreUpdate foreach { domHooks :+= "inner-preupdate" }, onDomUpdate foreach { domHooks :+= "inner-update" }, onDomUnmount foreach { domHooks :+= "inner-unmount" }, Observable.now("distract")))
      domHooks shouldBe List("mount", "preupdate", "update", "preupdate", "update", "preupdate", "update", "inner-mount", "preupdate", "inner-preupdate", "update", "inner-update")

      otherHandler.onNext(span("hi!"))
      domHooks shouldBe List("mount", "preupdate", "update", "preupdate", "update", "preupdate", "update", "inner-mount", "preupdate", "inner-preupdate", "update", "inner-update", "preupdate", "inner-preupdate", "update", "inner-update")

      innerHandler.onNext(VDomModifier(onDomPreUpdate foreach { domHooks :+= "inner-preupdate2" }, onDomUpdate foreach { domHooks :+= "inner-update2" }))
      domHooks shouldBe List("mount", "preupdate", "update", "preupdate", "update", "preupdate", "update", "inner-mount", "preupdate", "inner-preupdate", "update", "inner-update", "preupdate", "inner-preupdate", "update", "inner-update", "preupdate", "update", "inner-unmount")

      innerHandler.onNext(VDomModifier(Observable("inner")))
      domHooks shouldBe List("mount", "preupdate", "update", "preupdate", "update", "preupdate", "update", "inner-mount", "preupdate", "inner-preupdate", "update", "inner-update", "preupdate", "inner-preupdate", "update", "inner-update", "preupdate", "update", "inner-unmount", "preupdate", "update", "preupdate", "update")

      innerHandler.onNext(VDomModifier(onDomMount foreach { domHooks :+= "inner-mount2" }, onDomUnmount foreach { domHooks :+= "inner-unmount2" }, "something-else"))
      domHooks shouldBe List("mount", "preupdate", "update", "preupdate", "update", "preupdate", "update", "inner-mount", "preupdate", "inner-preupdate", "update", "inner-update", "preupdate", "inner-preupdate", "update", "inner-update", "preupdate", "update", "inner-unmount", "preupdate", "update", "preupdate", "update", "preupdate", "update", "inner-mount2")

      modHandler.onNext(onDomMount foreach { domHooks :+= "mount2" })
      domHooks shouldBe List("mount", "preupdate", "update", "preupdate", "update", "preupdate", "update", "inner-mount", "preupdate", "inner-preupdate", "update", "inner-update", "preupdate", "inner-preupdate", "update", "inner-update", "preupdate", "update", "inner-unmount", "preupdate", "update", "preupdate", "update", "preupdate", "update", "inner-mount2", "mount2", "unmount", "inner-unmount2")

      modHandler.onNext(onDomUnmount foreach { domHooks :+= "unmount2" })
      domHooks shouldBe List("mount", "preupdate", "update", "preupdate", "update", "preupdate", "update", "inner-mount", "preupdate", "inner-preupdate", "update", "inner-update", "preupdate", "inner-preupdate", "update", "inner-update", "preupdate", "update", "inner-unmount", "preupdate", "update", "preupdate", "update", "preupdate", "update", "inner-mount2", "mount2", "unmount", "inner-unmount2")

      modHandler.onNext(VDomModifier.empty)
      domHooks shouldBe List("mount", "preupdate", "update", "preupdate", "update", "preupdate", "update", "inner-mount", "preupdate", "inner-preupdate", "update", "inner-update", "preupdate", "inner-preupdate", "update", "inner-update", "preupdate", "update", "inner-unmount", "preupdate", "update", "preupdate", "update", "preupdate", "update", "inner-mount2", "mount2", "unmount", "inner-unmount2", "unmount2")
    }
  }

  it should "be called for default and streaming out" in {

    var domHooks = List.empty[String]

    val modHandler = PublishSubject[VDomModifier]()
    val innerHandler = PublishSubject[VDomModifier]()
    val otherHandler = PublishSubject[VDomModifier]()
    val node = div(otherHandler, ValueObservable.from(modHandler, VDomModifier(onDomMount foreach { domHooks :+= "default-mount" }, onDomPreUpdate foreach { domHooks :+= "default-preupdate" }, onDomUpdate foreach { domHooks :+= "default-update" }, onDomUnmount foreach { domHooks :+= "default-unmount" }, innerHandler)))

    OutWatch.renderInto("#app", node).map { _ =>
      domHooks shouldBe List("default-mount")

      innerHandler.onNext(VDomModifier(onDomMount foreach { domHooks :+= "inner-mount" }, onDomPreUpdate foreach { domHooks :+= "inner-preupdate" }, onDomUpdate foreach { domHooks :+= "inner-update" }, onDomUnmount foreach { domHooks :+= "inner-unmount" }))
      domHooks shouldBe List("default-mount", "default-preupdate", "default-update", "inner-mount")

      otherHandler.onNext(span("hi!"))
      domHooks shouldBe List("default-mount", "default-preupdate", "default-update", "inner-mount", "default-preupdate", "inner-preupdate", "default-update", "inner-update")

      modHandler.onNext(VDomModifier(onDomMount foreach { domHooks :+= "mount" }, onDomUnmount foreach { domHooks :+= "unmount" }))
      domHooks shouldBe List("default-mount", "default-preupdate", "default-update", "inner-mount", "default-preupdate", "inner-preupdate", "default-update", "inner-update", "mount", "default-unmount", "inner-unmount")

      modHandler.onNext(onDomMount foreach { domHooks :+= "mount2" })
      domHooks shouldBe List("default-mount", "default-preupdate", "default-update", "inner-mount", "default-preupdate", "inner-preupdate", "default-update", "inner-update", "mount", "default-unmount", "inner-unmount", "mount2", "unmount")

      modHandler.onNext(onDomUnmount foreach { domHooks :+= "unmount2" })
      domHooks shouldBe List("default-mount", "default-preupdate", "default-update", "inner-mount", "default-preupdate", "inner-preupdate", "default-update", "inner-update", "mount", "default-unmount", "inner-unmount", "mount2", "unmount")

      modHandler.onNext(VDomModifier.empty)
      domHooks shouldBe List("default-mount", "default-preupdate", "default-update", "inner-mount", "default-preupdate", "inner-preupdate", "default-update", "inner-update", "mount", "default-unmount", "inner-unmount", "mount2", "unmount", "unmount2")
    }
  }

  "Hooks" should "support emitter operations" in {

    val operations = mutable.ArrayBuffer.empty[String]

    val sink = Sink.create { (op: String) =>
      operations += op
      Continue
    }

    val divTagName = onSnabbdomInsert.map(_.tagName.toLowerCase).filter(_ == "div")

    val node = div(onSnabbdomInsert("insert") --> sink,
      div(divTagName --> sink),
      span(divTagName --> sink)
    )

    OutWatch.renderInto("#app", node).map { _ =>
      operations.toList shouldBe List("div", "insert")
    }
  }

  "Lifecycle and subscription" should "with a snabbdom key" in {
    var onSnabbdomInsertCount = 0
    var onSnabbdomPrePatchCount = 0
    var onSnabbdomPostPatchCount = 0
    var onSnabbdomUpdateCount = 0
    var onSnabbdomDestroyCount = 0
    var innerHandlerCount = 0
    var onDomMountList = List.empty[Int]
    var onDomUnmountList = List.empty[Int]
    var onDomUpdateList = List.empty[Int]

    val innerHandler = Handler.unsafe[Int]
    val handler = Handler.unsafe[(String, String)]
    val otherHandler = Handler.unsafe[String]

    var counter = 0
    val node = div(
      id := "strings",
      otherHandler.map(div(_)),
      handler.map { case (keyText, text) =>
        counter += 1
        val c = counter
        div(
          dsl.key := keyText,
          text,
          innerHandler.map { i => innerHandlerCount += 1; i },
          onSnabbdomInsert foreach { onSnabbdomInsertCount += 1 },
          onSnabbdomPrePatch foreach { onSnabbdomPrePatchCount += 1 },
          onSnabbdomPostPatch foreach { onSnabbdomPostPatchCount += 1 },
          onSnabbdomUpdate foreach { onSnabbdomUpdateCount += 1 },
          onSnabbdomDestroy foreach { onSnabbdomDestroyCount += 1 },
          onDomMount foreach { onDomMountList :+= c },
          onDomUnmount foreach { onDomUnmountList :+= c },
          onDomUpdate foreach { onDomUpdateList :+= c }
        )
      }
    )

    OutWatch.renderInto("#app", node).map { _ =>
      val element = document.getElementById("strings")

      element.innerHTML shouldBe ""
      onSnabbdomInsertCount shouldBe 0
      onSnabbdomPrePatchCount shouldBe 0
      onSnabbdomPostPatchCount shouldBe 0
      onSnabbdomUpdateCount shouldBe 0
      onSnabbdomDestroyCount shouldBe 0
      onDomMountList shouldBe Nil
      onDomUnmountList shouldBe Nil
      onDomUpdateList shouldBe Nil

      handler.onNext(("key", "heinz"))
      element.innerHTML shouldBe "<div>heinz</div>"
      onSnabbdomInsertCount shouldBe 1
      onSnabbdomPrePatchCount shouldBe 0
      onSnabbdomPostPatchCount shouldBe 0
      onSnabbdomUpdateCount shouldBe 0
      onSnabbdomDestroyCount shouldBe 0
      onDomMountList shouldBe List(1)
      onDomUnmountList shouldBe Nil
      onDomUpdateList shouldBe Nil

      handler.onNext(("key", "golum"))
      element.innerHTML shouldBe "<div>golum</div>"
      onSnabbdomInsertCount shouldBe 1
      onSnabbdomPrePatchCount shouldBe 1
      onSnabbdomPostPatchCount shouldBe 1
      onSnabbdomUpdateCount shouldBe 1
      onSnabbdomDestroyCount shouldBe 0
      onDomMountList shouldBe List(1, 2)
      onDomUnmountList shouldBe List(1)
      onDomUpdateList shouldBe Nil

      handler.onNext(("key", "dieter"))
      element.innerHTML shouldBe "<div>dieter</div>"
      onSnabbdomInsertCount shouldBe 1
      onSnabbdomPrePatchCount shouldBe 2
      onSnabbdomPostPatchCount shouldBe 2
      onSnabbdomUpdateCount shouldBe 2
      onSnabbdomDestroyCount shouldBe 0
      onDomMountList shouldBe List(1, 2, 3)
      onDomUnmountList shouldBe List(1,2)
      onDomUpdateList shouldBe Nil

      handler.onNext(("nope", "dieter"))
      element.innerHTML shouldBe "<div>dieter</div>"
      onSnabbdomInsertCount shouldBe 2
      onSnabbdomPrePatchCount shouldBe 2
      onSnabbdomPostPatchCount shouldBe 2
      onSnabbdomUpdateCount shouldBe 2
      onSnabbdomDestroyCount shouldBe 1
      onDomMountList shouldBe List(1, 2 ,3, 4)
      onDomUnmountList shouldBe List(1,2,3)
      onDomUpdateList shouldBe Nil

      handler.onNext(("yes", "peter"))
      element.innerHTML shouldBe "<div>peter</div>"
      onSnabbdomInsertCount shouldBe 3
      onSnabbdomPrePatchCount shouldBe 2
      onSnabbdomPostPatchCount shouldBe 2
      onSnabbdomUpdateCount shouldBe 2
      onSnabbdomDestroyCount shouldBe 2
      onDomMountList shouldBe List(1, 2, 3, 4, 5)
      onDomUnmountList shouldBe List(1, 2, 3, 4)
      onDomUpdateList shouldBe Nil

      otherHandler.onNext("hi")
      element.innerHTML shouldBe "<div>hi</div><div>peter</div>"
      onSnabbdomInsertCount shouldBe 3
      onSnabbdomPrePatchCount shouldBe 2
      onSnabbdomPostPatchCount shouldBe 2
      onSnabbdomUpdateCount shouldBe 2
      onSnabbdomDestroyCount shouldBe 2
      onDomMountList shouldBe List(1, 2, 3, 4, 5)
      onDomUnmountList shouldBe List(1, 2, 3, 4)
      onDomUpdateList shouldBe Nil

      innerHandlerCount shouldBe 0

      innerHandler.onNext(0)
      element.innerHTML shouldBe "<div>hi</div><div>peter0</div>"
      onSnabbdomInsertCount shouldBe 3
      onSnabbdomPrePatchCount shouldBe 3
      onSnabbdomPostPatchCount shouldBe 3
      onSnabbdomUpdateCount shouldBe 3
      onSnabbdomDestroyCount shouldBe 2
      onDomMountList shouldBe List(1, 2, 3, 4, 5)
      onDomUnmountList shouldBe List(1, 2, 3, 4)
      onDomUpdateList shouldBe List(5)

      innerHandlerCount shouldBe 1
    }
  }

  it should "without a custom key" in {
    var onSnabbdomInsertCount = 0
    var onSnabbdomPrePatchCount = 0
    var onSnabbdomPostPatchCount = 0
    var onSnabbdomUpdateCount = 0
    var onSnabbdomDestroyCount = 0
    var innerHandlerCount = 0
    var onDomMountList = List.empty[Int]
    var onDomUnmountList = List.empty[Int]
    var onDomUpdateList = List.empty[Int]

    val innerHandler = Handler.unsafe[Int]
    val handler = Handler.unsafe[String]
    val otherHandler = Handler.unsafe[String]

    var counter = 0
    val node = div(
      id := "strings",
      otherHandler.map(div(_)),
      handler.map { text =>
        counter += 1
        val c = counter
        div(
          text,
          innerHandler.map { i => innerHandlerCount += 1; i },
          onSnabbdomInsert foreach { onSnabbdomInsertCount += 1 },
          onSnabbdomPrePatch foreach { onSnabbdomPrePatchCount += 1 },
          onSnabbdomPostPatch foreach { onSnabbdomPostPatchCount += 1 },
          onSnabbdomUpdate foreach { onSnabbdomUpdateCount += 1 },
          onSnabbdomDestroy foreach { onSnabbdomDestroyCount += 1 },
          onDomMount foreach { onDomMountList :+= c },
          onDomUnmount foreach { onDomUnmountList :+= c },
          onDomUpdate foreach { onDomUpdateList :+= c }
        )
      }
    )

    OutWatch.renderInto("#app", node).map { _ =>
      val element = document.getElementById("strings")

      element.innerHTML shouldBe ""
      onSnabbdomInsertCount shouldBe 0
      onSnabbdomPrePatchCount shouldBe 0
      onSnabbdomPostPatchCount shouldBe 0
      onSnabbdomUpdateCount shouldBe 0
      onSnabbdomDestroyCount shouldBe 0
      onDomMountList shouldBe Nil
      onDomUnmountList shouldBe Nil
      onDomUpdateList shouldBe Nil

      handler.onNext("heinz")
      element.innerHTML shouldBe "<div>heinz</div>"
      onSnabbdomInsertCount shouldBe 1
      onSnabbdomPrePatchCount shouldBe 0
      onSnabbdomPostPatchCount shouldBe 0
      onSnabbdomUpdateCount shouldBe 0
      onSnabbdomDestroyCount shouldBe 0
      onDomMountList shouldBe List(1)
      onDomUnmountList shouldBe Nil
      onDomUpdateList shouldBe Nil

      handler.onNext("golum")
      element.innerHTML shouldBe "<div>golum</div>"
      onSnabbdomInsertCount shouldBe 1
      onSnabbdomPrePatchCount shouldBe 1
      onSnabbdomPostPatchCount shouldBe 1
      onSnabbdomUpdateCount shouldBe 1
      onSnabbdomDestroyCount shouldBe 0
      onDomMountList shouldBe List(1, 2)
      onDomUnmountList shouldBe List(1)
      onDomUpdateList shouldBe Nil

      handler.onNext("dieter")
      element.innerHTML shouldBe "<div>dieter</div>"
      onSnabbdomInsertCount shouldBe 1
      onSnabbdomPrePatchCount shouldBe 2
      onSnabbdomPostPatchCount shouldBe 2
      onSnabbdomUpdateCount shouldBe 2
      onSnabbdomDestroyCount shouldBe 0
      onDomMountList shouldBe List(1, 2, 3)
      onDomUnmountList shouldBe List(1,2)
      onDomUpdateList shouldBe Nil

      handler.onNext("dieter")
      element.innerHTML shouldBe "<div>dieter</div>"
      onSnabbdomInsertCount shouldBe 1
      onSnabbdomPrePatchCount shouldBe 3
      onSnabbdomPostPatchCount shouldBe 3
      onSnabbdomUpdateCount shouldBe 3
      onSnabbdomDestroyCount shouldBe 0
      onDomMountList shouldBe List(1, 2 ,3, 4)
      onDomUnmountList shouldBe List(1,2,3)
      onDomUpdateList shouldBe Nil

      handler.onNext("peter")
      element.innerHTML shouldBe "<div>peter</div>"
      onSnabbdomInsertCount shouldBe 1
      onSnabbdomPrePatchCount shouldBe 4
      onSnabbdomPostPatchCount shouldBe 4
      onSnabbdomUpdateCount shouldBe 4
      onSnabbdomDestroyCount shouldBe 0
      onDomMountList shouldBe List(1, 2, 3, 4, 5)
      onDomUnmountList shouldBe List(1, 2, 3, 4)
      onDomUpdateList shouldBe Nil

      otherHandler.onNext("hi")
      element.innerHTML shouldBe "<div>hi</div><div>peter</div>"
      onSnabbdomInsertCount shouldBe 1
      onSnabbdomPrePatchCount shouldBe 4
      onSnabbdomPostPatchCount shouldBe 4
      onSnabbdomUpdateCount shouldBe 4
      onSnabbdomDestroyCount shouldBe 0
      onDomMountList shouldBe List(1, 2, 3, 4, 5)
      onDomUnmountList shouldBe List(1, 2, 3, 4)
      onDomUpdateList shouldBe Nil

      innerHandlerCount shouldBe 0

      innerHandler.onNext(0)
      element.innerHTML shouldBe "<div>hi</div><div>peter0</div>"
      onSnabbdomInsertCount shouldBe 1
      onSnabbdomPrePatchCount shouldBe 5
      onSnabbdomPostPatchCount shouldBe 5
      onSnabbdomUpdateCount shouldBe 5
      onSnabbdomDestroyCount shouldBe 0
      onDomMountList shouldBe List(1, 2, 3, 4, 5)
      onDomUnmountList shouldBe List(1, 2, 3, 4)
      onDomUpdateList shouldBe List(5)

      innerHandlerCount shouldBe 1
    }
  }
}
