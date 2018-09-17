package outwatch

import cats.effect.IO
import outwatch.dom._
import outwatch.dom.dsl._
import snabbdom.VNodeProxy
import scala.scalajs.js

class AttributeSpec extends JSDomAsyncSpec {

  "class attributes" should "be accumulated" in {

    val node: IO[VNodeProxy] = input(
      className := "class1",
      cls := "class2"
    ).map(_.toSnabbdom)

    for(n <- node) yield {
      n.data.attrs.toList shouldBe List("class" -> "class1 class2")
    }

  }

  "custom attributes" should "be able to be accumulated" in {

    val node = input(
      attr("id").accum(",") := "foo1",
      attr("id").accum(",") := "foo2"
    ).map(_.toSnabbdom)

    for(n <- node) yield {
      n.data.attrs.toList shouldBe List("id" -> "foo1,foo2")
    }

  }

  "data attributes" should "be able to be accumulated" in {

    val node = input(
      data.foo.accum(",") := "foo1",
      data.foo.accum(",") := "foo2"
    ).map(_.toSnabbdom)

    for(n <- node) yield {
      n.data.attrs.toList shouldBe List("data-foo" -> "foo1,foo2")
    }

  }

  "data attribute" should "correctly render only Data" in {

    val node = input(
      data.geul := "bar",
      data.geuli.gurk := "barz"
    ).map(_.toSnabbdom)

    for(n <- node) yield {
      n.data.attrs.toList should contain theSameElementsAs List(
        "data-geul" -> "bar",
        "data-geuli-gurk" -> "barz"
      )
    }

  }

  it should "correctly render only expanded data with dynamic content" in {

    val node = input(
      dataAttr("geul") := "bar",
      dataAttr("geuli-gurk") := "barz"
    ).map(_.toSnabbdom)

    for(n <- node) yield {
      n.data.attrs.toList should contain theSameElementsAs List(
        "data-geul" -> "bar",
        "data-geuli-gurk" -> "barz"
      )
    }

  }

  it should "not compile data.without suffix" in {
    """input(data.:= "bar")""" shouldNot compile
  }

  //TODO: doesn't compile but test still fails
//   it should "not compile data without suffix" in {
//     """input(data := "bar")""" shouldNot compile
//   }

  "attr/prop/style" should "correctly render type" in {

    val node = tag("input")(
      attr("foo") := "foo",
      attr[Boolean]("boo", identity) := true,
      attr[Boolean]("yoo", x => if (x) "yes" else "no") := true,
      prop("bar") := "bar",
      prop("num") := 12,
      style("baz") := "baz",
      contentEditable := false,
      unselectable := false,
      disabled := false
    ).map(_.toSnabbdom)

    for(n <- node) yield {

      n.data.attrs.toList should contain theSameElementsAs List(
        "foo" -> "foo",
        "boo" -> true,
        "yoo" -> "yes",
        "contenteditable" -> "false",
        "unselectable" -> "off",
        "disabled" -> false
      )
      n.data.props.toList should contain theSameElementsAs List(
        "bar" -> "bar",
        "num" -> 12
      )
      n.data.style.toList should contain theSameElementsAs List(
        "baz" -> "baz"
      )
    }

  }

  "optional attributes" should "correctly render" in {

    val node = input(
      data.foo :=? Option("bar"),
      data.bar :=? Option.empty[String]
    ).map(_.toSnabbdom)

    for(n <- node) yield {
      n.data.attrs.toList should contain theSameElementsAs List(
        "data-foo" -> "bar"
      )
    }

  }

  "apply on vtree" should "correctly merge attributes" in {

    val node = input(
      data.a := "bar",
      data.a.gurke := "franz"
    )(
      data.a := "buh",
      data.a.tomate := "gisela"
    ).map(_.toSnabbdom)

    for(n <- node) yield {
      n.data.attrs.toList should contain theSameElementsAs List(
        "data-a" -> "buh",
        "data-a-gurke" -> "franz",
        "data-a-tomate" -> "gisela"
      )
    }

  }

  it should "correctly merge styles written with style" in {

    val node = input(
      style("color") := "red",
      fontSize:= "5px"
    )(
      style("color") := "blue",
      border := "1px solid black"
    ).map(_.toSnabbdom)

    for(n <- node) yield {
      n.data.style.toList should contain theSameElementsAs List(
        ("color", "blue"),
        ("font-size", "5px"),
        ("border", "1px solid black")
      )
    }

  }

  it should "correctly merge styles" in {

    val node = input(
      color.red,
      fontSize:= "5px"
    )(
      color.blue,
      border := "1px solid black"
    ).map(_.toSnabbdom)

    for(n <- node) yield {
      n.data.style.toList should contain theSameElementsAs List(
        ("color", "blue"),
        ("font-size", "5px"),
        ("border", "1px solid black")
      )
    }

  }

  it should "correctly merge keys" in {

    val node1 = input( attributes.key := "bumm")( attributes.key := "klapp").map(_.toSnabbdom)
    val node2 = input()( attributes.key := "klapp").map(_.toSnabbdom)
    val node3 = input( attributes.key := "bumm")().map(_.toSnabbdom)

    (for(n1 <- node1; n2 <- node2; n3 <- node3) yield {
      n1.data.key.toList should contain theSameElementsAs List("klapp")
      n2.data.key.toList should contain theSameElementsAs List("klapp")
      n3.data.key.toList should contain theSameElementsAs List("bumm")
    })
  }

  "style attribute" should "render correctly" in {

    val node = input(color.red).map(_.toSnabbdom)

    for(n <- node) yield {
      n.data.style.toList should contain theSameElementsAs List(
        "color" -> "red"
      )
    }
  }


  "extended styles" should "convert correctly" in {

    val node = div(
      opacity := 0,
      opacity.delayed := 1,
      opacity.remove := 0,
      opacity.destroy := 0
    ).map(_.toSnabbdom)

    for(n <- node) yield {
      n.data.style("opacity") shouldBe "0"
      n.data.style("delayed").asInstanceOf[js.Dictionary[String]].toMap shouldBe Map("opacity" -> "1")
      n.data.style("remove").asInstanceOf[js.Dictionary[String]].toMap shouldBe Map("opacity" -> "0")
      n.data.style("destroy").asInstanceOf[js.Dictionary[String]].toMap shouldBe Map("opacity" -> "0")
    }
  }

  "style accum" should "convert correctly" in {

    val node = div(
      transition := "transform .2s ease-in-out",
      transition.accum(",") := "opacity .2s ease-in-out"
    ).map(_.toSnabbdom)

    for(n <- node) yield {
      n.data.style.toMap shouldBe Map("transition" -> "transform .2s ease-in-out,opacity .2s ease-in-out")
    }

  }

  "svg" should "should work with tags and attributes" in {

    import outwatch.dom.dsl.svg._
    val node = svg(
      path(fill := "red", d := "M 100 100 L 300 100 L 200 300 z")
    ).map(_.toSnabbdom)

    for(n <- node) yield {
      n.children.get.head.data.attrs.toMap shouldBe Map("fill" -> "red", "d" -> "M 100 100 L 300 100 L 200 300 z")
    }
  }
}
