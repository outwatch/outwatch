package outwatch

import cats.effect.IO
import org.scalajs.dom._
import org.scalatest.BeforeAndAfterEach
import outwatch.dom._
import rxscalajs.{Observable, Subject}

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

    val node = div(insert --> sink)

    switch shouldBe false

    OutWatch.render("#app", node).unsafeRunSync()

    switch shouldBe true

  }

  it should "be called correctly on merged nodes" in {
    var switch = false
    val sink = Sink.create((_: Element) => IO{switch = true})
    var switch2 = false
    val sink2 = Sink.create((_: Element) => IO{switch2 = true})

    val node = div(insert --> sink)(insert --> sink2)

    switch shouldBe false
    switch2 shouldBe false

    OutWatch.render("#app", node).unsafeRunSync()

    switch shouldBe true
    switch2 shouldBe true

  }

  "Destruction hooks" should "be called correctly on merged nodes" in {

    var switch = false
    val sink = Sink.create((_: Element) => IO{switch = true})
    var switch2 = false
    val sink2 = Sink.create((_: Element) => IO{switch2 = true})

    val node = div(child <-- Observable.of[VNode](span(destroy --> sink)(destroy --> sink2), "Hasdasd"))

    switch shouldBe false
    switch2 shouldBe false

    OutWatch.render("#app", node).unsafeRunSync()

    switch shouldBe true
    switch2 shouldBe true

  }

  it should "be called correctly" in {

    var switch = false
    val sink = Sink.create((_: Element) => IO { switch = true })

    val node = div(child <-- Observable.of[VNode](span(destroy --> sink), "Hasdasd"))

    switch shouldBe false

    OutWatch.render("#app", node).unsafeRunSync()

    switch shouldBe true

  }

  "Update hooks" should "be called correctly on merged nodes" in {
    var switch1 = false
    val sink1 = Sink.create((_: (Element, Element)) => IO{switch1 = true})
    var switch2 = false
    val sink2 = Sink.create((_: (Element, Element)) => IO{switch2 = true})

    val message = Subject[String]()
    val node = div(child <-- message, update --> sink1)(update --> sink2)

    OutWatch.render("#app", node).unsafeRunSync()
    switch1 shouldBe false
    switch2 shouldBe false

    message.next("wursi")
    switch1 shouldBe true
    switch2 shouldBe true
  }


  it should "be called correctly" in {

    var switch = false
    val sink = Sink.create((_: (Element, Element)) => IO { switch = true })

    val node = div(child <-- Observable.of(span(update --> sink, "Hello"), span(update --> sink, "Hey")))

    switch shouldBe false

    OutWatch.render("#app", node).unsafeRunSync()

    switch shouldBe true

  }

}
