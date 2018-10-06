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

@js.native
trait Hooks extends js.Object {
  val init: js.UndefOr[Hooks.HookSingleFn]
  val insert: js.UndefOr[Hooks.HookSingleFn]
  val prepatch: js.UndefOr[Hooks.HookPairFn]
  val update: js.UndefOr[Hooks.HookPairFn]
  val postpatch: js.UndefOr[Hooks.HookPairFn]
  val destroy: js.UndefOr[Hooks.HookSingleFn]
}

object Hooks {
  type HookSingleFn = js.Function1[VNodeProxy, Unit]
  type HookPairFn = js.Function2[VNodeProxy, VNodeProxy, Unit]

  def apply(
    init: js.UndefOr[HookSingleFn] = js.undefined,
    insert: js.UndefOr[HookSingleFn] = js.undefined,
    prepatch: js.UndefOr[HookPairFn] = js.undefined,
    update: js.UndefOr[HookPairFn] = js.undefined,
    postpatch: js.UndefOr[HookPairFn] = js.undefined,
    destroy: js.UndefOr[HookSingleFn] = js.undefined
  ): Hooks = js.Dynamic.literal(init = init, insert = insert, prepatch = prepatch, update = update, postpatch = postpatch, destroy = destroy).asInstanceOf[Hooks]
}

@js.native
trait DataObject extends js.Object {
  import DataObject._

  val attrs: js.UndefOr[js.Dictionary[AttrValue]]
  val props: js.UndefOr[js.Dictionary[PropValue]]
  val style: js.UndefOr[js.Dictionary[StyleValue]]
  val on: js.UndefOr[js.Dictionary[js.Function1[Event, Unit]]]
  val hook: js.UndefOr[Hooks]
  val key: js.UndefOr[KeyValue]
  val ns: js.UndefOr[String]
  val args: js.UndefOr[js.Array[js.Any]]
  val fn: js.UndefOr[js.Function]
}

object DataObject {

  type PropValue = Any
  type AttrValue = String | Boolean
  type StyleValue = String | js.Dictionary[String]
  type KeyValue = String | Double  // https://github.com/snabbdom/snabbdom#key--string--number

  def empty: DataObject = js.Dynamic.literal().asInstanceOf[DataObject]

  def apply(attrs: js.UndefOr[js.Dictionary[AttrValue]] = js.undefined,
    props: js.UndefOr[js.Dictionary[PropValue]] = js.undefined,
    style: js.UndefOr[js.Dictionary[StyleValue]] = js.undefined,
    on: js.UndefOr[js.Dictionary[js.Function1[Event, Unit]]] = js.undefined,
    hook: js.UndefOr[Hooks] = js.undefined,
    key: js.UndefOr[KeyValue] = js.undefined,
    ns: js.UndefOr[String] = js.undefined,
    args: js.UndefOr[js.Array[Any]] = js.undefined,
    fn: js.UndefOr[js.Function] = js.undefined
   ): DataObject = js.Dynamic.literal(attrs = attrs, props = props, style = style, on = on, hook = hook, key = key.asInstanceOf[js.Any], ns = ns, args = args, fn = fn).asInstanceOf[DataObject]

}

// @js.native
// @JSImport("snabbdom/thunk", JSImport.Namespace, globalFallback = "thunk")
// object thunkProvider extends js.Object {
//   val default: thunkFunction = js.native
// }

// @js.native
// trait thunkFunction extends js.Any {
//   def apply(selector: String, renderFn: js.Function, argument: js.Array[Any]): VNodeProxy = js.native
//   def apply(selector: String, key: String, renderFn: js.Function, argument: js.Array[Any]): VNodeProxy = js.native
// }
object thunk {
  // own implementation of https://github.com/snabbdom/snabbdom/blob/master/src/thunk.ts
  //does respect equality. snabbdom thunk does not: https://github.com/snabbdom/snabbdom/issues/143

  private def copyToThunk(vnode: VNodeProxy, thunk: VNodeProxy): Unit = {
    vnode.data.asInstanceOf[js.Dynamic].fn = thunk.data.flatMap(_.fn)
    vnode.data.asInstanceOf[js.Dynamic].args = thunk.data.flatMap(_.args)
    thunk.asInstanceOf[js.Dynamic].data = vnode.data
    thunk.asInstanceOf[js.Dynamic].children = vnode.children
    thunk.asInstanceOf[js.Dynamic].text = vnode.text
    thunk.asInstanceOf[js.Dynamic].elm = vnode.elm
    thunk.asInstanceOf[js.Dynamic].key = vnode.key.asInstanceOf[js.Any]
    thunk.asInstanceOf[js.Dynamic].outwatchDomUnmountHook = vnode.outwatchDomUnmountHook
    thunk.asInstanceOf[js.Dynamic].outwatchId = vnode.outwatchId
  }

  private def init(thunk: VNodeProxy): Unit =
    for {
      data <- thunk.data
      fn <- data.fn
      newArgs <- data.args
    } copyToThunk(fn.call(null, newArgs: _*).asInstanceOf[VNodeProxy], thunk)

  private def prepatch(oldVNode: VNodeProxy, thunk: VNodeProxy): Unit =
    for {
      data <- thunk.data
      fn <- data.fn
      newArgs <- data.args
    } {
      @inline def update() = copyToThunk(fn.call(null, newArgs.toSeq: _*).asInstanceOf[VNodeProxy], thunk)
      @inline def keep() = copyToThunk(oldVNode, thunk)

      val oldArgs = oldVNode.data.flatMap(_.args)
      oldArgs.fold(keep()) { oldArgs =>
        if (oldArgs.length == newArgs.length) {
          var i = 0
          var isDifferent = false
          while (!isDifferent && i < oldArgs.length) {
            if (oldArgs(i) != newArgs(i)) isDifferent = true
            i += 1
          }

          if (isDifferent) update()
          else keep()
        } else update()
      }
    }

  def apply(selector: String, renderFn: js.Function, args: js.Array[Any]): VNodeProxy =
    VNodeProxy(selector, DataObject(hook = Hooks(init = (init ): Hooks.HookSingleFn, prepatch = (prepatch _): Hooks.HookPairFn), fn = renderFn, args = args))

  def apply(selector: String, key: DataObject.KeyValue, renderFn: js.Function, args: js.Array[Any]): VNodeProxy =
    VNodeProxy(selector, DataObject(hook = Hooks(init = (init ): Hooks.HookSingleFn, prepatch = (prepatch _): Hooks.HookPairFn), key = key, fn = renderFn, args = args), key = key)
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
  val sel: js.UndefOr[String]
  val data: js.UndefOr[DataObject]
  val children: js.UndefOr[js.Array[VNodeProxy]]
  val elm: js.UndefOr[Element]
  val text: js.UndefOr[String]
  val key: js.UndefOr[DataObject.KeyValue]

  val outwatchId: js.UndefOr[Int]
  val outwatchDomUnmountHook: js.UndefOr[Hooks.HookSingleFn]
}

object VNodeProxy {
  def fromString(string: String): VNodeProxy = js.Dynamic.literal(text = string).asInstanceOf[VNodeProxy]

  def fromElement(element: Element): VNodeProxy = js.Dynamic.literal(
    sel = element.tagName.toLowerCase,
    elm = element,
    text = "",
    data = DataObject.empty
  ).asInstanceOf[VNodeProxy]

  def apply(
    sel: js.UndefOr[String] = js.undefined,
    data: js.UndefOr[DataObject] = js.undefined,
    children: js.UndefOr[js.Array[VNodeProxy]] = js.undefined,
    key: js.UndefOr[DataObject.KeyValue] = js.undefined,
    outwatchId: js.UndefOr[Int] = js.undefined,
    outwatchDomUnmountHook: js.UndefOr[Hooks.HookSingleFn] = js.undefined): VNodeProxy =
    js.Dynamic.literal(sel = sel, data = data, children = children, key = key.asInstanceOf[js.Any], outwatchId = outwatchId, outwatchDomUnmountHook = outwatchDomUnmountHook).asInstanceOf[VNodeProxy]
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
