package outwatch.definitions

import org.scalajs.dom
import org.scalajs.dom.Element
import outwatch._
import outwatch.helpers._
import snabbdom.VNodeProxy
import colibri.{Cancelable, Observer, Source}
import com.raquo.domtypes.jsdom.defs.tags._

import scala.scalajs.js

/** Trait containing the contents of the `Attributes` module, so they can be mixed in to other objects if needed. This
  * should contain "all" attributes and mix in other traits (defined above) as needed to get full coverage.
  */
trait OutwatchAttributes {

  private final def proxyElementEmitter(
    f: js.Function1[VNodeProxy, Unit] => VModifier,
  ): Observer[dom.Element] => VModifier =
    obs => f(p => p.elm.foreach(obs.unsafeOnNext(_)))
  private final def proxyElementFirstEmitter(
    f: js.Function2[VNodeProxy, VNodeProxy, Unit] => VModifier,
  ): Observer[dom.Element] => VModifier =
    obs => f((o, _) => o.elm.foreach(obs.unsafeOnNext(_)))
  private final def proxyElementPairEmitter(
    f: js.Function2[VNodeProxy, VNodeProxy, Unit] => VModifier,
  ): Observer[(dom.Element, dom.Element)] => VModifier =
    obs => f((o, p) => o.elm.foreach(oe => p.elm.foreach(pe => obs.unsafeOnNext((oe, pe)))))
  private final def proxyElementPairOptionEmitter(
    f: js.Function2[VNodeProxy, VNodeProxy, Unit] => VModifier,
  ): Observer[(Option[dom.Element], Option[dom.Element])] => VModifier =
    obs => f((o, p) => obs.unsafeOnNext((o.elm.toOption, p.elm.toOption)))

  /** Outwatch component life cycle hooks. */
  final lazy val onDomMount: EmitterBuilder[dom.Element, VModifier] = EmitterBuilder(
    proxyElementEmitter(DomMountHook.apply),
  )
  final lazy val onDomUnmount: EmitterBuilder[dom.Element, VModifier] = EmitterBuilder(
    proxyElementEmitter(DomUnmountHook.apply),
  )
  final lazy val onDomPreUpdate: EmitterBuilder[dom.Element, VModifier] = EmitterBuilder(
    proxyElementFirstEmitter(DomPreUpdateHook.apply),
  )
  final lazy val onDomUpdate: EmitterBuilder[dom.Element, VModifier] = EmitterBuilder(
    proxyElementFirstEmitter(DomUpdateHook.apply),
  )

  /** Lifecycle hook for component insertion.
    *
    * This hook is invoked once the DOM element for a vnode has been inserted into the document and the rest of the
    * patch cycle is done.
    */
  final lazy val onSnabbdomInsert: EmitterBuilder[Element, VModifier] = EmitterBuilder(
    proxyElementEmitter(InsertHook.apply),
  )

  /** Lifecycle hook for component prepatch. */
  final lazy val onSnabbdomPrePatch: EmitterBuilder[(Option[dom.Element], Option[dom.Element]), VModifier] =
    EmitterBuilder(proxyElementPairOptionEmitter(PrePatchHook.apply))

  /** Lifecycle hook for component updates. */
  final lazy val onSnabbdomUpdate: EmitterBuilder[(dom.Element, dom.Element), VModifier] = EmitterBuilder(
    proxyElementPairEmitter(UpdateHook.apply),
  )

  /** Lifecycle hook for component postpatch.
    *
    * This hook is invoked every time a node has been patched against an older instance of itself.
    */
  final lazy val onSnabbdomPostPatch: EmitterBuilder[(dom.Element, dom.Element), VModifier] = EmitterBuilder(
    proxyElementPairEmitter(PostPatchHook.apply),
  )

  /** Lifecycle hook for component destruction.
    *
    * This hook is invoked on a virtual node when its DOM element is removed from the DOM or if its parent is being
    * removed from the DOM.
    */
  final lazy val onSnabbdomDestroy: EmitterBuilder[dom.Element, VModifier] = EmitterBuilder(
    proxyElementEmitter(DestroyHook.apply),
  )

  /** Snabbdom Key Attribute */
  @inline final def key = KeyBuilder
}

trait DocumentTagDeprecations[T[_]] {
  self: DocumentTags[T]
    with com.raquo.domtypes.generic.builders.HtmlTagBuilder[
      T,
      org.scalajs.dom.html.Element,
    ] => // Workaround for https://github.com/lampepfl/dotty/issues/14095
  @deprecated(
    "removed to free up name for use in local variables, use linkTag instead",
    "scala-dom-types: 0.10.0; outwatch: 1.0.0",
  )
  @inline final def link = linkTag
}

trait EmbedTagDeprecations[T[_]] {
  self: EmbedTags[T]
    with com.raquo.domtypes.generic.builders.HtmlTagBuilder[
      T,
      org.scalajs.dom.html.Element,
    ] => // Workaround for https://github.com/lampepfl/dotty/issues/14095
  @deprecated(
    "removed to free up name for use in local variables, use objectTag instead",
    "scala-dom-types: 0.10.0; outwatch: 1.0.0",
  )
  @inline final def `object` = objectTag

  @deprecated(
    "removed to free up name for use in local variables, use paramTag instead",
    "scala-dom-types: 0.10.0; outwatch: 1.0.0",
  )
  @inline final def param = paramTag

  @deprecated(
    "removed to free up name for use in local variables, use mapTag instead",
    "scala-dom-types: 0.10.0; outwatch: 1.0.0",
  )
  @inline final def map = mapTag
}

trait HtmlAttributeDeprecations { self: Attributes =>
  @deprecated(
    "removed to free up name for use in local variables, use maxAttr instead",
    "scala-dom-types: 0.10.0; outwatch: 1.0.0",
  )
  @inline final def max = maxAttr

  @deprecated(
    "removed to free up name for use in local variables, use minAttr instead",
    "scala-dom-types: 0.10.0; outwatch: 1.0.0",
  )
  @inline final def min = minAttr

  @deprecated(
    "removed to free up name for use in local variables, use stepAttr instead",
    "scala-dom-types: 0.10.0; outwatch: 1.0.0",
  )
  @inline final def step = stepAttr

  @deprecated(
    "removed to free up name for use in local variables, use idAttr instead",
    "scala-dom-types: 0.10.0; outwatch: 1.0.0",
  )
  @inline final def id = idAttr
}

trait SvgAttributeDeprecations { self: SvgAttrs =>
  @deprecated(
    "removed to free up name for use in local variables, use idAttr instead",
    "scala-dom-types: 0.10.0; outwatch: 1.0.0",
  )
  @inline final def id = idAttr

  @deprecated(
    "removed to free up name for use in local variables, use maxAttr instead",
    "scala-dom-types: 0.10.0; outwatch: 1.0.0",
  )
  @inline final def max = maxAttr

  @deprecated(
    "removed to free up name for use in local variables, use minAttr instead",
    "scala-dom-types: 0.10.0; outwatch: 1.0.0",
  )
  @inline final def min = minAttr

  @deprecated(
    "removed to free up name for use in local variables, use offsetAttr instead",
    "scala-dom-types: 0.10.0; outwatch: 1.0.0",
  )
  @inline final def offset = offsetAttr

  @deprecated(
    "removed to free up name for use in local variables, use resultAttr instead",
    "scala-dom-types: 0.10.0; outwatch: 1.0.0",
  )
  @inline final def result = resultAttr
}

trait AttributeHelpers { self: Attributes =>
  final val innerHTML: PropBuilder[UnsafeHTML] = VModifier.prop[UnsafeHTML]("innerHTML", _.html)

  @inline final def `class` = className

  @inline final def `for` = forId

  @inline final def data = new DynamicAttrBuilder[Any]("data")
  @inline final def aria = new DynamicAttrBuilder[Any]("aria")

  @deprecated("use VModifier.attr instead", "0.11.0")
  @inline final def attr[T](key: String, convert: T => Attr.Value = (t: T) => t.toString: Attr.Value) =
    new BasicAttrBuilder[T](key, convert)
  @deprecated("use VModifier.prop instead", "0.11.0")
  @inline final def prop[T](key: String, convert: T => Prop.Value = (t: T) => t) = new PropBuilder[T](key, convert)
  @deprecated("use VModifier.style instead", "0.11.0")
  @inline final def style[T](key: String, dummy: Unit = ()) = new BasicStyleBuilder[T](key)

  @deprecated("use EmitterBuilder.fromSource instead", "0.11.0")
  @inline final def emitter[F[_]: Source, E](source: F[E]): EmitterBuilder[E, VModifier] =
    EmitterBuilder.fromSource(source)

  @deprecated("use colibri.Cancelable instead", "0.11.0")
  @inline final def cancelable(cancel: () => Unit): Cancelable = Cancelable(cancel)
}

trait TagHelpers {
  @deprecated("use VNode.html instead", "0.11.0")
  @inline final def htmlTag(name: String): HtmlVNode = VNode.html(name)

  @deprecated("use VNode.svg instead", "0.11.0")
  @inline final def svgTag(name: String): SvgVNode = VNode.svg(name)
}

trait ManagedHelpers {
  import colibri._
  import colibri.effect.RunEffect
  import cats._
  import cats.effect.Sync
  import cats.implicits._

  @deprecated("use VModifier.managed(subscription) instead", "1.0.0")
  @inline final def managed[F[_]: Sync: RunEffect, T: CanCancel](subscription: F[T]): VModifier =
    VModifier.managed(subscription)
  @deprecated("use VModifier.managed(sub1), VModifier.managed(sub2), ... instead", "1.0.0")
  final def managed[F[_]: Sync: RunEffect: Applicative, T: CanCancel: Monoid](
    sub1: F[T],
    sub2: F[T],
    subscriptions: F[T]*,
  ): VModifier = {
    val composite = (sub1 :: sub2 :: subscriptions.toList).sequence.map[T](subs => Monoid[T].combineAll(subs))
    managed(composite)
  }
  @deprecated("use VModifier.managedEval(subscription) instead", "1.0.0")
  @inline final def managedFunction[T: CanCancel](subscription: () => T): VModifier =
    VModifier.managedFunction(subscription)
  @deprecated("use VModifier.managedElement instead", "1.0.0")
  final val managedElement = VModifier.managedElement
}
