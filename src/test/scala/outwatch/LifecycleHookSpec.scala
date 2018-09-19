package outwatch

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

    val node = sink.map { sink =>
      div(onSnabbdomInsert --> sink)
    }

    switch shouldBe false

    node.flatMap(OutWatch.renderInto("#app", _)).unsafeRunSync()

    switch shouldBe true

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

    val node = for {
      sink <- sink
      sink2 <- sink2
      node = div(onSnabbdomInsert --> sink)(onSnabbdomInsert --> sink2)
    } yield node

    switch shouldBe false
    switch2 shouldBe false

    node.flatMap(OutWatch.renderInto("#app", _)).unsafeRunSync()

    switch shouldBe true
    switch2 shouldBe true

  }


  "Destruction hooks"  should "be called correctly" in {

    var switch = false
    val sink = Sink.create{(_: Element) =>
      switch = true
      Continue
    }

    val node = sink.map { sink =>
      div(Observable(span(onSnabbdomDestroy --> sink), div("Hasdasd")))
    }

    switch shouldBe false

    node.flatMap(OutWatch.renderInto("#app", _)).unsafeRunSync()

    switch shouldBe true

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

    val node = for {
      sink <- sink
      sink2 <- sink2
      node = div(Observable(span(onSnabbdomDestroy --> sink)(onSnabbdomDestroy --> sink2), div("Hasdasd")))
    } yield node

    switch shouldBe false
    switch2 shouldBe false

    node.flatMap(OutWatch.renderInto("#app", _)).unsafeRunSync()

    switch shouldBe true
    switch2 shouldBe true
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
    val node = for {
      sink1 <- sink1
      sink2 <- sink2
      node = div(message, onSnabbdomUpdate --> sink1)(onSnabbdomUpdate --> sink2)
    } yield node

    node.flatMap(OutWatch.renderInto("#app", _)).unsafeRunSync()
    switch1 shouldBe false
    switch2 shouldBe false

    message.onNext("wursi")
    switch1 shouldBe true
    switch2 shouldBe true
  }


  it should "be called correctly" in {

    var switch = false
    val sink = Sink.create{(_: (Element, Element)) =>
      switch = true
      Continue
    }

    val node = sink.map { sink =>
      div(Observable(span(onSnabbdomUpdate --> sink, "Hello"), span(onSnabbdomUpdate --> sink, "Hey")))
    }

    switch shouldBe false

    node.flatMap(OutWatch.renderInto("#app", _)).unsafeRunSync()

    switch shouldBe true

  }

  "Prepatch hooks" should "be called" in {

    var switch = false
    val sink = Sink.create{(_: (Option[Element], Option[Element])) =>
      switch = true
      Continue
    }

    val node = sink.map { sink =>
      div(Observable(span("Hello")), span(attributes.key := "1", onSnabbdomPrePatch --> sink, "Hey"))
    }

    switch shouldBe false

    node.flatMap(OutWatch.renderInto("#app", _)).unsafeRunSync()

    switch shouldBe true
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
    val node =  for {
      sink1 <- sink1
      sink2 <- sink2
      node = div(message, onSnabbdomPrePatch --> sink1)(onSnabbdomPrePatch --> sink2)
    } yield node

    node.flatMap(OutWatch.renderInto("#app", _)).unsafeRunSync()
    switch1 shouldBe false
    switch2 shouldBe false

    message.onNext("wursi")

    switch1 shouldBe true
    switch2 shouldBe true
  }

  "Postpatch hooks" should "be called" in {

    var switch = false
    val sink = Sink.create{(_: (Element, Element)) =>
      switch = true
      Continue
    }

    val node = sink.map { sink =>
      div(Observable.pure("message"), onSnabbdomPostPatch --> sink, "Hey")
    }

    switch shouldBe false

    node.flatMap(OutWatch.renderInto("#app", _)).unsafeRunSync()

    switch shouldBe true
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
    val node = for {
      sink1 <- sink1
      sink2 <- sink2
      node = div(message, onSnabbdomPostPatch --> sink1)(onSnabbdomPostPatch --> sink2)
    } yield node

    node.flatMap(OutWatch.renderInto("#app", _)).unsafeRunSync()
    switch1 shouldBe false
    switch2 shouldBe false

    message.onNext("wursi")

    switch1 shouldBe true
    switch2 shouldBe true
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
    val node = for {
      insertSink <- insertSink
      updateSink <- updateSink
      destroySink <- destroySink
      prepatchSink <- prepatchSink
      postpatchSink <- postpatchSink
      node = div(message,
        onSnabbdomInsert --> insertSink,
        onSnabbdomPrePatch --> prepatchSink,
        onSnabbdomUpdate --> updateSink,
        onSnabbdomPostPatch --> postpatchSink,
        onSnabbdomDestroy --> destroySink
      )
    } yield node

    hooks shouldBe empty

    node.flatMap(OutWatch.renderInto("#app", _)).unsafeRunSync()

    hooks.toList shouldBe List("insert")

    message.onNext("next")

    hooks.toList shouldBe List("insert", "prepatch", "update", "postpatch")
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
    val node = for {
      insertSink <- insertSink
      updateSink <- updateSink
      node = div("Hello", messageList.map(_.map(span(_))),
        onSnabbdomInsert --> insertSink,
        onSnabbdomUpdate --> updateSink
      )
    } yield node

    hooks shouldBe empty

    node.flatMap(OutWatch.renderInto("#app", _)).unsafeRunSync()

    hooks.toList shouldBe  List("insert")
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
    val node = for {
      insertSink <- insertSink
      updateSink <- updateSink
      destroySink <- destroySink
      node = div(span("Hello", onSnabbdomInsert --> insertSink, onSnabbdomUpdate --> updateSink, onSnabbdomDestroy --> destroySink),
        message.map(span(_))
      )
    } yield node

    hooks shouldBe empty

    node.flatMap(OutWatch.renderInto("#app", _)).unsafeRunSync()

    message.onNext("next")

    hooks.contains("destroy") shouldBe false
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
    val node = for {
      insertSink <- insertSink
      updateSink <- updateSink
      destroySink <- destroySink
      node = div(messageList.map(_.map(span(_))),
        span("Hello", onSnabbdomInsert --> insertSink, onSnabbdomUpdate --> updateSink, onSnabbdomDestroy --> destroySink)
      )
    } yield node

    hooks shouldBe empty

    node.flatMap(OutWatch.renderInto("#app", _)).unsafeRunSync()

    messageList.onNext(Seq("one"))

    messageList.onNext(Seq("one", "two"))

    hooks.count(_ == "insert") shouldBe 1
  }


  "Managed subscriptions" should "unsubscribe on destroy" in {

    val nodes = PublishSubject[VNode]

    var latest = ""
    val sink = Sink.create { (elem: String) =>
      latest = elem
      Continue
    }

    val sub = PublishSubject[String]

    val node = sink.map { sink =>
      div(nodes.startWith(Seq(
        span(managed(sink <-- sub))
      )))
    }

    node.flatMap(OutWatch.renderInto("#app", _)).unsafeRunSync()

    latest shouldBe ""

    sub.onNext("first")
    latest shouldBe "first"

    nodes.onNext(div()) // this triggers child destroy and subscription cancelation

    sub.onNext("second")
    latest shouldBe "first"
  }


  "Hooks" should "support emitter operations" in {

    val operations = mutable.ArrayBuffer.empty[String]

    val sink = Sink.create { (op: String) =>
      operations += op
      Continue
    }

    val divTagName = onSnabbdomInsert.map(_.tagName.toLowerCase).filter(_ == "div")

    val node = sink.map { sink =>
      div(onSnabbdomInsert("insert") --> sink,
        div(divTagName --> sink),
        span(divTagName --> sink)
      )
    }

    node.flatMap(OutWatch.renderInto("#app", _)).unsafeRunSync()

    operations.toList shouldBe List("div", "insert")

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

    val innerHandler = Handler.create[Int].unsafeRunSync
    val handler = Handler.create[(String, String)].unsafeRunSync
    val otherHandler = Handler.create[String].unsafeRunSync

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
          onSnabbdomInsert --> sideEffect { onSnabbdomInsertCount += 1 },
          onSnabbdomPrePatch --> sideEffect { onSnabbdomPrePatchCount += 1 },
          onSnabbdomPostPatch --> sideEffect { onSnabbdomPostPatchCount += 1 },
          onSnabbdomUpdate --> sideEffect { onSnabbdomUpdateCount += 1 },
          onSnabbdomDestroy --> sideEffect { onSnabbdomDestroyCount += 1 },
          onDomMount --> sideEffect { onDomMountList :+= c },
          onDomUnmount --> sideEffect { onDomUnmountList :+= c },
          onDomUpdate --> sideEffect { onDomUpdateList :+= c }
        )
      }
    )

    OutWatch.renderInto("#app", node).unsafeRunSync()
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
    //    onDomUnmountList shouldBe List(1)
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
    onSnabbdomPrePatchCount shouldBe 3
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
    onSnabbdomPrePatchCount shouldBe 4
    onSnabbdomPostPatchCount shouldBe 3
    onSnabbdomUpdateCount shouldBe 3
    onSnabbdomDestroyCount shouldBe 2
    onDomMountList shouldBe List(1, 2, 3, 4, 5)
    onDomUnmountList shouldBe List(1, 2, 3, 4)
    onDomUpdateList shouldBe List(5)

    innerHandlerCount shouldBe 1
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

    val innerHandler = Handler.create[Int].unsafeRunSync
    val handler = Handler.create[String].unsafeRunSync
    val otherHandler = Handler.create[String].unsafeRunSync

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
          onSnabbdomInsert --> sideEffect { onSnabbdomInsertCount += 1 },
          onSnabbdomPrePatch --> sideEffect { onSnabbdomPrePatchCount += 1 },
          onSnabbdomPostPatch --> sideEffect { onSnabbdomPostPatchCount += 1 },
          onSnabbdomUpdate --> sideEffect { onSnabbdomUpdateCount += 1 },
          onSnabbdomDestroy --> sideEffect { onSnabbdomDestroyCount += 1 },
          onDomMount --> sideEffect { onDomMountList :+= c },
          onDomUnmount --> sideEffect { onDomUnmountList :+= c },
          onDomUpdate --> sideEffect { onDomUpdateList :+= c }
        )
      }
    )

    OutWatch.renderInto("#app", node).unsafeRunSync()
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
    onSnabbdomInsertCount shouldBe 2
    onSnabbdomPrePatchCount shouldBe 0
    onSnabbdomPostPatchCount shouldBe 0
    onSnabbdomUpdateCount shouldBe 0
    onSnabbdomDestroyCount shouldBe 1
    onDomMountList shouldBe List(1,2)
    onDomUnmountList shouldBe List(1)
    onDomUpdateList shouldBe Nil

    handler.onNext("dieter")
    element.innerHTML shouldBe "<div>dieter</div>"
    onSnabbdomInsertCount shouldBe 3
    onSnabbdomPrePatchCount shouldBe 0
    onSnabbdomPostPatchCount shouldBe 0
    onSnabbdomUpdateCount shouldBe 0
    onSnabbdomDestroyCount shouldBe 2
    onDomMountList shouldBe List(1,2,3)
    onDomUnmountList shouldBe List(1,2)
    onDomUpdateList shouldBe Nil

    handler.onNext("dieter")
    element.innerHTML shouldBe "<div>dieter</div>"
    onSnabbdomInsertCount shouldBe 4
    onSnabbdomPrePatchCount shouldBe 0
    onSnabbdomPostPatchCount shouldBe 0
    onSnabbdomUpdateCount shouldBe 0
    onSnabbdomDestroyCount shouldBe 3
    onDomMountList shouldBe List(1,2,3,4)
    onDomUnmountList shouldBe List(1,2,3)
    onDomUpdateList shouldBe Nil

    handler.onNext("peter")
    element.innerHTML shouldBe "<div>peter</div>"
    onSnabbdomInsertCount shouldBe 5
    onSnabbdomPrePatchCount shouldBe 0
    onSnabbdomPostPatchCount shouldBe 0
    onSnabbdomUpdateCount shouldBe 0
    onSnabbdomDestroyCount shouldBe 4
    onDomMountList shouldBe List(1,2,3,4,5)
    onDomUnmountList shouldBe List(1,2,3,4)
    onDomUpdateList shouldBe Nil

    otherHandler.onNext("hi")
    element.innerHTML shouldBe "<div>hi</div><div>peter</div>"
    onSnabbdomInsertCount shouldBe 5
    onSnabbdomPrePatchCount shouldBe 1
    onSnabbdomPostPatchCount shouldBe 0
    onSnabbdomUpdateCount shouldBe 0
    onSnabbdomDestroyCount shouldBe 4
    onDomMountList shouldBe List(1, 2, 3, 4, 5)
    onDomUnmountList shouldBe List(1, 2, 3, 4)
    onDomUpdateList shouldBe Nil

    innerHandlerCount shouldBe 0

    innerHandler.onNext(0)
    element.innerHTML shouldBe "<div>hi</div><div>peter0</div>"
    onSnabbdomInsertCount shouldBe 5
    onSnabbdomPrePatchCount shouldBe 2
    onSnabbdomPostPatchCount shouldBe 1
    onSnabbdomUpdateCount shouldBe 1
    onSnabbdomDestroyCount shouldBe 4
    onDomMountList shouldBe List(1, 2, 3, 4, 5)
    onDomUnmountList shouldBe List(1, 2, 3, 4)
    onDomUpdateList shouldBe List(5)

    innerHandlerCount shouldBe 1
  }
}
