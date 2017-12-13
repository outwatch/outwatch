package outwatch

import cats.effect.IO
import monix.execution.Ack.Continue
import monix.reactive.Observable
import monix.reactive.subjects.PublishSubject
import org.scalajs.dom._
import outwatch.dom._

import scala.collection.mutable

class LifecycleHookSpec extends JSDomSpec {

  "Insertion hooks" should "be called correctly" in {

    var switch = false
    val sink = Sink.create((_: Element) => IO {
      switch = true
      Continue
    })

    val node = div(onInsert --> sink)

    switch shouldBe false

    OutWatch.renderInto("#app", node).unsafeRunSync()

    switch shouldBe true

  }

  it should "be called correctly on merged nodes" in {
    var switch = false
    val sink = Sink.create((_: Element) => IO{
      switch = true
      Continue
    })
    var switch2 = false
    val sink2 = Sink.create((_: Element) => IO{
      switch2 = true
      Continue
    })

    val node = div(onInsert --> sink)(onInsert --> sink2)

    switch shouldBe false
    switch2 shouldBe false

    OutWatch.renderInto("#app", node).unsafeRunSync()

    switch shouldBe true
    switch2 shouldBe true

  }


  "Destruction hooks"  should "be called correctly" in {

    var switch = false
    val sink = Sink.create((_: Element) => IO {
      switch = true
      Continue
    })

    val node = div(child <-- Observable(span(onDestroy --> sink), div("Hasdasd")))

    switch shouldBe false

    OutWatch.renderInto("#app", node).unsafeRunSync()

    switch shouldBe true

  }

  it should "be called correctly on merged nodes" in {

    var switch = false
    val sink = Sink.create((_: Element) => IO{
      switch = true
      Continue
    })
    var switch2 = false
    val sink2 = Sink.create((_: Element) => IO{
      switch2 = true
      Continue
    })

    val node = div(child <-- Observable(span(onDestroy --> sink)(onDestroy --> sink2), div("Hasdasd")))

    switch shouldBe false
    switch2 shouldBe false

    OutWatch.renderInto("#app", node).unsafeRunSync()

    switch shouldBe true
    switch2 shouldBe true
  }

  "Update hooks" should "be called correctly on merged nodes" in {
    var switch1 = false
    val sink1 = Sink.create((_: (Element, Element)) => IO{
      switch1 = true
      Continue
    })
    var switch2 = false
    val sink2 = Sink.create((_: (Element, Element)) => IO{
      switch2 = true
      Continue
    })

    val message = PublishSubject[String]
    val node = div(child <-- message, onUpdate --> sink1)(onUpdate --> sink2)

    OutWatch.renderInto("#app", node).unsafeRunSync()
    switch1 shouldBe false
    switch2 shouldBe false

    message.onNext("wursi")
    switch1 shouldBe true
    switch2 shouldBe true
  }


  it should "be called correctly" in {

    var switch = false
    val sink = Sink.create((_: (Element, Element)) => IO {
      switch = true
      Continue
    })

    val node = div(child <-- Observable(span(onUpdate --> sink, "Hello"), span(onUpdate --> sink, "Hey")))

    switch shouldBe false

    OutWatch.renderInto("#app", node).unsafeRunSync()

    switch shouldBe true

  }

  "Prepatch hooks" should "be called" in {

    var switch = false
    val sink = Sink.create((_: (Option[Element], Option[Element])) => IO {
      switch = true
      Continue
    })

    val node = div(child <-- Observable(span("Hello")), span(outwatch.dom.key := "1", onPrepatch --> sink, "Hey"))

    switch shouldBe false

    OutWatch.renderInto("#app", node).unsafeRunSync()

    switch shouldBe true
  }

  it should "be called correctly on merged nodes" in {
    var switch1 = false
    val sink1 = Sink.create((_: (Option[Element], Option[Element])) => IO {
      switch1 = true
      Continue
    })
    var switch2 = false
    val sink2 = Sink.create((_: (Option[Element], Option[Element])) => IO {
      switch2 = true
      Continue
    })
    val message = PublishSubject[String]()
    val node = div(child <-- message, onPrepatch --> sink1)(onPrepatch --> sink2)

    OutWatch.renderInto("#app", node).unsafeRunSync()
    switch1 shouldBe false
    switch2 shouldBe false

    message.onNext("wursi")

    switch1 shouldBe true
    switch2 shouldBe true
  }

  "Postpatch hooks" should "be called" in {

    var switch = false
    val sink = Sink.create((_: (Element, Element)) => IO {
      switch = true
      Continue
    })

    val node = div(child <-- Observable("message"), onPostpatch --> sink, "Hey")

    switch shouldBe false

    OutWatch.renderInto("#app", node).unsafeRunSync()

    switch shouldBe true
  }


  it should "be called correctly on merged nodes" in {
    var switch1 = false
    val sink1 = Sink.create((_: (Element, Element)) => IO {
      switch1 = true
      Continue
    })
    var switch2 = false
    val sink2 = Sink.create((_: (Element, Element)) => IO {
      switch2 = true
      Continue
    })
    val message = PublishSubject[String]()
    val node = div(child <-- message, onPostpatch --> sink1)(onPostpatch --> sink2)

    OutWatch.renderInto("#app", node).unsafeRunSync()
    switch1 shouldBe false
    switch2 shouldBe false

    message.onNext("wursi")

    switch1 shouldBe true
    switch2 shouldBe true
  }


  "Hooks" should "be called in the correct order for modified node" in {
    val hooks = mutable.ArrayBuffer.empty[String]
    val insertSink = Sink.create((_: Element) =>
      IO {
        hooks += "insert"
        Continue
      }
    )
    val prepatchSink = Sink.create((_: (Option[Element], Option[Element])) =>
      IO {
        hooks += "prepatch"
        Continue
      }
    )
    val updateSink = Sink.create((_: (Element, Element)) =>
      IO {
        hooks += "update"
        Continue
      }
    )
    val postpatchSink = Sink.create((_: (Element, Element)) =>
      IO {
        hooks += "postpatch"
        Continue
      }
    )
    val destroySink = Sink.create((_: Element) =>
      IO {
        hooks += "destroy"
        Continue
      }
    )

    val message = PublishSubject[String]()
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

    message.onNext("next")

    hooks.toList shouldBe List("insert", "prepatch", "update", "postpatch")
  }

  "Empty single children receiver" should "not trigger node update on render" in {
    val hooks = mutable.ArrayBuffer.empty[String]
    val insertSink = Sink.create((_: Element) =>
      IO {
        hooks += "insert"
        Continue
      }
    )
    val updateSink = Sink.create((_: (Element, Element)) =>
      IO {
        hooks += "update"
        Continue
      }
    )

    val messageList = PublishSubject[Seq[String]]()
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
        Continue
      }
    )
    val updateSink = Sink.create((_: (Element, Element)) =>
      IO {
        hooks += "update"
        Continue
      }
    )
    val destroySink = Sink.create((_: Element) =>
      IO {
        hooks += "destroy"
        Continue
      }
    )

    val message = PublishSubject[String]()
    val node = div(span("Hello", onInsert --> insertSink, onUpdate --> updateSink, onDestroy --> destroySink),
      child <-- message.map(span(_))
    )

    hooks shouldBe empty

    OutWatch.renderInto("#app", node).unsafeRunSync()

    message.onNext("next")

    hooks.contains("destroy") shouldBe false
  }

  it should "be only inserted once when children stream emits" in {
    val hooks = mutable.ArrayBuffer.empty[String]
    val insertSink = Sink.create((_: Element) =>
      IO {
        hooks += "insert"
        Continue
      }
    )
    val updateSink = Sink.create((_: (Element, Element)) =>
      IO {
        hooks += "update"
        Continue
      }
    )
    val destroySink = Sink.create((_: Element) =>
      IO {
        hooks += "destroy"
        Continue
      }
    )

    val messageList = PublishSubject[Seq[String]]()
    val node = div(children <-- messageList.map(_.map(span(_))),
      span("Hello", onInsert --> insertSink, onUpdate --> updateSink, onDestroy --> destroySink)
    )

    hooks shouldBe empty

    OutWatch.renderInto("#app", node).unsafeRunSync()

    messageList.onNext(Seq("one"))

    messageList.onNext(Seq("one", "two"))

    hooks.count(_ == "insert") shouldBe 1
  }

}
