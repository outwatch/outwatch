package outwatch.definitions

import org.scalajs.dom
import org.scalajs.dom.Element
import outwatch._
import snabbdom.VNodeProxy
import colibri.Observer

import scala.scalajs.js

/** Trait containing the contents of the `Attributes` module, so they can be mixed in to other objects if needed. This
  * should contain "all" attributes and mix in other traits (defined above) as needed to get full coverage.
  */
trait OutwatchAttributes {

  private final def proxyElementEmitter(
    f: js.Function1[VNodeProxy, Unit] => VMod,
  ): Observer[dom.Element] => VMod =
    obs => f(p => p.elm.foreach(obs.unsafeOnNext(_)))
  private final def proxyElementFirstEmitter(
    f: js.Function2[VNodeProxy, VNodeProxy, Unit] => VMod,
  ): Observer[dom.Element] => VMod =
    obs => f((o, _) => o.elm.foreach(obs.unsafeOnNext(_)))
  private final def proxyElementPairEmitter(
    f: js.Function2[VNodeProxy, VNodeProxy, Unit] => VMod,
  ): Observer[(dom.Element, dom.Element)] => VMod =
    obs => f((o, p) => o.elm.foreach(oe => p.elm.foreach(pe => obs.unsafeOnNext((oe, pe)))))
  private final def proxyElementPairOptionEmitter(
    f: js.Function2[VNodeProxy, VNodeProxy, Unit] => VMod,
  ): Observer[(Option[dom.Element], Option[dom.Element])] => VMod =
    obs => f((o, p) => obs.unsafeOnNext((o.elm.toOption, p.elm.toOption)))

  /** Outwatch component life cycle hooks. */
  final lazy val onDomMount: EmitterBuilder[dom.Element, VMod] = EmitterBuilder(
    proxyElementEmitter(DomMountHook.apply),
  )
  final lazy val onDomUnmount: EmitterBuilder[dom.Element, VMod] = EmitterBuilder(
    proxyElementEmitter(DomUnmountHook.apply),
  )
  final lazy val onDomPreUpdate: EmitterBuilder[dom.Element, VMod] = EmitterBuilder(
    proxyElementFirstEmitter(DomPreUpdateHook.apply),
  )
  final lazy val onDomUpdate: EmitterBuilder[dom.Element, VMod] = EmitterBuilder(
    proxyElementFirstEmitter(DomUpdateHook.apply),
  )

  /** Lifecycle hook for component insertion.
    *
    * This hook is invoked once the DOM element for a vnode has been inserted into the document and the rest of the
    * patch cycle is done.
    */
  final lazy val onSnabbdomInsert: EmitterBuilder[Element, VMod] = EmitterBuilder(
    proxyElementEmitter(InsertHook.apply),
  )

  /** Lifecycle hook for component prepatch. */
  final lazy val onSnabbdomPrePatch: EmitterBuilder[(Option[dom.Element], Option[dom.Element]), VMod] =
    EmitterBuilder(proxyElementPairOptionEmitter(PrePatchHook.apply))

  /** Lifecycle hook for component updates. */
  final lazy val onSnabbdomUpdate: EmitterBuilder[(dom.Element, dom.Element), VMod] = EmitterBuilder(
    proxyElementPairEmitter(UpdateHook.apply),
  )

  /** Lifecycle hook for component postpatch.
    *
    * This hook is invoked every time a node has been patched against an older instance of itself.
    */
  final lazy val onSnabbdomPostPatch: EmitterBuilder[(dom.Element, dom.Element), VMod] = EmitterBuilder(
    proxyElementPairEmitter(PostPatchHook.apply),
  )

  /** Lifecycle hook for component destruction.
    *
    * This hook is invoked on a virtual node when its DOM element is removed from the DOM or if its parent is being
    * removed from the DOM.
    */
  final lazy val onSnabbdomDestroy: EmitterBuilder[dom.Element, VMod] = EmitterBuilder(
    proxyElementEmitter(DestroyHook.apply),
  )

  /** Snabbdom Key Attribute */
  @inline final def key = KeyBuilder

  final val innerHTML: AttrBuilder.ToProp[UnsafeHTML] = VMod.prop[UnsafeHTML]("innerHTML", _.html)
}

trait AdditionalAttributes { self: Attributes =>

  @inline final def `class` = className

  @inline final def `for` = forId

  @inline final def data = new AttrBuilder.ToDynamicAttr[Any]("data")
  @inline final def aria = new AttrBuilder.ToDynamicAttr[Any]("aria")
}
