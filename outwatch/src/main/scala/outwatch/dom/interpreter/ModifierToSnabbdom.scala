package outwatch.dom.interpreter

import org.scalajs.dom
import outwatch.dom._
import outwatch.dom.helpers.MutableNestedArray
import outwatch.dom.helpers.NativeHelpers._
import outwatch.reactive._
import snabbdom.{DataObject, Hooks, VNodeProxy}

import scala.scalajs.js

// This file is about interpreting VDomModifiers for building snabbdom virtual nodes.
// We want to convert a div(modifiers) into a VNodeProxy that we can use for patching
// with snabbdom.
//
// This code is really performance cirtical, because every patch call is preceeded by
// interpreting all VDomModifiers involed. This code is written in a mutable fashion
// using native js types to reduce overhead.

// This represents the structured definition of a VNodeProxy (like snabbdom expects it).
private[outwatch] class SeparatedModifiers {
  var hasOnlyTextChildren = true
  var nextModifiers: js.UndefOr[js.Array[StaticVDomModifier]] = js.undefined
  var proxies: js.UndefOr[js.Array[VNodeProxy]] = js.undefined
  var attrs: js.UndefOr[js.Dictionary[DataObject.AttrValue]] = js.undefined
  var props: js.UndefOr[js.Dictionary[DataObject.PropValue]] = js.undefined
  var styles: js.UndefOr[js.Dictionary[DataObject.StyleValue]] = js.undefined
  var emitters: js.UndefOr[js.Dictionary[js.Function1[dom.Event, Unit]]] = js.undefined
  var keyOption: js.UndefOr[Key.Value] = js.undefined
  var initHook: js.UndefOr[Hooks.HookSingleFn] = js.undefined
  var insertHook: js.UndefOr[Hooks.HookSingleFn] = js.undefined
  var prePatchHook: js.UndefOr[Hooks.HookPairFn] = js.undefined
  var updateHook: js.UndefOr[Hooks.HookPairFn] = js.undefined
  var postPatchHook: js.UndefOr[Hooks.HookPairFn] = js.undefined
  var destroyHook: js.UndefOr[Hooks.HookSingleFn] = js.undefined
  var domUnmountHook: js.UndefOr[Hooks.HookSingleFn] = js.undefined
}

private[outwatch] object SeparatedModifiers {
  def from(modifiers: MutableNestedArray[StaticVDomModifier], prependModifiers: js.UndefOr[js.Array[StaticVDomModifier]] = js.undefined, appendModifiers: js.UndefOr[js.Array[StaticVDomModifier]] = js.undefined): SeparatedModifiers = {
    val separatedModifiers = new SeparatedModifiers
    import separatedModifiers._

    @inline def assureProxies() = proxies getOrElse assign(new js.Array[VNodeProxy])(proxies = _)
    @inline def assureNextModifiers() = nextModifiers getOrElse assign(new js.Array[StaticVDomModifier])(nextModifiers = _)
    @inline def assureEmitters() = emitters getOrElse assign(js.Dictionary[js.Function1[dom.Event, Unit]]())(emitters = _)
    @inline def assureAttrs() = attrs getOrElse assign(js.Dictionary[DataObject.AttrValue]())(attrs = _)
    @inline def assureProps() = props getOrElse assign(js.Dictionary[DataObject.PropValue]())(props = _)
    @inline def assureStyles() = styles getOrElse assign(js.Dictionary[DataObject.StyleValue]())(styles = _)
    @inline def setSpecialStyle(styleName: String)(title: String, value: String): Unit = {
      val styles = assureStyles()
      styles.raw(styleName).fold {
        styles(styleName) = js.Dictionary[String](title -> value): DataObject.StyleValue
      } { style =>
        style.asInstanceOf[js.Dictionary[String]](title) = value
      }
    }
    @inline def createHooksSingle[T](current: js.UndefOr[js.Function1[T, Unit]], hook: js.Function1[T, Unit]): js.Function1[T, Unit] =
      current.fold(hook)(current => { p => current(p); hook(p) })
    @inline def createHooksPair[T](current: js.UndefOr[js.Function2[T, T, Unit]], hook: js.Function2[T, T, Unit]): js.Function2[T, T, Unit] =
      current.fold(hook)(current => { (o,p) => current(o, p); hook(o, p) })


    // append unmount hook for when patching a different proxy out of the dom.
    // the proxies will then have different OutwatchStates and we then need to
    // call the unmount hook of the oldProxy.
    postPatchHook = { (oldProxy, proxy) =>
      if (!NativeModifiers.equalsVNodeIds(proxy._id, oldProxy._id)) {
        oldProxy._unmount.foreach(_(oldProxy))
      }
    }: Hooks.HookPairFn

    def append(mod: StaticVDomModifier): Unit = mod match {
      case VNodeProxyNode(proxy) =>
        hasOnlyTextChildren = hasOnlyTextChildren && proxy.data.isEmpty && proxy.text.isDefined
        val proxies = assureProxies()
        proxies += proxy
        ()
      case a : BasicAttr =>
        val attrs = assureAttrs()
        attrs(a.title) = a.value
        ()
      case a : AccumAttr =>
        val attrs = assureAttrs()
        val attr = attrs.raw(a.title)
        attr.fold {
          attrs(a.title) = a.value
        } { attr =>
          attrs(a.title) = a.accum(attr, a.value)
        }
        ()
      case p : Prop =>
        val props = assureProps()
        props(p.title) = p.value
        ()
      case s: BasicStyle =>
        val styles = assureStyles()
        styles(s.title) = s.value
        ()
      case s: DelayedStyle =>
        setSpecialStyle(StyleKey.delayed)(s.title, s.value)
        ()
      case s: RemoveStyle =>
        setSpecialStyle(StyleKey.remove)(s.title, s.value)
        ()
      case s: DestroyStyle =>
        setSpecialStyle(StyleKey.destroy)(s.title, s.value)
        ()
      case a: AccumStyle =>
        val styles = assureStyles()
        val style = styles.raw(a.title)
        style.fold {
          styles(a.title) = a.value
        } { style =>
          styles(a.title) = a.accum(style.asInstanceOf[String], a.value): DataObject.StyleValue
        }
        ()
      case k: Key =>
        keyOption = k.value
        ()
      case e: Emitter =>
        val emitters = assureEmitters()
        val emitter = emitters.raw(e.eventType)
        emitters(e.eventType) = createHooksSingle(emitter, e.trigger)
        ()
      case h: DomMountHook =>
        insertHook = createHooksSingle(insertHook, h.trigger)
        postPatchHook = createHooksPair[VNodeProxy](postPatchHook, { (oldproxy, proxy) =>
          if (!NativeModifiers.equalsVNodeIds(oldproxy._id, proxy._id)) {
            h.trigger(proxy)
          }
        })
        ()
      case h: DomUnmountHook =>
        destroyHook = createHooksSingle(destroyHook, h.trigger)
        domUnmountHook = createHooksSingle(domUnmountHook, h.trigger)
        ()
      case h: DomUpdateHook =>
        postPatchHook = createHooksPair[VNodeProxy](postPatchHook, { (oldproxy, proxy) =>
          if (NativeModifiers.equalsVNodeIds(oldproxy._id, proxy._id)) {
            h.trigger(proxy, proxy)
          }
        })
        ()
      case h: DomPreUpdateHook =>
        prePatchHook = createHooksPair[VNodeProxy](prePatchHook, { (oldproxy, proxy) =>
          if (NativeModifiers.equalsVNodeIds(oldproxy._id, proxy._id)) {
            h.trigger(oldproxy, proxy)
          }
        })
        ()
      case h: InitHook =>
        initHook = createHooksSingle(initHook, h.trigger)
        ()
      case h: InsertHook =>
        insertHook = createHooksSingle(insertHook, h.trigger)
        ()
      case h: PrePatchHook =>
        prePatchHook = createHooksPair(prePatchHook, h.trigger)
        ()
      case h: UpdateHook =>
        updateHook = createHooksPair(updateHook, h.trigger)
        ()
      case h: PostPatchHook =>
        postPatchHook = createHooksPair(postPatchHook, h.trigger)
        ()
      case h: DestroyHook =>
        destroyHook = createHooksSingle(destroyHook, h.trigger)
        ()
      case n: NextVDomModifier =>
        val nextModifiers = assureNextModifiers()
        nextModifiers += n.modifier
        ()
    }

    prependModifiers.foreach(_.foreach(append))
    modifiers.foreach(append)
    appendModifiers.foreach(_.foreach(append))

    separatedModifiers
  }
}

// Each VNode or each streamed CompositeVDomModifier contains static and
// potentially dynamic content (i.e. streams). The contained VDomModifiers
// within this VNode or this CompositeVDomModifier need to be transformed into
// a list of static VDomModifiers (non-dynamic like attributes, vnode proxies,
// ... that can directly be rendered) and a combined observable of all dynamic
// content (like StreamModifier).
//
// The NativeModifier class represents exactly that: the static and dynamic
// part of a list of VDomModifiers. The static part is an array of all
// modifiers that are to-be-rendered at the current point in time. The dynamic
// part is an observable that changes the previously mentioned array to reflect
// the new state.
//
// Example
//  Input:
//    div (
//     "a",
//     observable
//     observable2
//    )
//  Output:
//    - currently active modifiers: Array("a", ?, ?)
//    - dynamic changes: collections of callbacks that fill the array of active modifiers

private[outwatch] class NativeModifiers(
  val modifiers: MutableNestedArray[StaticVDomModifier],
  val subscribables: MutableNestedArray[Subscribable]
)
private[outwatch] class Subscribable(
  newSubscription: SinkObserver[Unit] => Subscription
) {
  var subscription: Subscription = null

  def subscribe(sink: SinkObserver[Unit]): Unit = if (subscription == null) {
    // this is a weird function, it ignores a subsription, eventhough it does not know
    // wether this specific observer is already subscribed. In this case it is okay,
    // because this is an internal class that only ever is called with the same observer
    val variable = Subscription.variable()
    subscription = variable
    variable() = newSubscription(sink)
  }

  def unsubscribe(): Unit = if (subscription != null) {
    subscription.cancel()
    subscription = null
  }
}

private[outwatch] object NativeModifiers {
  def from(appendModifiers: js.Array[_ <: VDomModifier]): NativeModifiers = {
    val allModifiers = new MutableNestedArray[StaticVDomModifier]()
    val allSubscribables = new MutableNestedArray[Subscribable]()

    def append(subscribables: MutableNestedArray[Subscribable], modifiers: MutableNestedArray[StaticVDomModifier], modifier: VDomModifier, inStream: Boolean): Unit = {

      @inline def appendStatic(mod: StaticVDomModifier): Unit = {
        modifiers.push(mod)
        ()
      }

      @inline def appendStream(mod: StreamModifier): Unit = {

        val streamedModifiers = new MutableNestedArray[StaticVDomModifier]()
        val streamedSubscribables = new MutableNestedArray[Subscribable]()

        subscribables.push(new Subscribable(
          sink => mod.subscription(SinkObserver.contramap[SinkObserver, Unit, VDomModifier](sink) { modifier =>
            streamedSubscribables.foreach(_.unsubscribe())
            streamedSubscribables.clear()
            streamedModifiers.clear()
            append(streamedSubscribables, streamedModifiers, modifier, inStream = true)
          })
        ))

        modifiers.push(streamedModifiers)
        subscribables.push(streamedSubscribables)
        ()
      }

      modifier match {
        case EmptyModifier => ()
        case c: CompositeModifier => c.modifiers.foreach(append(subscribables, modifiers, _, inStream))
        case h: DomHook if inStream => mirrorStreamedDomHook(h).foreach(appendStatic)
        case mod: StaticVDomModifier => appendStatic(mod)
        case child: VNode  => appendStatic(VNodeProxyNode(SnabbdomOps.toSnabbdom(child)))
        case child: StringVNode  => appendStatic(VNodeProxyNode(VNodeProxy.fromString(child.text)))
        case m: StreamModifier => appendStream(m)
        case s: SubscriptionModifier => subscribables.push(new Subscribable(_ => s.subscription()))
        case m: SyncEffectModifier => append(subscribables, modifiers, m.unsafeRun(), inStream)
      }
    }

    appendModifiers.foreach(append(allSubscribables, allModifiers, _, inStream = false))

    new NativeModifiers(allModifiers, allSubscribables)
  }

  // if a dom mount hook is streamed, we want to emulate an intuitive interface as if they were static.
  // This means we need to translate the next update to a mount event and an unmount event for the previously
  // streamed hooks. the first update event needs to be ignored to emulate static update events.
  private def mirrorStreamedDomHook(h: DomHook): js.Array[StaticVDomModifier] = h match {
    case h: DomMountHook =>
      // trigger once for the next update event and always for each mount event.
      // if we are streamed in with an insert event, then ignore all update events.
      var triggered = false
      js.Array(
        InsertHook { p =>
          triggered = true
          h.trigger(p)
        },
        PostPatchHook { (o, p) =>
          if (!triggered || !equalsVNodeIds(o._id, p._id)) h.trigger(p)
          triggered = true
        })
    case h: DomPreUpdateHook =>
      // ignore the next pre-update event, we are streamed into the node with this update
      // trigger on all succeeding pre-update events. if we are streamed in with an insert
      // event, then trigger on next update events as well.
      var triggered = false
      js.Array(
        InsertHook { _ =>
          triggered = true
        },
        PrePatchHook { (o, p) =>
          if (triggered && equalsVNodeIds(o._id, p._id)) h.trigger(o, p)
          triggered = true
        }
      )
    case h: DomUpdateHook =>
      // ignore the next update event, we are streamed into the node with this update
      // trigger on all succeeding update events. if we are streamed in with an insert
      // event, then trigger on next update events as well.
      var triggered = false
      js.Array(
        InsertHook { _ =>
          triggered = true
        },
        PostPatchHook { (o, p) =>
          if (triggered && equalsVNodeIds(o._id, p._id)) h.trigger(o, p)
          triggered = true
        }
      )
    case h: DomUnmountHook =>
      // we call the unmount hook, whenever this hook is freshly superseded by a new modifier
      // in a stream. whenever the node is patched afterwards we check whether we are still
      // present in the node. if not, we are unmounted and call the hook. We additionally
      // react to the normal unmount event.
      var triggered = false
      var isOpen = true
      js.Array(
        h,
        InsertHook { _ => triggered = true },
        UpdateHook { (o, p) =>
          if (triggered && equalsVNodeIds(o._id, p._id)) isOpen = false
          triggered = true
        },
        NextVDomModifier(UpdateHook { (o, p) =>
          if (isOpen && equalsVNodeIds(o._id, p._id)) h.trigger(p)
          isOpen = true
        })
      )
  }

  @inline def equalsVNodeIds(oid: js.UndefOr[Int], nid: js.UndefOr[Int]): Boolean = {
    // Just doing oid == nid in scala does boxes-runtime equals
    oid.fold(nid.isEmpty)(oid => nid.fold(false)(_ == oid))
  }
}

private object StyleKey {
  @inline def delayed = "delayed"
  @inline def remove = "remove"
  @inline def destroy = "destroy"
}

