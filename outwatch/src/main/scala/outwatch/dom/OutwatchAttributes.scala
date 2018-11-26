package outwatch.dom

import monix.reactive.{Observer, Observable}
import org.scalajs.dom
import org.scalajs.dom.Element
import outwatch.dom.helpers._
import snabbdom.VNodeProxy

import scala.scalajs.js

/** Trait containing the contents of the `Attributes` module, so they can be
  * mixed in to other objects if needed. This should contain "all" attributes
  * and mix in other traits (defined above) as needed to get full coverage.
  */
trait OutwatchAttributes
  extends SnabbdomKeyAttributes
  with OutWatchLifeCycleAttributes

/** Outwatch component life cycle hooks. */
trait OutWatchLifeCycleAttributes {
  private def proxyElementEmitter(f: js.Function1[VNodeProxy, Unit] => VDomModifier): Observer[dom.Element] => VDomModifier =
    obs => f(p => p.elm.foreach(obs.onNext(_)))
  private def proxyElementFirstEmitter(f: js.Function2[VNodeProxy, VNodeProxy, Unit] => VDomModifier): Observer[dom.Element] => VDomModifier =
    obs => f((o,_) => o.elm.foreach(obs.onNext(_)))
  private def proxyElementPairEmitter(f: js.Function2[VNodeProxy, VNodeProxy, Unit] => VDomModifier): Observer[(dom.Element, dom.Element)] => VDomModifier =
    obs => f((o,p) => o.elm.foreach(oe => p.elm.foreach(pe => obs.onNext((oe,pe)))))
  private def proxyElementPairOptionEmitter(f: js.Function2[VNodeProxy, VNodeProxy, Unit] => VDomModifier): Observer[(Option[dom.Element], Option[dom.Element])] => VDomModifier =
    obs => f((o,p) => obs.onNext((o.elm.toOption, p.elm.toOption)))

  lazy val onDomMount: SyncEmitterBuilder[dom.Element, VDomModifier] = EmitterBuilder.ofModifier(proxyElementEmitter(DomMountHook))
  lazy val onDomUnmount: SyncEmitterBuilder[dom.Element, VDomModifier] = EmitterBuilder.ofModifier(proxyElementEmitter(DomUnmountHook))
  lazy val onDomPreUpdate: SyncEmitterBuilder[dom.Element, VDomModifier] = EmitterBuilder.ofModifier(proxyElementFirstEmitter(DomPreUpdateHook))
  lazy val onDomUpdate: SyncEmitterBuilder[dom.Element, VDomModifier] = EmitterBuilder.ofModifier(proxyElementFirstEmitter(DomUpdateHook))

  /**
    * Lifecycle hook for component insertion.
    *
    * This hook is invoked once the DOM element for a vnode has been inserted into the document
    * and the rest of the patch cycle is done.
    */
  @deprecated("Consider using onDomMount instead for getting realiably notified whenever the element is mounted with this VNode. For the raw snabbdom event as before, you can use onSnabbdomInsert.", "")
  def onInsert   = onSnabbdomInsert
  lazy val onSnabbdomInsert: SyncEmitterBuilder[Element, VDomModifier] = EmitterBuilder.ofModifier(proxyElementEmitter(InsertHook))

  /** Lifecycle hook for component prepatch. */
  @deprecated("Consider using onDomPreUpdate instead for getting realiably notified before the element is updated with this VNode. For the raw snabbdom event as before, you can use onSnabbdomPrePatch.", "")
  def onPrePatch   = onSnabbdomPrePatch
  lazy val onSnabbdomPrePatch: SyncEmitterBuilder[(Option[dom.Element],Option[dom.Element]), VDomModifier] = EmitterBuilder.ofModifier(proxyElementPairOptionEmitter(PrePatchHook))

  /** Lifecycle hook for component updates. */
  @deprecated("Consider using onDomUpdate instead for getting realiably notified whenever the element is updated with this VNode. For the raw snabbdom event as before, you can use onSnabbdomUpdate.", "")
  def onUpdate   = onSnabbdomUpdate
  lazy val onSnabbdomUpdate: SyncEmitterBuilder[(dom.Element,dom.Element), VDomModifier] = EmitterBuilder.ofModifier(proxyElementPairEmitter(UpdateHook))

  /**
    * Lifecycle hook for component postpatch.
    *
    *  This hook is invoked every time a node has been patched against an older instance of itself.
    */
  @deprecated("Consider using onDomUpdate instead for getting realiably notified whenever the element is updated with this VNode. For the raw snabbdom event as before, you can use onSnabbdomPostPatch.", "")
  def onPostPatch   = onSnabbdomPostPatch
  lazy val onSnabbdomPostPatch: SyncEmitterBuilder[(dom.Element,dom.Element), VDomModifier] = EmitterBuilder.ofModifier(proxyElementPairEmitter(PostPatchHook))

  /**
    * Lifecycle hook for component destruction.
    *
    * This hook is invoked on a virtual node when its DOM element is removed from the DOM
    * or if its parent is being removed from the DOM.
    */
  @deprecated("Consider using onDomUnmount instead for getting realiably notified whenever an element is unmounted with this VNode. For the raw snabbdom event as before, you can use onSnabbdomDestroy.", "")
  def onDestroy  = onSnabbdomDestroy
  lazy val onSnabbdomDestroy: SyncEmitterBuilder[dom.Element, VDomModifier] = EmitterBuilder.ofModifier(proxyElementEmitter(DestroyHook))
}

/** Snabbdom Key Attribute */
trait SnabbdomKeyAttributes {
  @inline def key = KeyBuilder
}

trait AttributeHelpers { self: Attributes =>
  @inline def `class` = className

  @inline def `for` = forId

  lazy val data = new DynamicAttrBuilder[Any]("data" :: Nil)

  @inline def attr[T](key: String, convert: T => Attr.Value = (t: T) => t.toString : Attr.Value) = new BasicAttrBuilder[T](key, convert)
  @inline def prop[T](key: String, convert: T => Prop.Value = (t: T) => t) = new PropBuilder[T](key, convert)
  @inline def style[T](key: String) = new BasicStyleBuilder[T](key)

  @inline def emitter[E](observable: Observable[E]): EmitterBuilder[E, VDomModifier] = EmitterBuilder.fromObservable(observable)
}

trait TagHelpers { self: Tags =>
  @deprecated("Use htmlTag(name) instead. For svg, you can use svgTag(name).", "")
  @inline def tag(name: String): VNode= HtmlVNode(name, js.Array())
  @inline def htmlTag(name: String): HtmlVNode = HtmlVNode(name, js.Array())
  @inline def svgTag(name: String): SvgVNode = SvgVNode(name, js.Array())
}
