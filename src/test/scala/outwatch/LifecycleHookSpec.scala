package outwatch

import cats.effect.IO
import org.scalajs.dom._
import org.scalatest.BeforeAndAfterEach
import outwatch.dom._
import rxscalajs.{Observable, Subject}

import scala.collection.mutable

class LifecycleHookSpec extends UnitSpec with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    val root = document.createElement("div")
    root.id = "app"
    document.body.appendChild(root)
    ()
  }

  override def afterEach(): Unit = {
    document.body.innerHTML = ""
  }

  "Insertion hooks" should "be called correctly" in {

    var switch = false
    val sink = Sink.create((_: Element) => IO { switch = true })

    val node = div(onInsert --> sink)

    switch shouldBe false

    OutWatch.renderInto("#app", node).unsafeRunSync()

    switch shouldBe true

  }

  it should "be called correctly on merged nodes" in {
    var switch = false
    val sink = Sink.create((_: Element) => IO{switch = true})
    var switch2 = false
    val sink2 = Sink.create((_: Element) => IO{switch2 = true})

    val node = div(onInsert --> sink)(onInsert --> sink2)

    switch shouldBe false
    switch2 shouldBe false

    OutWatch.renderInto("#app", node).unsafeRunSync()

    switch shouldBe true
    switch2 shouldBe true

  }


  "Destruction hooks"  should "be called correctly" in {

    var switch = false
    val sink = Sink.create((_: Element) => IO { switch = true })

    val node = div(child <-- Observable.of(span(onDestroy --> sink), div("Hasdasd")))

    switch shouldBe false

    OutWatch.renderInto("#app", node).unsafeRunSync()

    switch shouldBe true

  }

  it should "be called correctly on merged nodes" in {

    var switch = false
    val sink = Sink.create((_: Element) => IO{switch = true})
    var switch2 = false
    val sink2 = Sink.create((_: Element) => IO{switch2 = true})

    val node = div(child <-- Observable.of(span(onDestroy --> sink)(onDestroy --> sink2), div("Hasdasd")))

    switch shouldBe false
    switch2 shouldBe false

    OutWatch.renderInto("#app", node).unsafeRunSync()

    switch shouldBe true
    switch2 shouldBe true
  }

  "Update hooks" should "be called correctly on merged nodes" in {
    var switch1 = false
    val sink1 = Sink.create((_: (Element, Element)) => IO{switch1 = true})
    var switch2 = false
    val sink2 = Sink.create((_: (Element, Element)) => IO{switch2 = true})

    val message = Subject[String]()
    val node = div(child <-- message, onUpdate --> sink1)(onUpdate --> sink2)

    OutWatch.renderInto("#app", node).unsafeRunSync()
    switch1 shouldBe false
    switch2 shouldBe false

    message.next("wursi")
    switch1 shouldBe true
    switch2 shouldBe true
  }


  it should "be called correctly" in {

    var switch = false
    val sink = Sink.create((_: (Element, Element)) => IO { switch = true })

    val node = div(child <-- Observable.of(span(onUpdate --> sink, "Hello"), span(onUpdate --> sink, "Hey")))

    switch shouldBe false

    OutWatch.renderInto("#app", node).unsafeRunSync()

    switch shouldBe true

  }

  "Prepatch hooks" should "be called" in {

    var switch = false
    val sink = Sink.create((_: (Option[Element], Option[Element])) => IO {
      switch = true
    })

    val node = div(child <-- Observable.of(span("Hello")), span(outwatch.dom.key := "1", onPrepatch --> sink, "Hey"))

    switch shouldBe false

    OutWatch.renderInto("#app", node).unsafeRunSync()

    switch shouldBe true
  }

  it should "be called correctly on merged nodes" in {
    var switch1 = false
    val sink1 = Sink.create((_: (Option[Element], Option[Element])) => IO {
      switch1 = true
    })
    var switch2 = false
    val sink2 = Sink.create((_: (Option[Element], Option[Element])) => IO {
      switch2 = true
    })
    val message = Subject[String]()
    val node = div(child <-- message, onPrepatch --> sink1)(onPrepatch --> sink2)

    OutWatch.renderInto("#app", node).unsafeRunSync()
    switch1 shouldBe false
    switch2 shouldBe false

    message.next("wursi")

    switch1 shouldBe true
    switch2 shouldBe true
  }

  "Postpatch hooks" should "be called" in {

    var switch = false
    val sink = Sink.create((_: (Element, Element)) => IO {
      switch = true
    })

    val node = div(child <-- Observable.of("message"), onPostpatch --> sink, "Hey")

    switch shouldBe false

    OutWatch.renderInto("#app", node).unsafeRunSync()

    switch shouldBe true
  }


  it should "be called correctly on merged nodes" in {
    var switch1 = false
    val sink1 = Sink.create((_: (Element, Element)) => IO {
      switch1 = true
    })
    var switch2 = false
    val sink2 = Sink.create((_: (Element, Element)) => IO {
      switch2 = true
    })
    val message = Subject[String]()
    val node = div(child <-- message, onPostpatch --> sink1)(onPostpatch --> sink2)

    OutWatch.renderInto("#app", node).unsafeRunSync()
    switch1 shouldBe false
    switch2 shouldBe false

    message.next("wursi")

    switch1 shouldBe true
    switch2 shouldBe true
  }


  "Hooks" should "be called in the correct order for modified node" in {
    val hooks = mutable.ArrayBuffer.empty[String]
    val insertSink = Sink.create((_: Element) =>
      IO {
        hooks += "insert"
        ()
      }
    )
    val prepatchSink = Sink.create((_: (Option[Element], Option[Element])) =>
      IO {
        hooks += "prepatch"
        ()
      }
    )
    val updateSink = Sink.create((_: (Element, Element)) =>
      IO {
        hooks += "update"
        ()
      }
    )
    val postpatchSink = Sink.create((_: (Element, Element)) =>
      IO {
        hooks += "postpatch"
        ()
      }
    )
    val destroySink = Sink.create((_: Element) =>
      IO {
        hooks += "destroy"
        ()
      }
    )

    val message = Subject[String]()
    val node = div(child <-- message,
      onInsert --> insertSink,
      onPrepatch --> prepatchSink,
      onUpdate --> updateSink,
      onPostpatch --> postpatchSink,
      onDestroy --> destroySink
    )

    hooks shouldBe empty

    OutWatch.renderInto("#app", node).unsafeRunSync()

    hooks.toList shouldBe List("insert")

    message.next("next")

    hooks.toList shouldBe List("insert", "prepatch", "update", "postpatch")
  }

  "Empty single children receiver" should "not trigger node update on render" in {
    val hooks = mutable.ArrayBuffer.empty[String]
    val insertSink = Sink.create((_: Element) =>
      IO {
        hooks += "insert"
        ()
      }
    )
    val updateSink = Sink.create((_: (Element, Element)) =>
      IO {
        hooks += "update"
        ()
      }
    )

    val messageList = Subject[Seq[String]]()
    val node = div("Hello",  children <-- messageList.map(_.map(span(_))),
      onInsert --> insertSink,
      onUpdate --> updateSink
    )

    hooks shouldBe empty

    OutWatch.renderInto("#app", node).unsafeRunSync()

    hooks.toList shouldBe  List("insert")
  }

  "Static child nodes" should "not be destroyed and inserted when child stream emits" in {
    val hooks = mutable.ArrayBuffer.empty[String]
    val insertSink = Sink.create((_: Element) =>
      IO {
        hooks += "insert"
        ()
      }
    )
    val updateSink = Sink.create((_: (Element, Element)) =>
      IO {
        hooks += "update"
        ()
      }
    )
    val destroySink = Sink.create((_: Element) =>
      IO {
        hooks += "destroy"
        ()
      }
    )

    val message = Subject[String]()
    val node = div(span("Hello", onInsert --> insertSink, onUpdate --> updateSink, onDestroy --> destroySink),
      child <-- message.map(span(_))
    )

    hooks shouldBe empty

    OutWatch.renderInto("#app", node).unsafeRunSync()

    message.next("next")

    hooks.contains("destroy") shouldBe false
  }

  it should "be only inserted once when children stream emits" in {
    val hooks = mutable.ArrayBuffer.empty[String]
    val insertSink = Sink.create((_: Element) =>
      IO {
        hooks += "insert"
        ()
      }
    )
    val updateSink = Sink.create((_: (Element, Element)) =>
      IO {
        hooks += "update"
        ()
      }
    )
    val destroySink = Sink.create((_: Element) =>
      IO {
        hooks += "destroy"
        ()
      }
    )

    val messageList = Subject[Seq[String]]()
    val node = div(children <-- messageList.map(_.map(span(_))),
      span("Hello", onInsert --> insertSink, onUpdate --> updateSink, onDestroy --> destroySink)
    )

    hooks shouldBe empty

    OutWatch.renderInto("#app", node).unsafeRunSync()

    messageList.next(Seq("one"))

    messageList.next(Seq("one", "two"))

    hooks.count(_ == "insert") shouldBe 1
  }

}
