package outwatch

import cats.effect.IO
import org.scalajs.dom._
import org.scalatest.BeforeAndAfterEach
import outwatch.dom._
import rxscalajs.Observable

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

  "Destruction hooks" should "be called correctly" in {

    var switch = false
    val sink = Sink.create((_: Element) => IO { switch = true })

    val node = div(child <-- Observable.of(span(destroy --> sink), "Hasdasd"))

    switch shouldBe false

    OutWatch.render("#app", node).unsafeRunSync()

    switch shouldBe true

  }

  "Update hooks" should "be called correctly" in {

    var switch = false
    val sink = Sink.create((_: (Element, Element)) => IO { switch = true })

    val node = div(child <-- Observable.of(span(update --> sink, "Hello"), span(update --> sink, "Hey")))

    switch shouldBe false

    OutWatch.render("#app", node).unsafeRunSync()

    switch shouldBe true

  }

}
