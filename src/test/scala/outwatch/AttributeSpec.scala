package outwatch

import cats.effect.IO
import outwatch.dom._

class AttributeSpec extends UnitSpec {

  "data attribute" should "correctly render only data" in {
    val node = input(data := "bar").map(_.asProxy).unsafeRunSync()

    node.data.attrs.toList should contain theSameElementsAs List(
      "data" -> "bar"
    )
  }

  it should "correctly render expanded data with dynamic content" in {
    val node = input(data.foo := "bar").map(_.asProxy).unsafeRunSync()

    node.data.attrs.toList should contain theSameElementsAs List(
      "data-foo" -> "bar"
    )
  }

  "optional attributes" should "correctly render" in {
    val node = input(
      data.foo :=? Option("bar"),
      data.bar :=? Option.empty[String]
    ).map(_.asProxy).unsafeRunSync()

    node.data.attrs.toList should contain theSameElementsAs List(
      "data-foo" -> "bar"
    )
  }

  "apply on vtree" should "correctly merge attributes" in {
    val node = input(data := "bar",
        data.gurke := "franz")(data := "buh", data.tomate := "gisela").map(_.asProxy).unsafeRunSync()

    node.data.attrs.toList should contain theSameElementsAs List(
        "data" -> "buh",
        "data-gurke" -> "franz",
        "data-tomate" -> "gisela"
    )
  }

  def style_(title: String, value: String): IO[Style] = IO.pure(Style(title: String, value: String))

  it should "correctly merge styles" in {
    val node = input(
      style_("color", "red"),
      style_("font-size", "5px")
    )(
      style_("color", "blue"),
      style_("border", "1px solid black")
    ).map(_.asProxy).unsafeRunSync()

    node.data.style.toList should contain theSameElementsAs List(
      ("color", "blue"),
      ("font-size", "5px"),
      ("border", "1px solid black")
    )
  }

  it should "correctly merge keys" in {
    val node = input( dom.key := "bumm")( dom.key := "klapp").map(_.asProxy).unsafeRunSync()
    node.data.key.toList should contain theSameElementsAs List("klapp")

    val node2 = input()( dom.key := "klapp").map(_.asProxy).unsafeRunSync()
    node2.data.key.toList should contain theSameElementsAs List("klapp")

    val node3 = input( dom.key := "bumm")().map(_.asProxy).unsafeRunSync()
    node3.data.key.toList should contain theSameElementsAs List("bumm")
  }

  "style attribute" should "render correctly" in {
    val node = input(style_("color", "red")).map(_.asProxy).unsafeRunSync()

    node.data.style.toList should contain theSameElementsAs List(
      "color" -> "red"
    )
  }
}
