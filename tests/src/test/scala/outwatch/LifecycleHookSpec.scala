package outwatch

import cats.effect.IO
import org.scalajs.dom._

import outwatch.dsl._

import colibri._

import scala.collection.mutable

class LifecycleHookSpec extends JSDomAsyncSpec {

  "Insertion hooks" should "be called correctly" in {

    var switch = false
    val observer = Observer.create { (_: Element) =>
      switch = true
    }

    val node = div(dsl.key := "unique", onSnabbdomInsert --> observer)

    switch shouldBe false

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)
      _  = switch shouldBe true
    } yield succeed
  }

  it should "be called correctly on merged nodes" in {
    var switch = false
    val observer = Observer.create { (_: Element) =>
      switch = true
    }
    var switch2 = false
    val observer2 = Observer.create { (_: Element) =>
      switch2 = true
    }

    val node = div(dsl.key := "unique", onSnabbdomInsert --> observer)(onSnabbdomInsert --> observer2)

    switch shouldBe false
    switch2 shouldBe false

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)
      _  = switch shouldBe true
      _  = switch2 shouldBe true
    } yield succeed
  }

  "Destruction hooks" should "be called correctly" in {

    var switch = false
    val observer = Observer.create { (_: Element) =>
      switch = true
    }

    val innerHandler = Subject.publish[VModifier]()
    val node         = div(innerHandler.prepend(span(onSnabbdomDestroy --> observer)))

    switch shouldBe false

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)
      _  = switch shouldBe false

      _ <- innerHandler.onNextIO(div("Hasdasd")) *> IO.cede

      _ = switch shouldBe true
    } yield succeed
  }

  it should "be called correctly on merged nodes" in {

    var switch = false
    val observer = Observer.create { (_: Element) =>
      switch = true
    }
    var switch2 = false
    val observer2 = Observer.create { (_: Element) =>
      switch2 = true
    }

    val innerHandler = Subject.publish[VModifier]()
    val node         = div(innerHandler.prepend(span(onSnabbdomDestroy --> observer)(onSnabbdomDestroy --> observer2)))

    switch shouldBe false
    switch2 shouldBe false

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)
      _  = switch shouldBe false
      _  = switch2 shouldBe false

      _ <- innerHandler.onNextIO(div("Hasdasd")) *> IO.cede

      _ = switch shouldBe true
      _ = switch2 shouldBe true
    } yield succeed
  }

  "Update hooks" should "be called correctly on merged nodes" in {
    var switch1 = false
    val observer1 = Observer.create { (_: (Element, Element)) =>
      switch1 = true
    }
    var switch2 = false
    val observer2 = Observer.create { (_: (Element, Element)) =>
      switch2 = true
    }

    val message = Subject.publish[String]()
    val node    = div(message, dsl.key := "unique", onSnabbdomUpdate --> observer1)(onSnabbdomUpdate --> observer2)

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      _ = switch1 shouldBe false
      _ = switch2 shouldBe false

      _ <- message.onNextIO("wursi") *> IO.cede
      _  = switch1 shouldBe true
      _  = switch2 shouldBe true
    } yield succeed
  }

  it should "be called correctly" in {

    var switch = false
    val observer = Observer.create { (_: (Element, Element)) =>
      switch = true
    }

    val innerHandler = Subject.publish[VModifier]()
    val node         = div(innerHandler.prepend(span(onSnabbdomUpdate --> observer, "Hello")))

    switch shouldBe false

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)
      _  = switch shouldBe false

      _ <- innerHandler.onNextIO(span(onSnabbdomUpdate --> observer, "Hey")) *> IO.cede

      _ = switch shouldBe true
    } yield succeed
  }

  "Prepatch hooks" should "be called" in {

    var switch = false
    val observer = Observer.create { (_: (Option[Element], Option[Element])) =>
      switch = true
    }

    val prepatchNode = span(attributes.key := "1", onSnabbdomPrePatch --> observer, "Hey", Observable("distract-sync"))
    val handler      = Subject.behavior[VModifier](prepatchNode)
    val node         = div(Observable(span("Hello")), handler)

    switch shouldBe false

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)
      _  = switch shouldBe false

      _ <- handler.onNextIO(prepatchNode) *> IO.cede

      _ = switch shouldBe true
    } yield succeed
  }

  it should "be called correctly on merged nodes" in {
    var switch1 = false
    val observer1 = Observer.create { (_: (Option[Element], Option[Element])) =>
      switch1 = true
    }
    var switch2 = false
    val observer2 = Observer.create { (_: (Option[Element], Option[Element])) =>
      switch2 = true
    }

    val message = Subject.publish[String]()
    val node    = div(message, dsl.key := "unique", onSnabbdomPrePatch --> observer1)(onSnabbdomPrePatch --> observer2)

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)
      _  = switch1 shouldBe false
      _  = switch2 shouldBe false

      _ <- message.onNextIO("wursi") *> IO.cede

      _ = switch1 shouldBe true
      _ = switch2 shouldBe true
    } yield succeed
  }

  "Postpatch hooks" should "be called" in {

    var switch = false
    val observer = Observer.create { (_: (Element, Element)) =>
      switch = true
    }

    val message = Subject.publish[String]()
    val node    = div(message, dsl.key := "unique", onSnabbdomPostPatch --> observer, "Hey")

    switch shouldBe false

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)
      _  = switch shouldBe false

      _ <- message.onNextIO("hallo") *> IO.cede

      _ = switch shouldBe true
    } yield succeed
  }

  it should "be called correctly on merged nodes" in {
    var switch1 = false
    val observer1 = Observer.create { (_: (Element, Element)) =>
      switch1 = true
    }
    var switch2 = false
    val observer2 = Observer.create { (_: (Element, Element)) =>
      switch2 = true
    }
    val message = Subject.publish[String]()
    val node    = div(message, dsl.key := "unique", onSnabbdomPostPatch --> observer1)(onSnabbdomPostPatch --> observer2)

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)
      _  = switch1 shouldBe false
      _  = switch2 shouldBe false

      _ <- message.onNextIO("wursi") *> IO.cede

      _ = switch1 shouldBe true
      _ = switch2 shouldBe true
    } yield succeed
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
      onSnabbdomDestroy --> destroyObs,
    )

    hooks shouldBe empty

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)
      _  = hooks.toList shouldBe List("insert")

      _ <- message.onNextIO("next") *> IO.cede

      _ = hooks.toList shouldBe List("insert", "prepatch", "update", "postpatch")
    } yield succeed
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
    val node = div(
      dsl.key := "unique",
      "Hello",
      messageList.map(_.map(span(_))),
      onSnabbdomInsert --> insertObs,
      onSnabbdomUpdate --> updateObs,
    )

    hooks shouldBe empty

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)
      _  = hooks.toList shouldBe List("insert")
    } yield succeed
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
    val node = div(
      span("Hello", onSnabbdomInsert --> insertObs, onSnabbdomUpdate --> updateObs, onSnabbdomDestroy --> destroyObs),
      message.map(span(_)),
    )

    hooks shouldBe empty

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)
      _ <- message.onNextIO("next") *> IO.cede

      _ = hooks.contains("destroy") shouldBe false
    } yield succeed
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
    val node = div(
      messageList.map(_.map(span(_))),
      span("Hello", onSnabbdomInsert --> insertObs, onSnabbdomUpdate --> updateObs, onSnabbdomDestroy --> destroyObs),
    )

    hooks shouldBe empty

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)
      _ <- messageList.onNextIO(Seq("one")) *> IO.cede

      _ <- messageList.onNextIO(Seq("one", "two")) *> IO.cede

      _ = hooks.count(_ == "insert") shouldBe 1
    } yield succeed
  }

  "Managed subscriptions" should "subscribe on insert and unsubscribe on destroy" in {

    val nodes = Subject.publish[VNode]()

    var latest = ""
    val observer = Observer.create { (elem: String) =>
      latest = elem
    }

    val sub = Subject.publish[String]()

    val node = div(
      nodes.startWith(
        Seq(
          span(VModifier.managedEval(sub.unsafeSubscribe(observer))),
        ),
      ),
    )

    for {
      _ <- sub.onNextIO("pre") *> IO.cede
      _  = latest shouldBe ""

      _ <- Outwatch.renderInto[IO]("#app", node)
      _ <- sub.onNextIO("first") *> IO.cede
      _  = latest shouldBe "first"

      _ <- nodes.onNextIO(div()) *> IO.cede // this triggers child destroy and subscription cancelation

      _ <- sub.onNextIO("second") *> IO.cede
      _  = latest shouldBe "first"
    } yield succeed
  }

  it should "subscribe on insert and unsubscribe on destroy 2" in {

    val nodes = Subject.publish[VNode]()

    var latest = ""
    val observer = Observer.create { (elem: String) =>
      latest = elem
    }

    val sub = Subject.publish[String]()

    val node = div(
      nodes.startWith(
        Seq(
          span(VModifier.managedSubscribe(sub.to(observer))),
        ),
      ),
    )

    for {
      _ <- sub.onNextIO("pre") *> IO.cede
      _  = latest shouldBe ""
      _ <- Outwatch.renderInto[IO]("#app", node)
      _ <- sub.onNextIO("first") *> IO.cede
      _  = latest shouldBe "first"

      _ <- nodes.onNextIO(div()) *> IO.cede // this triggers child destroy and subscription cancelation

      _ <- sub.onNextIO("second") *> IO.cede
      _  = latest shouldBe "first"
    } yield succeed
  }

  it should "work with EmitterBuilder.fromSource(observable)" in {

    val nodes = Subject.publish[VNode]()

    var latest = ""
    val observer = Observer.create { (elem: String) =>
      latest = elem
    }

    val sub = Subject.publish[String]()

    val node = div(
      nodes.startWith(
        Seq(
          span(EmitterBuilder.fromSource(sub) --> observer),
        ),
      ),
    )

    for {
      _ <- sub.onNextIO("pre") *> IO.cede
      _  = latest shouldBe ""
      _ <- Outwatch.renderInto[IO]("#app", node)
      _ <- sub.onNextIO("first") *> IO.cede
      _  = latest shouldBe "first"

      _ <- nodes.onNextIO(div()) *> IO.cede // this triggers child destroy and subscription cancelation

      _ <- sub.onNextIO("second") *> IO.cede
      _  = latest shouldBe "first"
    } yield succeed
  }

  "DomHook" should "be called on static nodes" in {

    var domHooks = List.empty[String]

    val modHandler = Subject.publish[VModifier]()

    val node = div(modHandler)

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      _ <- modHandler.onNextIO(
             div(onDomMount doAction { domHooks :+= "mount" }, p(onDomUnmount doAction { domHooks :+= "unmount" })),
           ) *> IO.cede
      _ = domHooks shouldBe List("mount")

      innerHandler = Subject.publish[String]()
      _ <- modHandler.onNextIO(
             div(
               "meh",
               p(onDomMount doAction { domHooks :+= "mount2" }),
               onDomPreUpdate doAction { domHooks :+= "preupdate2" },
               onDomUpdate doAction { domHooks :+= "update2" },
               onDomUnmount doAction { domHooks :+= "unmount2" },
               innerHandler,
               Observable("distract-sync"),
             ),
           ) *> IO.cede
      _ = domHooks shouldBe List("mount", "unmount", "mount2")

      _ <- innerHandler.onNextIO("distract") *> IO.cede
      _  = domHooks shouldBe List("mount", "unmount", "mount2", "preupdate2", "update2")

      _ <- modHandler.onNextIO(
             span(
               "muh",
               onDomMount doAction { domHooks :+= "mount3" },
               onDomPreUpdate doAction { domHooks :+= "preupdate3" },
               onDomUpdate doAction { domHooks :+= "update3" },
               onDomUnmount doAction { domHooks :+= "unmount3" },
             ),
           ) *> IO.cede
      _ = domHooks shouldBe List("mount", "unmount", "mount2", "preupdate2", "update2", "unmount2", "mount3")

      _ <- modHandler.onNextIO(VModifier.empty) *> IO.cede
      _ =
        domHooks shouldBe List("mount", "unmount", "mount2", "preupdate2", "update2", "unmount2", "mount3", "unmount3")
    } yield succeed
  }

  it should "be called on nested streaming" in {

    var domHooks = List.empty[String]

    val modHandler   = Subject.publish[VModifier]()
    val innerHandler = Subject.publish[VModifier]()
    val node         = div(modHandler)

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      _ <- modHandler.onNextIO(VModifier(innerHandler)) *> IO.cede
      _  = domHooks shouldBe List()

      _ <- innerHandler.onNextIO("inner") *> IO.cede
      _  = domHooks shouldBe List()

      _ <- innerHandler.onNextIO(onDomMount doAction { domHooks :+= "inner-mount" }) *> IO.cede
      _  = domHooks shouldBe List("inner-mount")

      _ <- innerHandler.onNextIO(onDomUnmount doAction { domHooks :+= "inner-unmount" }) *> IO.cede
      _  = domHooks shouldBe List("inner-mount")

      _ <- innerHandler.onNextIO(onDomUnmount doAction { domHooks :+= "inner-unmount2" }) *> IO.cede
      _  = domHooks shouldBe List("inner-mount", "inner-unmount")

      _ <- modHandler.onNextIO(VModifier.empty) *> IO.cede
      _  = domHooks shouldBe List("inner-mount", "inner-unmount", "inner-unmount2")
    } yield succeed
  }

  it should "be called on streaming in and streaming out" in {

    var domHooks = List.empty[String]

    val modHandler   = Subject.publish[VModifier]()
    val otherHandler = Subject.publish[VModifier]()
    val innerHandler = Subject.publish[VModifier]()
    val node         = div(modHandler, otherHandler)

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)

      _ <- modHandler.onNextIO(
             VModifier(
               onDomMount doAction { domHooks :+= "mount" },
               onDomPreUpdate doAction { domHooks :+= "preupdate" },
               onDomUpdate doAction { domHooks :+= "update" },
               onDomUnmount doAction { domHooks :+= "unmount" },
               innerHandler,
             ),
           ) *> IO.cede
      _ = domHooks shouldBe List("mount")

      _ <- otherHandler.onNextIO("other") *> IO.cede
      _  = domHooks shouldBe List("mount", "preupdate", "update")

      _ <- innerHandler.onNextIO("inner") *> IO.cede
      _  = domHooks shouldBe List("mount", "preupdate", "update", "preupdate", "update")

      _ <- innerHandler.onNextIO(
             VModifier(
               onDomMount doAction { domHooks :+= "inner-mount" },
               onDomPreUpdate doAction { domHooks :+= "inner-preupdate" },
               onDomUpdate doAction { domHooks :+= "inner-update" },
               onDomUnmount doAction { domHooks :+= "inner-unmount" },
               Observable("distract"),
             ),
           ) *> IO.cede
      _ = domHooks shouldBe List(
            "mount",
            "preupdate",
            "update",
            "preupdate",
            "update",
            "preupdate",
            "update",
            "inner-mount",
          )

      _ <- otherHandler.onNextIO(span("hi!")) *> IO.cede
      _ = domHooks shouldBe List(
            "mount",
            "preupdate",
            "update",
            "preupdate",
            "update",
            "preupdate",
            "update",
            "inner-mount",
            "preupdate",
            "inner-preupdate",
            "update",
            "inner-update",
          )

      _ <- innerHandler.onNextIO(
             VModifier(
               onDomPreUpdate doAction { domHooks :+= "inner-preupdate2" },
               onDomUpdate doAction { domHooks :+= "inner-update2" },
             ),
           ) *> IO.cede
      _ = domHooks shouldBe List(
            "mount",
            "preupdate",
            "update",
            "preupdate",
            "update",
            "preupdate",
            "update",
            "inner-mount",
            "preupdate",
            "inner-preupdate",
            "update",
            "inner-update",
            "preupdate",
            "inner-unmount",
            "update",
          )

      _ <- innerHandler.onNextIO(VModifier(Observable("inner"))) *> IO.cede
      _ = domHooks shouldBe List(
            "mount",
            "preupdate",
            "update",
            "preupdate",
            "update",
            "preupdate",
            "update",
            "inner-mount",
            "preupdate",
            "inner-preupdate",
            "update",
            "inner-update",
            "preupdate",
            "inner-unmount",
            "update",
            "preupdate",
            "update",
          )

      _ <- innerHandler.onNextIO(
             VModifier(
               onDomMount doAction { domHooks :+= "inner-mount2" },
               onDomUnmount doAction { domHooks :+= "inner-unmount2" },
               "something-else",
             ),
           ) *> IO.cede
      _ = domHooks shouldBe List(
            "mount",
            "preupdate",
            "update",
            "preupdate",
            "update",
            "preupdate",
            "update",
            "inner-mount",
            "preupdate",
            "inner-preupdate",
            "update",
            "inner-update",
            "preupdate",
            "inner-unmount",
            "update",
            "preupdate",
            "update",
            "preupdate",
            "update",
            "inner-mount2",
          )

      _ <- modHandler.onNextIO(onDomMount doAction { domHooks :+= "mount2" }) *> IO.cede
      _ = domHooks shouldBe List(
            "mount",
            "preupdate",
            "update",
            "preupdate",
            "update",
            "preupdate",
            "update",
            "inner-mount",
            "preupdate",
            "inner-preupdate",
            "update",
            "inner-update",
            "preupdate",
            "inner-unmount",
            "update",
            "preupdate",
            "update",
            "preupdate",
            "update",
            "inner-mount2",
            "unmount",
            "inner-unmount2",
            "mount2",
          )

      _ <- modHandler.onNextIO(onDomUnmount doAction { domHooks :+= "unmount2" }) *> IO.cede
      _ = domHooks shouldBe List(
            "mount",
            "preupdate",
            "update",
            "preupdate",
            "update",
            "preupdate",
            "update",
            "inner-mount",
            "preupdate",
            "inner-preupdate",
            "update",
            "inner-update",
            "preupdate",
            "inner-unmount",
            "update",
            "preupdate",
            "update",
            "preupdate",
            "update",
            "inner-mount2",
            "unmount",
            "inner-unmount2",
            "mount2",
          )

      _ <- modHandler.onNextIO(VModifier.empty) *> IO.cede
      _ = domHooks shouldBe List(
            "mount",
            "preupdate",
            "update",
            "preupdate",
            "update",
            "preupdate",
            "update",
            "inner-mount",
            "preupdate",
            "inner-preupdate",
            "update",
            "inner-update",
            "preupdate",
            "inner-unmount",
            "update",
            "preupdate",
            "update",
            "preupdate",
            "update",
            "inner-mount2",
            "unmount",
            "inner-unmount2",
            "mount2",
            "unmount2",
          )
    } yield succeed
  }

  it should "be called for default and streaming out" in {

    var domHooks = List.empty[String]

    val modHandler   = Subject.publish[VModifier]()
    val innerHandler = Subject.publish[VModifier]()
    val otherHandler = Subject.publish[VModifier]()
    val node = div(
      otherHandler,
      modHandler.prepend(
        VModifier(
          onDomMount doAction { domHooks :+= "default-mount" },
          onDomPreUpdate doAction { domHooks :+= "default-preupdate" },
          onDomUpdate doAction { domHooks :+= "default-update" },
          onDomUnmount doAction { domHooks :+= "default-unmount" },
          innerHandler,
        ),
      ),
    )

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)
      _  = domHooks shouldBe List("default-mount")

      _ <- innerHandler.onNextIO(
             VModifier(
               onDomMount doAction { domHooks :+= "inner-mount" },
               onDomPreUpdate doAction { domHooks :+= "inner-preupdate" },
               onDomUpdate doAction { domHooks :+= "inner-update" },
               onDomUnmount doAction { domHooks :+= "inner-unmount" },
             ),
           ) *> IO.cede
      _ = domHooks shouldBe List("default-mount", "default-preupdate", "default-update", "inner-mount")

      _ <- otherHandler.onNextIO(span("hi!")) *> IO.cede
      _ = domHooks shouldBe List(
            "default-mount",
            "default-preupdate",
            "default-update",
            "inner-mount",
            "default-preupdate",
            "inner-preupdate",
            "default-update",
            "inner-update",
          )

      _ <- modHandler.onNextIO(
             VModifier(onDomMount doAction { domHooks :+= "mount" }, onDomUnmount doAction { domHooks :+= "unmount" }),
           ) *> IO.cede
      _ = domHooks shouldBe List(
            "default-mount",
            "default-preupdate",
            "default-update",
            "inner-mount",
            "default-preupdate",
            "inner-preupdate",
            "default-update",
            "inner-update",
            "default-unmount",
            "inner-unmount",
            "mount",
          )

      _ <- modHandler.onNextIO(onDomMount doAction { domHooks :+= "mount2" }) *> IO.cede
      _ = domHooks shouldBe List(
            "default-mount",
            "default-preupdate",
            "default-update",
            "inner-mount",
            "default-preupdate",
            "inner-preupdate",
            "default-update",
            "inner-update",
            "default-unmount",
            "inner-unmount",
            "mount",
            "unmount",
            "mount2",
          )

      _ <- modHandler.onNextIO(onDomUnmount doAction { domHooks :+= "unmount2" }) *> IO.cede
      _ = domHooks shouldBe List(
            "default-mount",
            "default-preupdate",
            "default-update",
            "inner-mount",
            "default-preupdate",
            "inner-preupdate",
            "default-update",
            "inner-update",
            "default-unmount",
            "inner-unmount",
            "mount",
            "unmount",
            "mount2",
          )

      _ <- modHandler.onNextIO(VModifier.empty) *> IO.cede
      _ = domHooks shouldBe List(
            "default-mount",
            "default-preupdate",
            "default-update",
            "inner-mount",
            "default-preupdate",
            "inner-preupdate",
            "default-update",
            "inner-update",
            "default-unmount",
            "inner-unmount",
            "mount",
            "unmount",
            "mount2",
            "unmount2",
          )
    } yield succeed
  }

  it should "have unmount before mount hook when streamed" in {

    var domHooks = List.empty[String]

    val countHandler = Subject.publish[Int]()
    val node = div(
      countHandler.map { count =>
        VModifier(
          onDomMount.doAction { domHooks :+= "mount" + count },
          onDomUnmount.doAction { domHooks :+= "unmount" + count },
          div(
            onDomMount.doAction { domHooks :+= "child-mount" + count },
            onDomUnmount.doAction { domHooks :+= "child-unmount" + count },
          ),
        )
      },
    )

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)
      _  = domHooks shouldBe List.empty

      _ <- countHandler.onNextIO(1) *> IO.cede
      _  = domHooks shouldBe List("mount1", "child-mount1")

      _ <- countHandler.onNextIO(2) *> IO.cede
      _  = domHooks shouldBe List("mount1", "child-mount1", "unmount1", "child-unmount1", "child-mount2", "mount2")
    } yield succeed
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
      span(divTagName --> observer),
    )

    for {
      _ <- Outwatch.renderInto[IO]("#app", node)
      _  = operations.toList shouldBe List("div", "insert")
    } yield succeed
  }

  "Lifecycle and subscription" should "with a snabbdom key" in {
    var onSnabbdomInsertCount    = 0
    var onSnabbdomPrePatchCount  = 0
    var onSnabbdomPostPatchCount = 0
    var onSnabbdomUpdateCount    = 0
    var onSnabbdomDestroyCount   = 0
    var innerHandlerCount        = 0
    var onDomMountList           = List.empty[Int]
    var onDomUnmountList         = List.empty[Int]
    var onDomUpdateList          = List.empty[Int]

    val innerHandler = Subject.replayLatest[Int]()
    val handler      = Subject.replayLatest[(String, String)]()
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
          onDomUpdate doAction { onDomUpdateList :+= c },
        )
      },
    )

    for {
      _      <- Outwatch.renderInto[IO]("#app", node)
      element = document.getElementById("strings")

      _ = element.innerHTML shouldBe ""
      _ = onSnabbdomInsertCount shouldBe 0
      _ = onSnabbdomPrePatchCount shouldBe 0
      _ = onSnabbdomPostPatchCount shouldBe 0
      _ = onSnabbdomUpdateCount shouldBe 0
      _ = onSnabbdomDestroyCount shouldBe 0
      _ = onDomMountList shouldBe Nil
      _ = onDomUnmountList shouldBe Nil
      _ = onDomUpdateList shouldBe Nil

      _ <- handler.onNextIO(("key", "heinz")) *> IO.cede
      _  = element.innerHTML shouldBe "<div>heinz</div>"
      _  = onSnabbdomInsertCount shouldBe 1
      _  = onSnabbdomPrePatchCount shouldBe 0
      _  = onSnabbdomPostPatchCount shouldBe 0
      _  = onSnabbdomUpdateCount shouldBe 0
      _  = onSnabbdomDestroyCount shouldBe 0
      _  = onDomMountList shouldBe List(1)
      _  = onDomUnmountList shouldBe Nil
      _  = onDomUpdateList shouldBe Nil

      _ <- handler.onNextIO(("key", "golum")) *> IO.cede
      _  = element.innerHTML shouldBe "<div>golum</div>"
      _  = onSnabbdomInsertCount shouldBe 1
      _  = onSnabbdomPrePatchCount shouldBe 1
      _  = onSnabbdomPostPatchCount shouldBe 1
      _  = onSnabbdomUpdateCount shouldBe 1
      _  = onSnabbdomDestroyCount shouldBe 0
      _  = onDomMountList shouldBe List(1, 2)
      _  = onDomUnmountList shouldBe List(1)
      _  = onDomUpdateList shouldBe Nil

      _ <- handler.onNextIO(("key", "dieter")) *> IO.cede
      _  = element.innerHTML shouldBe "<div>dieter</div>"
      _  = onSnabbdomInsertCount shouldBe 1
      _  = onSnabbdomPrePatchCount shouldBe 2
      _  = onSnabbdomPostPatchCount shouldBe 2
      _  = onSnabbdomUpdateCount shouldBe 2
      _  = onSnabbdomDestroyCount shouldBe 0
      _  = onDomMountList shouldBe List(1, 2, 3)
      _  = onDomUnmountList shouldBe List(1, 2)
      _  = onDomUpdateList shouldBe Nil

      _ <- handler.onNextIO(("nope", "dieter")) *> IO.cede
      _  = element.innerHTML shouldBe "<div>dieter</div>"
      _  = onSnabbdomInsertCount shouldBe 2
      _  = onSnabbdomPrePatchCount shouldBe 2
      _  = onSnabbdomPostPatchCount shouldBe 2
      _  = onSnabbdomUpdateCount shouldBe 2
      _  = onSnabbdomDestroyCount shouldBe 1
      _  = onDomMountList shouldBe List(1, 2, 3, 4)
      _  = onDomUnmountList shouldBe List(1, 2, 3)
      _  = onDomUpdateList shouldBe Nil

      _ <- handler.onNextIO(("yes", "peter")) *> IO.cede
      _  = element.innerHTML shouldBe "<div>peter</div>"
      _  = onSnabbdomInsertCount shouldBe 3
      _  = onSnabbdomPrePatchCount shouldBe 2
      _  = onSnabbdomPostPatchCount shouldBe 2
      _  = onSnabbdomUpdateCount shouldBe 2
      _  = onSnabbdomDestroyCount shouldBe 2
      _  = onDomMountList shouldBe List(1, 2, 3, 4, 5)
      _  = onDomUnmountList shouldBe List(1, 2, 3, 4)
      _  = onDomUpdateList shouldBe Nil

      _ <- otherHandler.onNextIO("hi") *> IO.cede
      _  = element.innerHTML shouldBe "<div>hi</div><div>peter</div>"
      _  = onSnabbdomInsertCount shouldBe 3
      _  = onSnabbdomPrePatchCount shouldBe 2
      _  = onSnabbdomPostPatchCount shouldBe 2
      _  = onSnabbdomUpdateCount shouldBe 2
      _  = onSnabbdomDestroyCount shouldBe 2
      _  = onDomMountList shouldBe List(1, 2, 3, 4, 5)
      _  = onDomUnmountList shouldBe List(1, 2, 3, 4)
      _  = onDomUpdateList shouldBe Nil

      _ = innerHandlerCount shouldBe 0

      _ <- innerHandler.onNextIO(0) *> IO.cede
      _  = element.innerHTML shouldBe "<div>hi</div><div>peter0</div>"
      _  = onSnabbdomInsertCount shouldBe 3
      _  = onSnabbdomPrePatchCount shouldBe 3
      _  = onSnabbdomPostPatchCount shouldBe 3
      _  = onSnabbdomUpdateCount shouldBe 3
      _  = onSnabbdomDestroyCount shouldBe 2
      _  = onDomMountList shouldBe List(1, 2, 3, 4, 5)
      _  = onDomUnmountList shouldBe List(1, 2, 3, 4)
      _  = onDomUpdateList shouldBe List(5)

      _ = innerHandlerCount shouldBe 1
    } yield succeed
  }

  it should "without a custom key" in {
    var onSnabbdomInsertCount    = 0
    var onSnabbdomPrePatchCount  = 0
    var onSnabbdomPostPatchCount = 0
    var onSnabbdomUpdateCount    = 0
    var onSnabbdomDestroyCount   = 0
    var innerHandlerCount        = 0
    var onDomMountList           = List.empty[Int]
    var onDomUnmountList         = List.empty[Int]
    var onDomUpdateList          = List.empty[Int]

    val innerHandler = Subject.replayLatest[Int]()
    val handler      = Subject.replayLatest[String]()
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
          onDomUpdate doAction { onDomUpdateList :+= c },
        )
      },
    )

    for {
      _      <- Outwatch.renderInto[IO]("#app", node)
      element = document.getElementById("strings")

      _ = element.innerHTML shouldBe ""
      _ = onSnabbdomInsertCount shouldBe 0
      _ = onSnabbdomPrePatchCount shouldBe 0
      _ = onSnabbdomPostPatchCount shouldBe 0
      _ = onSnabbdomUpdateCount shouldBe 0
      _ = onSnabbdomDestroyCount shouldBe 0
      _ = onDomMountList shouldBe Nil
      _ = onDomUnmountList shouldBe Nil
      _ = onDomUpdateList shouldBe Nil

      _ <- handler.onNextIO("heinz") *> IO.cede
      _  = element.innerHTML shouldBe "<div>heinz</div>"
      _  = onSnabbdomInsertCount shouldBe 1
      _  = onSnabbdomPrePatchCount shouldBe 0
      _  = onSnabbdomPostPatchCount shouldBe 0
      _  = onSnabbdomUpdateCount shouldBe 0
      _  = onSnabbdomDestroyCount shouldBe 0
      _  = onDomMountList shouldBe List(1)
      _  = onDomUnmountList shouldBe Nil
      _  = onDomUpdateList shouldBe Nil

      _ <- handler.onNextIO("golum") *> IO.cede
      _  = element.innerHTML shouldBe "<div>golum</div>"
      _  = onSnabbdomInsertCount shouldBe 1
      _  = onSnabbdomPrePatchCount shouldBe 1
      _  = onSnabbdomPostPatchCount shouldBe 1
      _  = onSnabbdomUpdateCount shouldBe 1
      _  = onSnabbdomDestroyCount shouldBe 0
      _  = onDomMountList shouldBe List(1, 2)
      _  = onDomUnmountList shouldBe List(1)
      _  = onDomUpdateList shouldBe Nil

      _ <- handler.onNextIO("dieter") *> IO.cede
      _  = element.innerHTML shouldBe "<div>dieter</div>"
      _  = onSnabbdomInsertCount shouldBe 1
      _  = onSnabbdomPrePatchCount shouldBe 2
      _  = onSnabbdomPostPatchCount shouldBe 2
      _  = onSnabbdomUpdateCount shouldBe 2
      _  = onSnabbdomDestroyCount shouldBe 0
      _  = onDomMountList shouldBe List(1, 2, 3)
      _  = onDomUnmountList shouldBe List(1, 2)
      _  = onDomUpdateList shouldBe Nil

      _ <- handler.onNextIO("dieter") *> IO.cede
      _  = element.innerHTML shouldBe "<div>dieter</div>"
      _  = onSnabbdomInsertCount shouldBe 1
      _  = onSnabbdomPrePatchCount shouldBe 3
      _  = onSnabbdomPostPatchCount shouldBe 3
      _  = onSnabbdomUpdateCount shouldBe 3
      _  = onSnabbdomDestroyCount shouldBe 0
      _  = onDomMountList shouldBe List(1, 2, 3, 4)
      _  = onDomUnmountList shouldBe List(1, 2, 3)
      _  = onDomUpdateList shouldBe Nil

      _ <- handler.onNextIO("peter") *> IO.cede
      _  = element.innerHTML shouldBe "<div>peter</div>"
      _  = onSnabbdomInsertCount shouldBe 1
      _  = onSnabbdomPrePatchCount shouldBe 4
      _  = onSnabbdomPostPatchCount shouldBe 4
      _  = onSnabbdomUpdateCount shouldBe 4
      _  = onSnabbdomDestroyCount shouldBe 0
      _  = onDomMountList shouldBe List(1, 2, 3, 4, 5)
      _  = onDomUnmountList shouldBe List(1, 2, 3, 4)
      _  = onDomUpdateList shouldBe Nil

      _ <- otherHandler.onNextIO("hi") *> IO.cede
      _  = element.innerHTML shouldBe "<div>hi</div><div>peter</div>"
      _  = onSnabbdomInsertCount shouldBe 1
      _  = onSnabbdomPrePatchCount shouldBe 4
      _  = onSnabbdomPostPatchCount shouldBe 4
      _  = onSnabbdomUpdateCount shouldBe 4
      _  = onSnabbdomDestroyCount shouldBe 0
      _  = onDomMountList shouldBe List(1, 2, 3, 4, 5)
      _  = onDomUnmountList shouldBe List(1, 2, 3, 4)
      _  = onDomUpdateList shouldBe Nil

      _ = innerHandlerCount shouldBe 0

      _ <- innerHandler.onNextIO(0) *> IO.cede
      _  = element.innerHTML shouldBe "<div>hi</div><div>peter0</div>"
      _  = onSnabbdomInsertCount shouldBe 1
      _  = onSnabbdomPrePatchCount shouldBe 5
      _  = onSnabbdomPostPatchCount shouldBe 5
      _  = onSnabbdomUpdateCount shouldBe 5
      _  = onSnabbdomDestroyCount shouldBe 0
      _  = onDomMountList shouldBe List(1, 2, 3, 4, 5)
      _  = onDomUnmountList shouldBe List(1, 2, 3, 4)
      _  = onDomUpdateList shouldBe List(5)

      _ = innerHandlerCount shouldBe 1
    } yield succeed
  }
}
