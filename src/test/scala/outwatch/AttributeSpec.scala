package outwatch

import outwatch.dom._

class AttributeSpec extends UnitSpec {

  "data attribute" should "correctly render only data" in {
    val node = input(data := "bar").asProxy

    node.data.attrs.iterator.contains("data" -> "bar") shouldBe true
    node.data.attrs.size shouldBe 1
  }

  it should "correctly render expanded data with dynamic content" in {
    val node = input(data.foo := "bar").asProxy

    node.data.attrs.iterator.contains("data-foo" -> "bar") shouldBe true
    node.data.attrs.size shouldBe 1
  }

}
