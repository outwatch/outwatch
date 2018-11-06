package snabbdom

import org.scalajs.dom._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js.|

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
  var init: js.UndefOr[Hooks.HookSingleFn] = js.undefined
  var insert: js.UndefOr[Hooks.HookSingleFn] = js.undefined
  var prepatch: js.UndefOr[Hooks.HookPairFn] = js.undefined
  var update: js.UndefOr[Hooks.HookPairFn] = js.undefined
  var postpatch: js.UndefOr[Hooks.HookPairFn] = js.undefined
  var destroy: js.UndefOr[Hooks.HookSingleFn] = js.undefined
}

object Hooks {
  type HookSingleFn = js.Function1[VNodeProxy, Unit]
  type HookPairFn = js.Function2[VNodeProxy, VNodeProxy, Unit]

  def empty: Hooks = new Hooks {}
}

trait DataObject extends js.Object {
  import DataObject._

  var attrs: js.UndefOr[js.Dictionary[AttrValue]] = js.undefined
  var props: js.UndefOr[js.Dictionary[PropValue]] = js.undefined
  var style: js.UndefOr[js.Dictionary[StyleValue]] = js.undefined
  var on: js.UndefOr[js.Dictionary[js.Function1[Event, Unit]]] = js.undefined
  var hook: js.UndefOr[Hooks] = js.undefined
  var key: js.UndefOr[KeyValue] = js.undefined
  var ns: js.UndefOr[String] = js.undefined
  var args: js.UndefOr[js.Array[Any] | Boolean] = js.undefined
}

object DataObject {

  type PropValue = Any
  type AttrValue = String | Boolean
  type StyleValue = String | js.Dictionary[String]
  type KeyValue = String | Double  // https://github.com/snabbdom/snabbdom#key--string--number

  def empty: DataObject = new DataObject { }
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

  private def initThunk(fn: () => VNodeProxy)(thunk: VNodeProxy): Unit = {
    for {
      data <- thunk.data
    } {
      VNodeProxy.copyInto(source = fn(), target = thunk)
    }

    thunk.data.foreach(_.hook.foreach(_.init.foreach(_ (thunk))))
  }

  private def prepatchArray(fn: () => VNodeProxy)(oldProxy: VNodeProxy, thunk: VNodeProxy): Unit = {
    for {
      data <- thunk.data
      newArgs <- data.args.asInstanceOf[js.UndefOr[js.Array[Any]]]
    } {
      val oldArgs = oldProxy.data.flatMap(_.args).asInstanceOf[js.UndefOr[js.Array[Any]]]
      val isDifferent = oldArgs.fold(true) { oldArgs =>
        (oldArgs.length != newArgs.length) || existsIndexWhere(oldArgs.length)(i => oldArgs(i) != newArgs(i))
      }

      prepatch(fn, isDifferent, oldProxy, thunk)
    }

    thunk.data.foreach(_.hook.foreach(_.prepatch.foreach(_ (oldProxy, thunk))))
  }

  private def prepatchBoolean(fn: () => VNodeProxy)(oldProxy: VNodeProxy, thunk: VNodeProxy): Unit = {
    for {
      data <- thunk.data
      shouldRender <- data.args.asInstanceOf[js.UndefOr[Boolean]]
    } {
      prepatch(fn, shouldRender, oldProxy, thunk)
    }

    thunk.data.foreach(_.hook.foreach(_.prepatch.foreach(_ (oldProxy, thunk))))
  }

  @inline private def prepatch(fn: () => VNodeProxy, shouldRender: Boolean, oldProxy: VNodeProxy, thunk: VNodeProxy): Unit = {
    if (shouldRender) VNodeProxy.copyInto(source = fn(), target = thunk)
    else VNodeProxy.copyInto(source = oldProxy, target = thunk)
  }

  @inline private def existsIndexWhere(maxIndex: Int)(predicate: Int => Boolean): Boolean = {
    var i = 0
    while (i < maxIndex) {
      if (predicate(i)) return true
      i += 1
    }
    false
  }

  private def createProxy(namespace: js.UndefOr[String], selector: String, keyValue: DataObject.KeyValue, renderArgs: js.Array[Any] | Boolean, initHook: Hooks.HookSingleFn, prepatchHook: Hooks.HookPairFn, onUpdate: VNodeProxy => Unit): VNodeProxy =
    new VNodeProxy {
      sel = selector
      data = new DataObject {
        hook = new Hooks {
          init = initHook
          insert = {(p: VNodeProxy) => p.data.foreach(_.hook.foreach(_.insert.foreach(_(p)))) }: Hooks.HookSingleFn
          prepatch = prepatchHook
          update = {(o: VNodeProxy, p: VNodeProxy) => p.data.foreach(_.hook.foreach(_.update.foreach(_(o,p)))) }: Hooks.HookPairFn
          postpatch = {(o: VNodeProxy, p: VNodeProxy) => p.data.foreach(_.hook.foreach(_.postpatch.foreach(_(o,p)))) }: Hooks.HookPairFn
          destroy = {(p: VNodeProxy) => p.data.foreach(_.hook.foreach(_.destroy.foreach(_(p)))) }: Hooks.HookSingleFn
        }
        key = keyValue
        args = renderArgs
        ns = namespace
      }
      _update = onUpdate: js.Function1[VNodeProxy, Unit]
      key = keyValue
    }

  @inline def apply(namespace: js.UndefOr[String], selector: String, keyValue: DataObject.KeyValue, renderFn: js.Function0[VNodeProxy], renderArgs: js.Array[Any], onUpdate: VNodeProxy => Unit = _ => ()): VNodeProxy =
    createProxy(namespace, selector, keyValue, renderArgs, initThunk(renderFn), prepatchArray(renderFn), onUpdate)

  @inline def conditional(namespace: js.UndefOr[String], selector: String, keyValue: DataObject.KeyValue, renderFn: js.Function0[VNodeProxy], shouldRender: Boolean, onUpdate: VNodeProxy => Unit = _ => ()): VNodeProxy =
    createProxy(namespace, selector, keyValue, shouldRender, initThunk(renderFn), prepatchBoolean(renderFn), onUpdate)
}

object patch {

  private val p = Snabbdom.init(js.Array(
    SnabbdomClass.default,
    SnabbdomEventListeners.default,
    SnabbdomAttributes.default,
    SnabbdomCustomProps.default,
    SnabbdomStyle.default
  ))

  def apply(firstNode: VNodeProxy, vNode: VNodeProxy): VNodeProxy = p(firstNode,vNode)

  def apply(firstNode: org.scalajs.dom.Element, vNode: VNodeProxy): VNodeProxy = p(firstNode,vNode)
}

trait VNodeProxy extends js.Object {
  var sel: js.UndefOr[String] = js.undefined
  var data: js.UndefOr[DataObject] = js.undefined
  var children: js.UndefOr[js.Array[VNodeProxy]] = js.undefined
  var elm: js.UndefOr[Element] = js.undefined
  var text: js.UndefOr[String] = js.undefined
  var key: js.UndefOr[DataObject.KeyValue] = js.undefined
  var listener: js.UndefOr[js.Any] = js.undefined

  var _id: js.UndefOr[Int] = js.undefined
  var _unmount: js.UndefOr[Hooks.HookSingleFn] = js.undefined
  var _update: js.UndefOr[js.Function1[VNodeProxy, Unit]] = js.undefined
}

object VNodeProxy {
  def fromString(string: String): VNodeProxy = new VNodeProxy {
    text = string
  }

  def fromElement(element: Element): VNodeProxy = new VNodeProxy {
    sel = element.tagName.toLowerCase
    elm = element
    text = ""
    data = DataObject.empty
  }

  def copyInto(source: VNodeProxy, target: VNodeProxy): Unit = if (source != target) {
    source.data.foreach { data =>
      // keep args from previous proxy
      data.args = target.data.flatMap(_.args)
    }
    target.sel = source.sel
    target.key = source.key
    target.data = source.data
    target.children = source.children
    target.text = source.text
    target.elm = source.elm
    target.listener = source.listener
    target._id = source._id
    target._unmount = source._unmount
  }
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
