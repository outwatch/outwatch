package snabbdom

import org.scalajs.dom._
import org.scalajs.dom.raw.HTMLInputElement

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSImport, ScalaJSDefined}
import scala.scalajs.js.|

@js.native
@JSImport("snabbdom/h", JSImport.Namespace, globalFallback = "h")
object hProvider extends js.Object {
  val default: hFunction = js.native
}

@js.native
trait hFunction extends js.Any {
  def apply(nodeType: String, dataObject: DataObject, children: String | js.Array[_ <: Any]): VNodeProxy = js.native
}

object h {
  def apply(nodeType: String, dataObject: DataObject, children: String | js.Array[_ <: Any]): VNodeProxy = {
    hProvider.default.apply(nodeType,dataObject,children)
  }
}


@ScalaJSDefined
trait DataObject extends js.Object {
  val attrs: js.Dictionary[String]
  val on: js.Dictionary[js.Function1[Event ,Unit]]
  val hook: js.Dynamic
  val key: js.UndefOr[String]
}

object DataObject {
  def apply(_attrs: js.Dictionary[String],
            _on: js.Dictionary[js.Function1[Event, Unit]]
           ): DataObject = {
    new DataObject {
      val attrs = _attrs
      val on = _on
      val hook = js.Dynamic.literal()
      val key = js.undefined
    }
  }


  def createWithHooks(_attrs: js.Dictionary[String],
                      _on: js.Dictionary[js.Function1[Event, Unit]],
                      insert: js.Function1[VNodeProxy, Unit],
                      destroy: js.Function1[VNodeProxy, Unit],
                      update: js.Function2[VNodeProxy, VNodeProxy, Unit],
                      _key: js.UndefOr[String]
                     ): DataObject = {
    new DataObject {
      val attrs = _attrs
      val on = _on
      val hook = js.Dynamic.literal(insert = insert, destroy = destroy, update = update)
      val key = _key
    }
  }


  def createWithValue(_attrs: js.Dictionary[String],
                      _on: js.Dictionary[js.Function1[Event, Unit]],
                      insert: js.Function1[VNodeProxy, Unit],
                      destroy: js.Function1[VNodeProxy, Unit],
                      update: js.Function2[VNodeProxy, VNodeProxy, Unit],
                      _key: js.UndefOr[String]
                     ): DataObject = {

    val uHook: js.Function2[VNodeProxy, VNodeProxy, Unit] = (old: VNodeProxy, node: VNodeProxy) => {
      update(old, node)
      updateHook(old, node)
    }

    new DataObject {
      val attrs = _attrs
      val on = _on
      val hook = js.Dynamic.literal(insert = insert, destroy = destroy, update = uHook)
      val key = _key
    }
  }

  lazy val updateHook: js.Function2[VNodeProxy, VNodeProxy, Unit] = (old: VNodeProxy, node: VNodeProxy) => {
    node.elm.foreach(elm => {
      val input = elm.asInstanceOf[HTMLInputElement]
      if (input.value != input.getAttribute("value")) {
        input.value = input.getAttribute("value")
      }
    })
  }

  def updateAttributes(obj: DataObject, attrs: Seq[(String, String)]): DataObject = {
    import scala.scalajs.js.JSConverters._

    val newAttrs = (obj.attrs ++ attrs).toJSDictionary

    new DataObject {
      val attrs = newAttrs
      val on = obj.on
      val hook = obj.hook
      val key = obj.key
    }
  }
}

object patch {
  lazy val p = Snabbdom.init(js.Array(
    SnabbdomClass.default,
    SnabbdomEventListeners.default,
    SnabbdomAttributes.default,
    SnabbdomProps.default
  ))
  def apply(firstNode: VNodeProxy, vNode: VNodeProxy) = p(firstNode,vNode)

  def apply(firstNode: org.scalajs.dom.raw.Element, vNode: VNodeProxy) = p(firstNode,vNode)
}

@js.native
trait VNodeProxy extends js.Object {
  val elm: js.UndefOr[Element]
  val data: DataObject
  val children: js.Array[VNodeProxy]
  val sel: String
}

object VNodeProxy {
  def fromString(string: String): VNodeProxy = string.asInstanceOf[VNodeProxy]
}


@js.native
@JSImport("snabbdom", JSImport.Namespace, globalFallback = "snabbdom")
object Snabbdom extends js.Object {
  def init(args: js.Array[Any]): js.Function2[Node | VNodeProxy, VNodeProxy, Unit] = js.native

}

@js.native
@JSImport("snabbdom/modules/class", JSImport.Namespace, globalFallback = "snabbdom_class")
object SnabbdomClass extends js.Object {
  val default: js.Any = js.native
}

@js.native
@JSImport("snabbdom/modules/eventlisteners", JSImport.Namespace, globalFallback = "snabbdom_eventlisteners")
object SnabbdomEventListeners extends js.Object{
  val default: js.Any = js.native
}

@js.native
@JSImport("snabbdom/modules/attributes", JSImport.Namespace, globalFallback = "snabbdom_attributes")
object SnabbdomAttributes extends js.Object{
  val default: js.Any = js.native
}

@js.native
@JSImport("snabbdom/modules/props", JSImport.Namespace, globalFallback = "snabbdom_props")
object SnabbdomProps extends js.Object{
  val default: js.Any = js.native
}

@js.native
@JSImport("snabbdom/modules/style", JSImport.Namespace, globalFallback = "snabbdom_style")
object SnabbdomStyle extends js.Object {
  val default: js.Any = js.native
}
