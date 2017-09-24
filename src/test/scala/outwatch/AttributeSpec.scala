package outwatch

import outwatch.dom._

class AttributeSpec extends UnitSpec {

  "data attribute" should "correctly render only data" in {
    val node = input(data := "bar").asProxy

    node.data.attrs.toList should contain theSameElementsAs List(
      "data" -> "bar"
    )
  }

  it should "correctly render expanded data with dynamic content" in {
    val node = input(data.foo := "bar").asProxy

    node.data.attrs.toList should contain theSameElementsAs List(
      "data-foo" -> "bar"
    )
  }

  "optional attributes" should "correctly render" in {
    val node = input(
      data.foo :=? Option("bar"),
      data.bar :=? Option.empty[String]
    ).asProxy

    node.data.attrs.toList should contain theSameElementsAs List(
      "data-foo" -> "bar"
    )
  }

  "apply on vtree" should "correctly merge attributes" in {
    val node = input(
      data := "bar",
      data.gurke := "franz"
    )(
      data := "buh",
      data.tomate := "gisela"
    ).asProxy

    node.data.attrs.toList should contain theSameElementsAs List(
      "data" -> "buh",
      "data-gurke" -> "franz",
      "data-tomate" -> "gisela"
    )
  }

  it should "correctly merge styles" in {
    val node = input(
      Style("color", "red"),
      Style("font-size", "5px")
    )(
      Style("color", "blue"),
      Style("border", "1px solid black")
    ).asProxy

    node.data.style.toList should contain theSameElementsAs List(
      ("color", "blue"),
      ("font-size", "5px"),
      ("border", "1px solid black")
    )
  }

  it should "correctly merge keys" in {
    val node = input( dom.key := "bumm")( dom.key := "klapp").asProxy
    node.data.key.toList should contain theSameElementsAs List("klapp")

    val node2 = input()( dom.key := "klapp").asProxy
    node2.data.key.toList should contain theSameElementsAs List("klapp")

    val node3 = input( dom.key := "bumm")().asProxy
    node3.data.key.toList should contain theSameElementsAs List("bumm")
  }

  "style attribute" should "render correctly" in {
    val node = input(Style("color", "red")).asProxy

    node.data.style.toList should contain theSameElementsAs List(
      "color" -> "red"
    )
  }
}
