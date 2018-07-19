package snabbdom

import org.scalajs.dom._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js.{UndefOr, |}

@js.native
@JSImport("snabbdom/h", JSImport.Namespace, globalFallback = "h")
object hProvider extends js.Object {
  val default: hFunction = js.native
}

@js.native
trait hFunction extends js.Any {
  def apply(nodeType: String, dataObject: DataObject): VNodeProxy = js.native
  def apply(nodeType: String, dataObject: DataObject, text: js.UndefOr[String]): VNodeProxy = js.native
  def apply(nodeType: String, dataObject: DataObject, children: js.Array[VNodeProxy]): VNodeProxy = js.native
}

object hFunction {
  def apply(nodeType: String, dataObject: DataObject): VNodeProxy = {
    hProvider.default.apply(nodeType, dataObject)
  }
  def apply(nodeType: String, dataObject: DataObject, text: js.UndefOr[String]): VNodeProxy = {
    hProvider.default.apply(nodeType, dataObject, text)
  }
  def apply(nodeType: String, dataObject: DataObject, children: js.Array[VNodeProxy]): VNodeProxy = {
    hProvider.default.apply(nodeType, dataObject, children)
  }
}


trait Hooks extends js.Object {
  var insert: js.UndefOr[Hooks.HookSingleFn] //TODO
  val prepatch: js.UndefOr[Hooks.HookPairFn]
  val update: js.UndefOr[Hooks.HookPairFn]
  val postpatch: js.UndefOr[Hooks.HookPairFn]
  val destroy: js.UndefOr[Hooks.HookSingleFn]
}

object Hooks {
  type HookSingleFn = js.Function1[VNodeProxy, Unit]
  type HookPairFn = js.Function2[VNodeProxy, VNodeProxy, Unit]

  def apply(
    insert: js.UndefOr[HookSingleFn] = js.undefined,
    prepatch: js.UndefOr[HookPairFn] = js.undefined,
    update: js.UndefOr[HookPairFn] = js.undefined,
    postpatch: js.UndefOr[HookPairFn] = js.undefined,
    destroy: js.UndefOr[HookSingleFn] = js.undefined
  ): Hooks = {
    val _insert = insert
    val _prepatch = prepatch
    val _update = update
    val _postpatch = postpatch
    val _destroy = destroy
    new Hooks {
      var insert = _insert
      val prepatch = _prepatch
      val update = _update
      val postpatch = _postpatch
      val destroy = _destroy
    }
  }
}

trait DataObject extends js.Object {
  import DataObject._

  val attrs: js.Dictionary[AttrValue]
  val props: js.Dictionary[PropValue]
  val style: js.Dictionary[StyleValue]
  val on: js.Dictionary[js.Function1[Event, Unit]]
  val hook: Hooks
  val key: js.UndefOr[KeyValue]
}

object DataObject {

  type PropValue = Any
  type AttrValue = String | Boolean
  type StyleValue = String | js.Dictionary[String]
  type KeyValue = String | Double  // https://github.com/snabbdom/snabbdom#key--string--number

  def apply(attrs: js.Dictionary[AttrValue],
            on: js.Dictionary[js.Function1[Event, Unit]],
            hooks : Hooks = Hooks()
           ): DataObject = apply(attrs, js.Dictionary.empty, js.Dictionary.empty, on, hooks, js.undefined)


  def apply(attrs: js.Dictionary[AttrValue],
            props: js.Dictionary[PropValue],
            style: js.Dictionary[StyleValue],
            on: js.Dictionary[js.Function1[Event, Unit]],
            hook: Hooks,
            key: js.UndefOr[KeyValue]
           ): DataObject = {

    val _attrs = attrs
    val _props = props
    val _style = style
    val _on = on
    val _hook = hook
    val _key = key

    new DataObject {
      val attrs: js.Dictionary[AttrValue] = _attrs
      val props: js.Dictionary[PropValue] = _props
      val style: js.Dictionary[StyleValue] = _style
      val on: js.Dictionary[js.Function1[Event, Unit]] = _on
      val hook: Hooks = _hook
      val key: UndefOr[KeyValue] = _key
    }
  }
}

object patch {

  private lazy val p = Snabbdom.init(js.Array(
    SnabbdomClass.default,
    SnabbdomEventListeners.default,
    SnabbdomAttributes.default,
    SnabbdomCustomProps.default,
    SnabbdomStyle.default
  ))

  def apply(firstNode: VNodeProxy, vNode: VNodeProxy): VNodeProxy = p(firstNode,vNode)

  def apply(firstNode: org.scalajs.dom.Element, vNode: VNodeProxy): VNodeProxy = p(firstNode,vNode)
}

@js.native
trait VNodeProxy extends js.Object {
  var sel: String
  var data: DataObject
  var children: js.UndefOr[js.Array[VNodeProxy]]
  var elm: js.UndefOr[Element]
  var text: js.UndefOr[String]
  var key: js.UndefOr[DataObject.KeyValue]
}

object VNodeProxy {
  def fromString(string: String): VNodeProxy = string.asInstanceOf[VNodeProxy]
}


@js.native
@JSImport("snabbdom", JSImport.Namespace, globalFallback = "snabbdom")
object Snabbdom extends js.Object {
  def init(args: js.Array[Any]): js.Function2[Node | VNodeProxy, VNodeProxy, VNodeProxy] = js.native

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

// forked snabbdom-props module where the ground-thruth is the dom
@js.native
@JSImport("./snabbdom-custom-props", JSImport.Namespace)
object SnabbdomCustomProps extends js.Object {
  val default: js.Any = js.native
}

// original snabbdom-props
// @js.native
// @JSImport("snabbdom/modules/props", JSImport.Namespace, globalFallback = "snabbdom_props")
// object SnabbdomProps extends js.Object{
//   val default: js.Any = js.native
// }


@js.native
@JSImport("snabbdom/modules/style", JSImport.Namespace, globalFallback = "snabbdom_style")
object SnabbdomStyle extends js.Object {
  val default: js.Any = js.native
}
