package outwatch.dom.definitions

import org.scalajs.dom
import org.scalajs.dom.Element
import outwatch.dom._
import outwatch.dom.helpers._
import snabbdom.VNodeProxy
import outwatch.reactive.{Source, SinkObserver, Subscription}

import scala.scalajs.js

/** Trait containing the contents of the `Attributes` module, so they can be
  * mixed in to other objects if needed. This should contain "all" attributes
  * and mix in other traits (defined above) as needed to get full coverage.
  */
trait OutwatchAttributes {

  private def proxyElementEmitter(f: js.Function1[VNodeProxy, Unit] => VDomModifier): SinkObserver[dom.Element] => VDomModifier =
    obs => f(p => p.elm.foreach(obs.onNext(_)))
  private def proxyElementFirstEmitter(f: js.Function2[VNodeProxy, VNodeProxy, Unit] => VDomModifier): SinkObserver[dom.Element] => VDomModifier =
    obs => f((o,_) => o.elm.foreach(obs.onNext(_)))
  private def proxyElementPairEmitter(f: js.Function2[VNodeProxy, VNodeProxy, Unit] => VDomModifier): SinkObserver[(dom.Element, dom.Element)] => VDomModifier =
    obs => f((o,p) => o.elm.foreach(oe => p.elm.foreach(pe => obs.onNext((oe,pe)))))
  private def proxyElementPairOptionEmitter(f: js.Function2[VNodeProxy, VNodeProxy, Unit] => VDomModifier): SinkObserver[(Option[dom.Element], Option[dom.Element])] => VDomModifier =
    obs => f((o,p) => {
      obs.onNext((o.elm.toOption, p.elm.toOption))
      ()
    })


  /** Outwatch component life cycle hooks. */
  lazy val onDomMount: EmitterBuilder.Sync[dom.Element, VDomModifier] = EmitterBuilder(proxyElementEmitter(DomMountHook))
  lazy val onDomUnmount: EmitterBuilder.Sync[dom.Element, VDomModifier] = EmitterBuilder(proxyElementEmitter(DomUnmountHook))
  lazy val onDomPreUpdate: EmitterBuilder.Sync[dom.Element, VDomModifier] = EmitterBuilder(proxyElementFirstEmitter(DomPreUpdateHook))
  lazy val onDomUpdate: EmitterBuilder.Sync[dom.Element, VDomModifier] = EmitterBuilder(proxyElementFirstEmitter(DomUpdateHook))

  /**
    * Lifecycle hook for component insertion.
    *
    * This hook is invoked once the DOM element for a vnode has been inserted into the document
    * and the rest of the patch cycle is done.
    */
  lazy val onSnabbdomInsert: EmitterBuilder.Sync[Element, VDomModifier] = EmitterBuilder(proxyElementEmitter(InsertHook))

  /** Lifecycle hook for component prepatch. */
  lazy val onSnabbdomPrePatch: EmitterBuilder.Sync[(Option[dom.Element],Option[dom.Element]), VDomModifier] = EmitterBuilder(proxyElementPairOptionEmitter(PrePatchHook))

  /** Lifecycle hook for component updates. */
  lazy val onSnabbdomUpdate: EmitterBuilder.Sync[(dom.Element,dom.Element), VDomModifier] = EmitterBuilder(proxyElementPairEmitter(UpdateHook))

  /**
    * Lifecycle hook for component postpatch.
    *
    *  This hook is invoked every time a node has been patched against an older instance of itself.
    */
  lazy val onSnabbdomPostPatch: EmitterBuilder.Sync[(dom.Element,dom.Element), VDomModifier] = EmitterBuilder(proxyElementPairEmitter(PostPatchHook))

  /**
    * Lifecycle hook for component destruction.
    *
    * This hook is invoked on a virtual node when its DOM element is removed from the DOM
    * or if its parent is being removed from the DOM.
    */
  lazy val onSnabbdomDestroy: EmitterBuilder.Sync[dom.Element, VDomModifier] = EmitterBuilder(proxyElementEmitter(DestroyHook))

  /** Snabbdom Key Attribute */
  @inline def key = KeyBuilder
}

trait AttributeHelpers { self: Attributes =>
  @inline def `class` = className

  @inline def `for` = forId

  @inline def data = new DynamicAttrBuilder[Any]("data")

  @inline def attr[T](key: String, convert: T => Attr.Value = (t: T) => t.toString : Attr.Value) = new BasicAttrBuilder[T](key, convert)
  @inline def prop[T](key: String, convert: T => Prop.Value = (t: T) => t) = new PropBuilder[T](key, convert)
  @inline def style[T](key: String) = new BasicStyleBuilder[T](key)

  @inline def emitter[F[_] : Source, E](source: F[E]): EmitterBuilder[E, VDomModifier] = EmitterBuilder.fromSource(source)

  @inline def cancelable(cancel: () => Unit): Subscription = Subscription(cancel)
}

trait TagHelpers {
  @inline def htmlTag(name: String): HtmlVNode = HtmlVNode(name, js.Array())
  @inline def svgTag(name: String): SvgVNode = SvgVNode(name, js.Array())
}
