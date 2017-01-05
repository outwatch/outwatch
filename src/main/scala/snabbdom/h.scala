package snabbdom

import org.scalajs.dom._
import org.scalajs.dom.raw.HTMLInputElement

import scala.scalajs.js
import scala.scalajs.js.|

@js.native
object h extends js.Object {
  def apply(nodeType: String, dataObject: DataObject, children: String | js.Array[_ <: Any]): VNodeProxy = js.native
}

@js.native
trait DataObject extends js.Object {
  val attrs: js.Dictionary[String] = js.native
  val on: js.Dictionary[js.Function1[Event ,Unit]] = js.native
  val hook: js.Dynamic = js.native
}

object DataObject {
  def apply(attrs: js.Dictionary[String], on: js.Dictionary[js.Function1[Event,Unit]]): DataObject = {
    js.Dynamic.literal(attrs = attrs, on = on).asInstanceOf[DataObject]
  }

  def createWithHooks(attrs: js.Dictionary[String], on: js.Dictionary[js.Function1[Event,Unit]],
                      insert: js.Function1[VNodeProxy,Unit], destroy: js.Function1[VNodeProxy,Unit]): DataObject = {
    js.Dynamic.literal(
      attrs = attrs,
      on = on,
      hook = js.Dynamic.literal(insert= insert, destroy= destroy)
    ).asInstanceOf[DataObject]
  }

  def createWithValue(attrs: js.Dictionary[String], on: js.Dictionary[js.Function1[Event,Unit]],
                      insert: js.Function1[VNodeProxy,Unit], destroy: js.Function1[VNodeProxy,Unit]): DataObject = {
    js.Dynamic.literal(
      attrs= attrs,
      on= on,
      hook=js.Dynamic.literal(insert= insert, destroy= destroy, update= updateHook)
    ).asInstanceOf[DataObject]
  }

  lazy val updateHook: js.Function2[VNodeProxy, VNodeProxy, Unit] = (old: VNodeProxy, node: VNodeProxy) => {
    node.elm.foreach(input => {
      if (input.value != input.getAttribute("value")) {
        input.value = input.getAttribute("value")
      }
    })
  }

  def updateAttributes(obj: DataObject, attrs: Seq[(String, String)]): DataObject = {
    import scala.scalajs.js.JSConverters._

    val newProps = (obj.attrs ++ attrs).toJSDictionary
    js.Dynamic.literal(attrs = newProps, on = obj.on, hook = obj.hook).asInstanceOf[DataObject]
  }
}

object patch {
  lazy val p = snabbdom.init(js.Array(snabbdom_class,snabbdom_eventlisteners,snabbdom_attributes, snabbdom_props))
  def apply(firstNode: org.scalajs.dom.raw.Element | VNodeProxy, vNode: VNodeProxy) = p(firstNode,vNode)
}

@js.native
trait VNodeProxy extends js.Object {
  val elm: js.UndefOr[HTMLInputElement]
  val data: DataObject
  val children: js.Array[VNodeProxy]
  val sel: String
}


@js.native
object snabbdom extends js.Object {
  def init(args: js.Array[Any]): js.Function2[Any | VNodeProxy, VNodeProxy, Unit] = js.native
}

@js.native
object snabbdom_class extends js.Object

@js.native
object snabbdom_eventlisteners extends js.Object

@js.native
object snabbdom_attributes extends js.Object

@js.native
object snabbdom_props extends js.Object

@js.native
object snabbdom_style extends js.Object
