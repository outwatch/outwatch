package outwatch

import cats.effect.IO
import org.scalajs.dom._

import outwatch.dsl._

import colibri._

import scala.collection.mutable

class LifecycleHookSpec extends JSDomAsyncSpec {

  "Insertion hooks" should "be called correctly" in {

    var switch = false
    val observer = Observer.create{(_: Element) =>
      switch = true
    }

    val node = div(dsl.key := "unique", onSnabbdomInsert --> observer)

    switch shouldBe false

    OutWatch.renderInto[IO]("#app", node).map { _ =>
      switch shouldBe true
    }
  }

  it should "be called correctly on merged nodes" in {
    var switch = false
    val observer = Observer.create{(_: Element) =>
      switch = true
    }
    var switch2 = false
    val observer2 = Observer.create{(_: Element) =>
      switch2 = true
    }

    val node = div(dsl.key := "unique", onSnabbdomInsert --> observer)(onSnabbdomInsert --> observer2)

    switch shouldBe false
    switch2 shouldBe false

    OutWatch.renderInto[IO]("#app", node).map { _ =>
      switch shouldBe true
      switch2 shouldBe true
    }
  }


  "Destruction hooks"  should "be called correctly" in {

    var switch = false
    val observer = Observer.create{(_: Element) =>
      switch = true
    }

    val innerHandler = Subject.publish[VDomModifier]()
    val node = div(innerHandler.prepend(span(onSnabbdomDestroy --> observer)))

    switch shouldBe false

    OutWatch.renderInto[IO]("#app", node).map { _ =>
      switch shouldBe false

      innerHandler.unsafeOnNext(div("Hasdasd"))

      switch shouldBe true
    }
  }

  it should "be called correctly on merged nodes" in {

    var switch = false
    val observer = Observer.create{(_: Element) =>
      switch = true
    }
    var switch2 = false
    val observer2 = Observer.create{(_: Element) =>
      switch2 = true
    }

    val innerHandler = Subject.publish[VDomModifier]()
    val node = div(innerHandler.prepend(span(onSnabbdomDestroy --> observer)(onSnabbdomDestroy --> observer2)))

    switch shouldBe false
    switch2 shouldBe false

    OutWatch.renderInto[IO]("#app", node).map { _ =>
      switch shouldBe false
      switch2 shouldBe false

      innerHandler.unsafeOnNext(div("Hasdasd"))

      switch shouldBe true
      switch2 shouldBe true
    }
  }

  "Update hooks" should "be called correctly on merged nodes" in {
    var switch1 = false
    val observer1 = Observer.create{(_: (Element, Element)) =>
      switch1 = true
    }
    var switch2 = false
    val observer2 = Observer.create{(_: (Element, Element)) =>
      switch2 = true
    }

    val message = Subject.publish[String]()
    val node = div(message, dsl.key := "unique", onSnabbdomUpdate --> observer1)(onSnabbdomUpdate --> observer2)

    OutWatch.renderInto[IO]("#app", node).map { _ =>

      switch1 shouldBe false
      switch2 shouldBe false

      message.unsafeOnNext("wursi")
      switch1 shouldBe true
      switch2 shouldBe true
    }
  }


  it should "be called correctly" in {

    var switch = false
    val observer = Observer.create{(_: (Element, Element)) =>
      switch = true
    }

    val innerHandler = Subject.publish[VDomModifier]()
    val node = div(innerHandler.prepend(span(onSnabbdomUpdate --> observer, "Hello")))

    switch shouldBe false

    OutWatch.renderInto[IO]("#app", node).map { _ =>
      switch shouldBe false

      innerHandler.unsafeOnNext(span(onSnabbdomUpdate --> observer, "Hey"))

      switch shouldBe true
    }
  }

  "Prepatch hooks" should "be called" in {

    var switch = false
    val observer = Observer.create{(_: (Option[Element], Option[Element])) =>
      switch = true
    }

    val prepatchNode = span(attributes.key := "1", onSnabbdomPrePatch --> observer, "Hey", Observable("distract-sync"))
    val handler = Subject.behavior[VDomModifier](prepatchNode)
    val node = div(Observable(span("Hello")), handler)

    switch shouldBe false

    OutWatch.renderInto[IO]("#app", node).map { _ =>
      switch shouldBe false

      handler.unsafeOnNext(prepatchNode)

      switch shouldBe true
    }
  }

  it should "be called correctly on merged nodes" in {
    var switch1 = false
    val observer1 = Observer.create{(_: (Option[Element], Option[Element])) =>
      switch1 = true
    }
    var switch2 = false
    val observer2 = Observer.create{(_: (Option[Element], Option[Element])) =>
      switch2 = true
    }

    val message = Subject.publish[String]()
    val node = div(message, dsl.key := "unique", onSnabbdomPrePatch --> observer1)(onSnabbdomPrePatch --> observer2)

    OutWatch.renderInto[IO]("#app", node).map { _ =>
      switch1 shouldBe false
      switch2 shouldBe false

      message.unsafeOnNext("wursi")

      switch1 shouldBe true
      switch2 shouldBe true
    }
  }

  "Postpatch hooks" should "be called" in {

    var switch = false
    val observer = Observer.create{(_: (Element, Element)) =>
      switch = true
    }

    val message = Subject.publish[String]()
    val node = div(message, dsl.key := "unique", onSnabbdomPostPatch --> observer, "Hey")

    switch shouldBe false

    OutWatch.renderInto[IO]("#app", node).map { _ =>
      switch shouldBe false

      message.unsafeOnNext("hallo")

      switch shouldBe true
    }
  }


  it should "be called correctly on merged nodes" in {
    var switch1 = false
    val observer1 = Observer.create{(_: (Element, Element)) =>
      switch1 = true
    }
    var switch2 = false
    val observer2 = Observer.create{(_: (Element, Element)) =>
      switch2 = true
    }
    val message = Subject.publish[String]()
    val node = div(message, dsl.key := "unique", onSnabbdomPostPatch --> observer1)(onSnabbdomPostPatch --> observer2)

    OutWatch.renderInto[IO]("#app", node).map { _ =>
      switch1 shouldBe false
      switch2 shouldBe false

      message.unsafeOnNext("wursi")

      switch1 shouldBe true
      switch2 shouldBe true
    }
  }


  "Hooks" should "be called in the correct order for modified node" in {
    val hooks = mutable.ArrayBuffer.empty[String]
    val insertObs = Observer.create { (_: Element) =>
      hooks += "insert"
    }
    val prepatchObs = Observer.create { (_: (Option[Element], Option[Element])) =>
      hooks += "prepatch"
    }
    val updateObs = Observer.create { (_: (Element, Element)) =>
      hooks += "update"
    }
    val postpatchObs = Observer.create { (_: (Element, Element)) =>
      hooks += "postpatch"

    }
    val destroyObs = Observer.create { (_: Element) =>
      hooks += "destroy"
    }

    val message = Subject.publish[String]()
    val node = div(
      dsl.key := "unique",
      message,
      onSnabbdomInsert --> insertObs,
      onSnabbdomPrePatch --> prepatchObs,
      onSnabbdomUpdate --> updateObs,
      onSnabbdomPostPatch --> postpatchObs,
      onSnabbdomDestroy --> destroyObs
    )

    hooks shouldBe empty

    OutWatch.renderInto[IO]("#app", node).map { _ =>
      hooks.toList shouldBe List("insert")

      message.unsafeOnNext("next")

      hooks.toList shouldBe List("insert", "prepatch", "update", "postpatch")
    }
  }

  "Empty single children receiver" should "not trigger node update on render" in {
    val hooks = mutable.ArrayBuffer.empty[String]
    val insertObs = Observer.create { (_: Element) =>
      hooks += "insert"
    }
    val updateObs = Observer.create { (_: (Element, Element)) =>
      hooks += "update"
    }

    val messageList = Subject.publish[Seq[String]]()
    val node = div(dsl.key := "unique", "Hello", messageList.map(_.map(span(_))),
      onSnabbdomInsert --> insertObs,
      onSnabbdomUpdate --> updateObs
    )

    hooks shouldBe empty

    OutWatch.renderInto[IO]("#app", node).map { _ =>
      hooks.toList shouldBe  List("insert")
    }
  }

  "Static child nodes" should "not be destroyed and inserted when child stream emits" in {
    val hooks = mutable.ArrayBuffer.empty[String]
    val insertObs = Observer.create { (_: Element) =>
      hooks += "insert"
    }
    val updateObs = Observer.create { (_: (Element, Element)) =>
      hooks += "update"
    }
    val destroyObs = Observer.create { (_: Element) =>
      hooks += "destroy"
    }

    val message = Subject.publish[String]()
    val node = div(span("Hello", onSnabbdomInsert --> insertObs, onSnabbdomUpdate --> updateObs, onSnabbdomDestroy --> destroyObs),
      message.map(span(_))
    )

    hooks shouldBe empty

    OutWatch.renderInto[IO]("#app", node).map { _ =>
      message.unsafeOnNext("next")

      hooks.contains("destroy") shouldBe false
    }
  }

  it should "be only inserted once when children stream emits" in {
    val hooks = mutable.ArrayBuffer.empty[String]
    val insertObs = Observer.create { (_: Element) =>
      hooks += "insert"
    }
    val updateObs = Observer.create { (_: (Element, Element)) =>
      hooks += "update"
    }
    val destroyObs = Observer.create { (_: Element) =>
      hooks += "destroy"
    }

    val messageList = Subject.publish[Seq[String]]()
    val node = div(messageList.map(_.map(span(_))),
      span("Hello", onSnabbdomInsert --> insertObs, onSnabbdomUpdate --> updateObs, onSnabbdomDestroy --> destroyObs)
    )

    hooks shouldBe empty

    OutWatch.renderInto[IO]("#app", node).map { _ =>
      messageList.unsafeOnNext(Seq("one"))

      messageList.unsafeOnNext(Seq("one", "two"))

      hooks.count(_ == "insert") shouldBe 1
    }
  }


  "Managed subscriptions" should "subscribe on insert and unsubscribe on destroy" in {

    val nodes = Subject.publish[VNode]()

    var latest = ""
    val observer = Observer.create { (elem: String) =>
      latest = elem
    }

    val sub = Subject.publish[String]()

    val node = div(nodes.startWith(Seq(
      span(managedDelay(sub.unsafeSubscribe(observer)))
    )))

    sub.unsafeOnNext("pre")
    latest shouldBe ""

    OutWatch.renderInto[IO]("#app", node).map { _ =>
      sub.unsafeOnNext("first")
      latest shouldBe "first"

      nodes.unsafeOnNext(div()) // this triggers child destroy and subscription cancelation

      sub.unsafeOnNext("second")
      latest shouldBe "first"
    }
  }

  it should "subscribe on insert and unsubscribe on destroy 2" in {

    val nodes = Subject.publish[VNode]()

    var latest = ""
    val observer = Observer.create { (elem: String) =>
      latest = elem
    }

    val sub = Subject.publish[String]()

    val node = div(nodes.startWith(Seq(
      span(managedSubscribe(sub.to(observer)))
    )))

    sub.unsafeOnNext("pre")
    latest shouldBe ""

    OutWatch.renderInto[IO]("#app", node).map { _ =>
      sub.unsafeOnNext("first")
      latest shouldBe "first"

      nodes.unsafeOnNext(div()) // this triggers child destroy and subscription cancelation

      sub.unsafeOnNext("second")
      latest shouldBe "first"
    }
  }

  it should "work with emitter(observable)" in {

    val nodes = Subject.publish[VNode]()

    var latest = ""
    val observer = Observer.create { (elem: String) =>
      latest = elem
    }

    val sub = Subject.publish[String]()

    val node = div(nodes.startWith(Seq(
      span(emitter(sub) --> observer)
    )))

    sub.unsafeOnNext("pre")
    latest shouldBe ""

    OutWatch.renderInto[IO]("#app", node).map { _ =>
      sub.unsafeOnNext("first")
      latest shouldBe "first"

      nodes.unsafeOnNext(div()) // this triggers child destroy and subscription cancelation

      sub.unsafeOnNext("second")
      latest shouldBe "first"
    }
  }

  "DomHook" should "be called on static nodes" in {

    val modHandler = Subject.publish[VDomModifier]()

    val node = div(modHandler)

    OutWatch.renderInto[IO]("#app", node).map { _ =>
      var domHooks = List.empty[String]

      modHandler.unsafeOnNext(div(onDomMount doAction { domHooks :+= "mount" }, p(onDomUnmount doAction { domHooks :+= "unmount" })))
      domHooks shouldBe List("mount")

      val innerHandler = Subject.publish[String]()
      modHandler.unsafeOnNext(div("meh", p(onDomMount doAction { domHooks :+= "mount2" }), onDomPreUpdate doAction { domHooks :+= "preupdate2" }, onDomUpdate doAction { domHooks :+= "update2" }, onDomUnmount doAction { domHooks :+= "unmount2" }, innerHandler, Observable("distract-sync")))
      domHooks shouldBe List("mount", "unmount", "mount2")

      innerHandler.unsafeOnNext("distract")
      domHooks shouldBe List("mount", "unmount", "mount2", "preupdate2", "update2")

      modHandler.unsafeOnNext(span("muh", onDomMount doAction { domHooks :+= "mount3" }, onDomPreUpdate doAction { domHooks :+= "preupdate3" }, onDomUpdate doAction { domHooks :+= "update3" }, onDomUnmount doAction { domHooks :+= "unmount3" }))
      domHooks shouldBe List("mount", "unmount", "mount2", "preupdate2", "update2", "unmount2", "mount3")

      modHandler.unsafeOnNext(VDomModifier.empty)
      domHooks shouldBe List("mount", "unmount", "mount2", "preupdate2", "update2", "unmount2", "mount3", "unmount3")
    }
  }

  it should "be called on nested streaming" in {

    val modHandler = Subject.publish[VDomModifier]()
    val innerHandler = Subject.publish[VDomModifier]()
    val node = div(modHandler)

    OutWatch.renderInto[IO]("#app", node).map { _ =>
      var domHooks = List.empty[String]

      modHandler.unsafeOnNext(VDomModifier(innerHandler))
      domHooks shouldBe List()

      innerHandler.unsafeOnNext("inner")
      domHooks shouldBe List()

      innerHandler.unsafeOnNext(onDomMount doAction { domHooks :+= "inner-mount" })
      domHooks shouldBe List("inner-mount")

      innerHandler.unsafeOnNext(onDomUnmount doAction { domHooks :+= "inner-unmount" })
      domHooks shouldBe List("inner-mount")

      innerHandler.unsafeOnNext(onDomUnmount doAction { domHooks :+= "inner-unmount2" })
      domHooks shouldBe List("inner-mount", "inner-unmount")

      modHandler.unsafeOnNext(VDomModifier.empty)
      domHooks shouldBe List("inner-mount", "inner-unmount", "inner-unmount2")
    }
  }

  it should "be called on streaming in and streaming out" in {

    val modHandler = Subject.publish[VDomModifier]()
    val otherHandler = Subject.publish[VDomModifier]()
    val innerHandler = Subject.publish[VDomModifier]()
    val node = div(modHandler, otherHandler)

    OutWatch.renderInto[IO]("#app", node).map { _ =>
      var domHooks = List.empty[String]

      modHandler.unsafeOnNext(VDomModifier(onDomMount doAction { domHooks :+= "mount" }, onDomPreUpdate doAction { domHooks :+= "preupdate" }, onDomUpdate doAction { domHooks :+= "update" }, onDomUnmount doAction { domHooks :+= "unmount" }, innerHandler))
      domHooks shouldBe List("mount")

      otherHandler.unsafeOnNext("other")
      domHooks shouldBe List("mount", "preupdate", "update")

      innerHandler.unsafeOnNext("inner")
      domHooks shouldBe List("mount", "preupdate", "update", "preupdate", "update")

      innerHandler.unsafeOnNext(VDomModifier(onDomMount doAction { domHooks :+= "inner-mount" }, onDomPreUpdate doAction { domHooks :+= "inner-preupdate" }, onDomUpdate doAction { domHooks :+= "inner-update" }, onDomUnmount doAction { domHooks :+= "inner-unmount" }, Observable("distract")))
      domHooks shouldBe List("mount", "preupdate", "update", "preupdate", "update", "preupdate", "update", "inner-mount", "preupdate", "inner-preupdate", "update", "inner-update")

      otherHandler.unsafeOnNext(span("hi!"))
      domHooks shouldBe List("mount", "preupdate", "update", "preupdate", "update", "preupdate", "update", "inner-mount", "preupdate", "inner-preupdate", "update", "inner-update", "preupdate", "inner-preupdate", "update", "inner-update")

      innerHandler.unsafeOnNext(VDomModifier(onDomPreUpdate doAction { domHooks :+= "inner-preupdate2" }, onDomUpdate doAction { domHooks :+= "inner-update2" }))
      domHooks shouldBe List("mount", "preupdate", "update", "preupdate", "update", "preupdate", "update", "inner-mount", "preupdate", "inner-preupdate", "update", "inner-update", "preupdate", "inner-preupdate", "update", "inner-update", "preupdate", "inner-unmount", "update")

      innerHandler.unsafeOnNext(VDomModifier(Observable("inner")))
      domHooks shouldBe List("mount", "preupdate", "update", "preupdate", "update", "preupdate", "update", "inner-mount", "preupdate", "inner-preupdate", "update", "inner-update", "preupdate", "inner-preupdate", "update", "inner-update", "preupdate", "inner-unmount", "update", "preupdate", "update", "preupdate", "update")

      innerHandler.unsafeOnNext(VDomModifier(onDomMount doAction { domHooks :+= "inner-mount2" }, onDomUnmount doAction { domHooks :+= "inner-unmount2" }, "something-else"))
      domHooks shouldBe List("mount", "preupdate", "update", "preupdate", "update", "preupdate", "update", "inner-mount", "preupdate", "inner-preupdate", "update", "inner-update", "preupdate", "inner-preupdate", "update", "inner-update", "preupdate", "inner-unmount", "update", "preupdate", "update", "preupdate", "update", "preupdate", "update", "inner-mount2")

      modHandler.unsafeOnNext(onDomMount doAction { domHooks :+= "mount2" })
      domHooks shouldBe List("mount", "preupdate", "update", "preupdate", "update", "preupdate", "update", "inner-mount", "preupdate", "inner-preupdate", "update", "inner-update", "preupdate", "inner-preupdate", "update", "inner-update", "preupdate", "inner-unmount", "update", "preupdate", "update", "preupdate", "update", "preupdate", "update", "inner-mount2", "unmount", "inner-unmount2", "mount2")

      modHandler.unsafeOnNext(onDomUnmount doAction { domHooks :+= "unmount2" })
      domHooks shouldBe List("mount", "preupdate", "update", "preupdate", "update", "preupdate", "update", "inner-mount", "preupdate", "inner-preupdate", "update", "inner-update", "preupdate", "inner-preupdate", "update", "inner-update", "preupdate", "inner-unmount", "update", "preupdate", "update", "preupdate", "update", "preupdate", "update", "inner-mount2", "unmount", "inner-unmount2", "mount2")

      modHandler.unsafeOnNext(VDomModifier.empty)
      domHooks shouldBe List("mount", "preupdate", "update", "preupdate", "update", "preupdate", "update", "inner-mount", "preupdate", "inner-preupdate", "update", "inner-update", "preupdate", "inner-preupdate", "update", "inner-update", "preupdate", "inner-unmount", "update", "preupdate", "update", "preupdate", "update", "preupdate", "update", "inner-mount2", "unmount", "inner-unmount2", "mount2", "unmount2")
    }
  }

  it should "be called for default and streaming out" in {

    var domHooks = List.empty[String]

    val modHandler = Subject.publish[VDomModifier]()
    val innerHandler = Subject.publish[VDomModifier]()
    val otherHandler = Subject.publish[VDomModifier]()
    val node = div(otherHandler, modHandler.prepend(VDomModifier(onDomMount doAction { domHooks :+= "default-mount" }, onDomPreUpdate doAction { domHooks :+= "default-preupdate" }, onDomUpdate doAction { domHooks :+= "default-update" }, onDomUnmount doAction { domHooks :+= "default-unmount" }, innerHandler)))

    OutWatch.renderInto[IO]("#app", node).map { _ =>
      domHooks shouldBe List("default-mount")

      innerHandler.unsafeOnNext(VDomModifier(onDomMount doAction { domHooks :+= "inner-mount" }, onDomPreUpdate doAction { domHooks :+= "inner-preupdate" }, onDomUpdate doAction { domHooks :+= "inner-update" }, onDomUnmount doAction { domHooks :+= "inner-unmount" }))
      domHooks shouldBe List("default-mount", "default-preupdate", "default-update", "inner-mount")

      otherHandler.unsafeOnNext(span("hi!"))
      domHooks shouldBe List("default-mount", "default-preupdate", "default-update", "inner-mount", "default-preupdate", "inner-preupdate", "default-update", "inner-update")

      modHandler.unsafeOnNext(VDomModifier(onDomMount doAction { domHooks :+= "mount" }, onDomUnmount doAction { domHooks :+= "unmount" }))
      domHooks shouldBe List("default-mount", "default-preupdate", "default-update", "inner-mount", "default-preupdate", "inner-preupdate", "default-update", "inner-update", "default-unmount", "inner-unmount", "mount")

      modHandler.unsafeOnNext(onDomMount doAction { domHooks :+= "mount2" })
      domHooks shouldBe List("default-mount", "default-preupdate", "default-update", "inner-mount", "default-preupdate", "inner-preupdate", "default-update", "inner-update", "default-unmount", "inner-unmount", "mount", "unmount", "mount2")

      modHandler.unsafeOnNext(onDomUnmount doAction { domHooks :+= "unmount2" })
      domHooks shouldBe List("default-mount", "default-preupdate", "default-update", "inner-mount", "default-preupdate", "inner-preupdate", "default-update", "inner-update", "default-unmount", "inner-unmount", "mount", "unmount", "mount2")

      modHandler.unsafeOnNext(VDomModifier.empty)
      domHooks shouldBe List("default-mount", "default-preupdate", "default-update", "inner-mount", "default-preupdate", "inner-preupdate", "default-update", "inner-update", "default-unmount", "inner-unmount", "mount", "unmount", "mount2", "unmount2")
    }
  }

  it should "have unmount before mount hook when streamed" in {

    var domHooks = List.empty[String]

    val countHandler = Subject.publish[Int]()
    val node = div(
      countHandler.map { count =>
        VDomModifier(
          onDomMount.doAction { domHooks :+= "mount" + count },
          onDomUnmount.doAction { domHooks :+= "unmount" + count },
          div(
            onDomMount.doAction { domHooks :+= "child-mount" + count },
            onDomUnmount.doAction { domHooks :+= "child-unmount" + count }
          )
        )
      }
    )

    OutWatch.renderInto[IO]("#app", node).map { _ =>
      domHooks shouldBe List.empty

      countHandler.unsafeOnNext(1)
      domHooks shouldBe List("mount1", "child-mount1")

      countHandler.unsafeOnNext(2)
      domHooks shouldBe List("mount1", "child-mount1", "unmount1", "child-unmount1", "child-mount2", "mount2")
    }
  }

  "Hooks" should "support emitter operations" in {

    val operations = mutable.ArrayBuffer.empty[String]

    val observer = Observer.create { (op: String) =>
      operations += op
    }

    val divTagName = onSnabbdomInsert.map(_.tagName.toLowerCase).filter(_ == "div")

    val node = div(
      dsl.key := "unique",
      onSnabbdomInsert.as("insert") --> observer,
      div(divTagName --> observer),
      span(divTagName --> observer)
    )

    OutWatch.renderInto[IO]("#app", node).map { _ =>
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

    val innerHandler = Subject.replayLatest[Int]()
    val handler = Subject.replayLatest[(String, String)]()
    val otherHandler = Subject.replayLatest[String]()

    var counter = 0
    val node = div(
      idAttr := "strings",
      otherHandler.map(div(_)),
      handler.map { case (keyText, text) =>
        counter += 1
        val c = counter
        div(
          dsl.key := keyText,
          text,
          innerHandler.map { i => innerHandlerCount += 1; i },
          onSnabbdomInsert doAction { onSnabbdomInsertCount += 1 },
          onSnabbdomPrePatch doAction { onSnabbdomPrePatchCount += 1 },
          onSnabbdomPostPatch doAction { onSnabbdomPostPatchCount += 1 },
          onSnabbdomUpdate doAction { onSnabbdomUpdateCount += 1 },
          onSnabbdomDestroy doAction { onSnabbdomDestroyCount += 1 },
          onDomMount doAction { onDomMountList :+= c },
          onDomUnmount doAction { onDomUnmountList :+= c },
          onDomUpdate doAction { onDomUpdateList :+= c }
        )
      }
    )

    OutWatch.renderInto[IO]("#app", node).map { _ =>
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

      handler.unsafeOnNext(("key", "heinz"))
      element.innerHTML shouldBe "<div>heinz</div>"
      onSnabbdomInsertCount shouldBe 1
      onSnabbdomPrePatchCount shouldBe 0
      onSnabbdomPostPatchCount shouldBe 0
      onSnabbdomUpdateCount shouldBe 0
      onSnabbdomDestroyCount shouldBe 0
      onDomMountList shouldBe List(1)
      onDomUnmountList shouldBe Nil
      onDomUpdateList shouldBe Nil

      handler.unsafeOnNext(("key", "golum"))
      element.innerHTML shouldBe "<div>golum</div>"
      onSnabbdomInsertCount shouldBe 1
      onSnabbdomPrePatchCount shouldBe 1
      onSnabbdomPostPatchCount shouldBe 1
      onSnabbdomUpdateCount shouldBe 1
      onSnabbdomDestroyCount shouldBe 0
      onDomMountList shouldBe List(1, 2)
      onDomUnmountList shouldBe List(1)
      onDomUpdateList shouldBe Nil

      handler.unsafeOnNext(("key", "dieter"))
      element.innerHTML shouldBe "<div>dieter</div>"
      onSnabbdomInsertCount shouldBe 1
      onSnabbdomPrePatchCount shouldBe 2
      onSnabbdomPostPatchCount shouldBe 2
      onSnabbdomUpdateCount shouldBe 2
      onSnabbdomDestroyCount shouldBe 0
      onDomMountList shouldBe List(1, 2, 3)
      onDomUnmountList shouldBe List(1,2)
      onDomUpdateList shouldBe Nil

      handler.unsafeOnNext(("nope", "dieter"))
      element.innerHTML shouldBe "<div>dieter</div>"
      onSnabbdomInsertCount shouldBe 2
      onSnabbdomPrePatchCount shouldBe 2
      onSnabbdomPostPatchCount shouldBe 2
      onSnabbdomUpdateCount shouldBe 2
      onSnabbdomDestroyCount shouldBe 1
      onDomMountList shouldBe List(1, 2 ,3, 4)
      onDomUnmountList shouldBe List(1,2,3)
      onDomUpdateList shouldBe Nil

      handler.unsafeOnNext(("yes", "peter"))
      element.innerHTML shouldBe "<div>peter</div>"
      onSnabbdomInsertCount shouldBe 3
      onSnabbdomPrePatchCount shouldBe 2
      onSnabbdomPostPatchCount shouldBe 2
      onSnabbdomUpdateCount shouldBe 2
      onSnabbdomDestroyCount shouldBe 2
      onDomMountList shouldBe List(1, 2, 3, 4, 5)
      onDomUnmountList shouldBe List(1, 2, 3, 4)
      onDomUpdateList shouldBe Nil

      otherHandler.unsafeOnNext("hi")
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

      innerHandler.unsafeOnNext(0)
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

    val innerHandler = Subject.replayLatest[Int]()
    val handler = Subject.replayLatest[String]()
    val otherHandler = Subject.replayLatest[String]()

    var counter = 0
    val node = div(
      idAttr := "strings",
      otherHandler.map(div(_)),
      handler.map { text =>
        counter += 1
        val c = counter
        div(
          text,
          innerHandler.map { i => innerHandlerCount += 1; i },
          onSnabbdomInsert doAction { onSnabbdomInsertCount += 1 },
          onSnabbdomPrePatch doAction { onSnabbdomPrePatchCount += 1 },
          onSnabbdomPostPatch doAction { onSnabbdomPostPatchCount += 1 },
          onSnabbdomUpdate doAction { onSnabbdomUpdateCount += 1 },
          onSnabbdomDestroy doAction { onSnabbdomDestroyCount += 1 },
          onDomMount doAction { onDomMountList :+= c },
          onDomUnmount doAction { onDomUnmountList :+= c },
          onDomUpdate doAction { onDomUpdateList :+= c }
        )
      }
    )

    OutWatch.renderInto[IO]("#app", node).map { _ =>
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

      handler.unsafeOnNext("heinz")
      element.innerHTML shouldBe "<div>heinz</div>"
      onSnabbdomInsertCount shouldBe 1
      onSnabbdomPrePatchCount shouldBe 0
      onSnabbdomPostPatchCount shouldBe 0
      onSnabbdomUpdateCount shouldBe 0
      onSnabbdomDestroyCount shouldBe 0
      onDomMountList shouldBe List(1)
      onDomUnmountList shouldBe Nil
      onDomUpdateList shouldBe Nil

      handler.unsafeOnNext("golum")
      element.innerHTML shouldBe "<div>golum</div>"
      onSnabbdomInsertCount shouldBe 1
      onSnabbdomPrePatchCount shouldBe 1
      onSnabbdomPostPatchCount shouldBe 1
      onSnabbdomUpdateCount shouldBe 1
      onSnabbdomDestroyCount shouldBe 0
      onDomMountList shouldBe List(1, 2)
      onDomUnmountList shouldBe List(1)
      onDomUpdateList shouldBe Nil

      handler.unsafeOnNext("dieter")
      element.innerHTML shouldBe "<div>dieter</div>"
      onSnabbdomInsertCount shouldBe 1
      onSnabbdomPrePatchCount shouldBe 2
      onSnabbdomPostPatchCount shouldBe 2
      onSnabbdomUpdateCount shouldBe 2
      onSnabbdomDestroyCount shouldBe 0
      onDomMountList shouldBe List(1, 2, 3)
      onDomUnmountList shouldBe List(1,2)
      onDomUpdateList shouldBe Nil

      handler.unsafeOnNext("dieter")
      element.innerHTML shouldBe "<div>dieter</div>"
      onSnabbdomInsertCount shouldBe 1
      onSnabbdomPrePatchCount shouldBe 3
      onSnabbdomPostPatchCount shouldBe 3
      onSnabbdomUpdateCount shouldBe 3
      onSnabbdomDestroyCount shouldBe 0
      onDomMountList shouldBe List(1, 2 ,3, 4)
      onDomUnmountList shouldBe List(1,2,3)
      onDomUpdateList shouldBe Nil

      handler.unsafeOnNext("peter")
      element.innerHTML shouldBe "<div>peter</div>"
      onSnabbdomInsertCount shouldBe 1
      onSnabbdomPrePatchCount shouldBe 4
      onSnabbdomPostPatchCount shouldBe 4
      onSnabbdomUpdateCount shouldBe 4
      onSnabbdomDestroyCount shouldBe 0
      onDomMountList shouldBe List(1, 2, 3, 4, 5)
      onDomUnmountList shouldBe List(1, 2, 3, 4)
      onDomUpdateList shouldBe Nil

      otherHandler.unsafeOnNext("hi")
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

      innerHandler.unsafeOnNext(0)
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
