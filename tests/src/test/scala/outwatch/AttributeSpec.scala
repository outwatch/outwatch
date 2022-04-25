package outwatch

import outwatch.dsl._
import outwatch.interpreter.SnabbdomOps

import scala.scalajs.js

class AttributeSpec extends JSDomSpec {

  "class attributes" should "be accumulated" in {

    val node = SnabbdomOps.toSnabbdom(div(
      className := "class1",
      cls := "class2"
    ), RenderConfig.ignoreError)

    node.data.get.attrs.get.toList shouldBe List("class" -> "class1 class2")
  }

  "style attributes" should "be accumulated" in {

    val node = SnabbdomOps.toSnabbdom(input(
      styleAttr := "height: 100px",
      styleAttr := "width: 200px"
    ), RenderConfig.ignoreError)

    node.data.get.attrs.get.toList shouldBe List("style" -> "height: 100px;width: 200px")
  }

  "custom attributes" should "be able to be accumulated" in {

    val node = SnabbdomOps.toSnabbdom(input(
      VModifier.attr("id").accum(",") := "foo1",
      VModifier.attr("id").accum(",") := "foo2"
    ), RenderConfig.ignoreError)

    node.data.get.attrs.get.toList shouldBe List("id" -> "foo1,foo2")
  }

  "data attributes" should "be able to be accumulated" in {

    val node = SnabbdomOps.toSnabbdom(input(
      data.foo.accum(",") := "foo1",
      data.foo.accum(",") := "foo2"
    ), RenderConfig.ignoreError)

    node.data.get.attrs.get.toList shouldBe List("data-foo" -> "foo1,foo2")
  }

  "data attribute" should "correctly render only Data" in {
    val node = SnabbdomOps.toSnabbdom(input(
      data.geul := "bar",
      data.geuli.gurk := "barz"
    ), RenderConfig.ignoreError)

    node.data.get.attrs.get.toList should contain theSameElementsAs List(
      "data-geul" -> "bar",
      "data-geuli-gurk" -> "barz"
    )
  }

  it should "correctly render only expanded data with dynamic content" in {
    val node = SnabbdomOps.toSnabbdom(input(
      dataAttr("geul") := "bar",
      dataAttr("geuli-gurk") := "barz"
    ), RenderConfig.ignoreError)

    node.data.get.attrs.get.toList should contain theSameElementsAs List(
      "data-geul" -> "bar",
      "data-geuli-gurk" -> "barz"
    )
  }

  it should "not compile data.without suffix" in {
    """input(data.:= "bar")""" shouldNot compile
  }

  //TODO: doesn't compile but test still fails
//   it should "not compile data without suffix" in {
//     """input(data := "bar")""" shouldNot compile
//   }

  "attr/prop/style" should "correctly render type" in {
    val node = SnabbdomOps.toSnabbdom(VNode.html("input")(
      VModifier.attr("foo") := "foo",
      VModifier.attr[Boolean]("boo", identity) := true,
      VModifier.attr[Boolean]("yoo", x => if (x) "yes" else "no") := true,
      VModifier.prop("bar") := "bar",
      VModifier.prop("num") := 12,
      VModifier.style("baz") := "baz",
      contentEditable := false,
      unselectable := false,
      disabled := false
    ), RenderConfig.ignoreError)

    node.data.get.attrs.get.toList should contain theSameElementsAs List(
      "foo" -> "foo",
      "boo" -> true,
      "yoo" -> "yes",
      "contenteditable" -> "false",
      "unselectable" -> "off",
      "disabled" -> false
    )
    node.data.get.props.get.toList should contain theSameElementsAs List(
      "bar" -> "bar",
      "num" -> 12
    )
    node.data.get.style.get.toList should contain theSameElementsAs List(
      "baz" -> "baz"
    )
  }

  "optional attributes" should "correctly render" in {
    val node = SnabbdomOps.toSnabbdom(input(
      cls := Option("bar"),
      styleAttr := Option.empty[String]
    ), RenderConfig.ignoreError)

    node.data.get.attrs.get.toList should contain theSameElementsAs List(
      "class" -> "bar"
    )
  }

  "optional attributes" should "correctly render with streams" in {
    val node = SnabbdomOps.toSnabbdom(input(
      cls <-- colibri.Observable(Option("bar")),
      styleAttr <-- colibri.Observable(Option.empty[String]),
      styleAttr <-- colibri.Subject.publish[Option[String]](),
      styleAttr <-- colibri.Subject.publish[String](),
    ), RenderConfig.ignoreError)

    node.data.get.attrs.get.toList should contain theSameElementsAs List(
      "class" -> "bar"
    )
  }

  "apply on vtree" should "correctly merge attributes" in {
    val node = SnabbdomOps.toSnabbdom(input(
      data.a := "bar",
      data.a.gurke := "franz"
    )(
      data.a := "buh",
      data.a.tomate := "gisela"
    ), RenderConfig.ignoreError)

    node.data.get.attrs.get.toList should contain theSameElementsAs List(
      "data-a" -> "buh",
      "data-a-gurke" -> "franz",
      "data-a-tomate" -> "gisela"
    )
  }

  it should "correctly merge styles written with style" in {
    val node = SnabbdomOps.toSnabbdom(input(
      VModifier.style("color") := "red",
      fontSize:= "5px"
    )(
      VModifier.style("color") := "blue",
      border := "1px solid black"
    ), RenderConfig.ignoreError)

    node.data.get.style.get.toList should contain theSameElementsAs List(
      ("color", "blue"),
      ("font-size", "5px"),
      ("border", "1px solid black")
    )
  }

  it should "correctly merge styles" in {
    val node = SnabbdomOps.toSnabbdom(input(
      color.red,
      fontSize:= "5px"
    )(
      color.blue,
      border := "1px solid black"
    ), RenderConfig.ignoreError)

    node.data.get.style.get.toList should contain theSameElementsAs List(
      ("color", "blue"),
      ("font-size", "5px"),
      ("border", "1px solid black")
    )
  }

  it should "correctly merge keys" in {

    val node = SnabbdomOps.toSnabbdom(input( attributes.key := "bumm")( attributes.key := "klapp"), RenderConfig.ignoreError)
    node.data.get.key.toOption shouldBe Some("klapp")

    val node2 = SnabbdomOps.toSnabbdom(input()( attributes.key := "klapp"), RenderConfig.ignoreError)
    node2.data.get.key.toOption shouldBe Some("klapp")

    val node3 = SnabbdomOps.toSnabbdom(input( attributes.key := "bumm")(), RenderConfig.ignoreError)
    node3.data.get.key.toOption shouldBe Some("bumm")
  }

  "style attribute" should "render correctly" in {
    val node = SnabbdomOps.toSnabbdom(input(color.red), RenderConfig.ignoreError)

    node.data.get.style.get.toList should contain theSameElementsAs List(
      "color" -> "red"
    )
  }


  "extended styles" should "convert correctly" in {
    val node = SnabbdomOps.toSnabbdom(div(
      opacity := 0,
      opacity.delayed := 1,
      opacity.remove := 0,
      opacity.destroy := 0
    ), RenderConfig.ignoreError)

    node.data.get.style.get("opacity") shouldBe "0"
    node.data.get.style.get("delayed").asInstanceOf[js.Dictionary[String]].toMap shouldBe Map("opacity" -> "1")
    node.data.get.style.get("remove").asInstanceOf[js.Dictionary[String]].toMap shouldBe Map("opacity" -> "0")
    node.data.get.style.get("destroy").asInstanceOf[js.Dictionary[String]].toMap shouldBe Map("opacity" -> "0")
  }

  "style accum" should "convert correctly" in {
    val node = SnabbdomOps.toSnabbdom(div(
      transition := "transform .2s ease-in-out",
      transition.accum(",") := "opacity .2s ease-in-out"
    ), RenderConfig.ignoreError)

    node.data.get.style.get.toMap shouldBe Map("transition" -> "transform .2s ease-in-out,opacity .2s ease-in-out")
  }

  "svg" should "should work with tags and attributes" in {
    import outwatch.dsl.svg._
    val node = SnabbdomOps.toSnabbdom(svg(
      path(fill := "red", d := "M 100 100 L 300 100 L 200 300 z")
    ), RenderConfig.ignoreError)

    node.children.get.head.data.get.attrs.get.toMap shouldBe Map("fill" -> "red", "d" -> "M 100 100 L 300 100 L 200 300 z")
  }
}
