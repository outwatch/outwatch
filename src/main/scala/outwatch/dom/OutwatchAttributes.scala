package outwatch.dom

import org.scalajs.dom
import org.scalajs.dom.Element
import outwatch.dom.helpers._

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
  lazy val onDomMount: SyncEmitterBuilder[dom.Element, VDomModifier] = EmitterBuilder.ofModifier(o => DomMountHook(o.onNext(_)))
  lazy val onDomUnmount: SyncEmitterBuilder[dom.Element, VDomModifier] = EmitterBuilder.ofModifier(o => DomUnmountHook(o.onNext(_)))
  lazy val onDomPreUpdate: SyncEmitterBuilder[dom.Element, VDomModifier] = EmitterBuilder.ofModifier(o => DomPreUpdateHook(o.onNext(_)))
  lazy val onDomUpdate: SyncEmitterBuilder[dom.Element, VDomModifier] = EmitterBuilder.ofModifier(o => DomUpdateHook(o.onNext(_)))

  /**
    * Lifecycle hook for component insertion.
    *
    * This hook is invoked once the DOM element for a vnode has been inserted into the document
    * and the rest of the patch cycle is done.
    */
  @deprecated("Consider using onDomMount instead for getting realiably notified whenever the element is mounted with this VNode. For the raw snabbdom event as before, you can use onSnabbdomInsert.", "")
  lazy val onInsert   = onSnabbdomInsert
  lazy val onSnabbdomInsert: SyncEmitterBuilder[Element, VDomModifier] = EmitterBuilder.ofModifier(o => InsertHook(o.onNext(_)))

  /** Lifecycle hook for component prepatch. */
  @deprecated("Consider using onDomPreUpdate instead for getting realiably notified whenever the element is updated with this VNode. For the raw snabbdom event as before, you can use onSnabbdomPrePatch.", "")
  lazy val onPrePatch   = onSnabbdomPrePatch
  lazy val onSnabbdomPrePatch: SyncEmitterBuilder[(Option[dom.Element],Option[dom.Element]), VDomModifier] = EmitterBuilder.ofModifier(o => PrePatchHook(o.onNext(_)))

  /** Lifecycle hook for component updates. */
  @deprecated("Consider using onDomUpdate instead for getting realiably notified whenever the element is updated with this VNode. For the raw snabbdom event as before, you can use onSnabbdomUpdate.", "")
  lazy val onUpdate   = onSnabbdomUpdate
  lazy val onSnabbdomUpdate: SyncEmitterBuilder[(dom.Element,dom.Element), VDomModifier] = EmitterBuilder.ofModifier(o => UpdateHook(o.onNext(_)))

  /**
    * Lifecycle hook for component postpatch.
    *
    *  This hook is invoked every time a node has been patched against an older instance of itself.
    */
  @deprecated("Consider using onDomUpdate instead for getting realiably notified whenever the element is updated with this VNode. For the raw snabbdom event as before, you can use onSnabbdomPostPatch.", "")
  lazy val onPostPatch   = onSnabbdomPostPatch
  lazy val onSnabbdomPostPatch: SyncEmitterBuilder[(dom.Element,dom.Element), VDomModifier] = EmitterBuilder.ofModifier(o => PostPatchHook(o.onNext(_)))

  /**
    * Lifecycle hook for component destruction.
    *
    * This hook is invoked on a virtual node when its DOM element is removed from the DOM
    * or if its parent is being removed from the DOM.
    */
  @deprecated("Consider using onDomUnmount instead for getting realiably notified whenever an element is unmounted with this VNode. For the raw snabbdom event as before, you can use onSnabbdomDestroy.", "")
  lazy val onDestroy  = onSnabbdomDestroy
  lazy val onSnabbdomDestroy: SyncEmitterBuilder[dom.Element, VDomModifier] = EmitterBuilder.ofModifier(o => DestroyHook(o.onNext(_)))
}

/** Snabbdom Key Attribute */
trait SnabbdomKeyAttributes {
  lazy val key = KeyBuilder
}

trait AttributeHelpers { self: Attributes =>
  lazy val `class` = className

  lazy val `for` = forId

  lazy val data = new DynamicAttrBuilder[Any]("data" :: Nil)

  def attr[T](key: String, convert: T => Attr.Value = (t: T) => t.toString : Attr.Value) = new BasicAttrBuilder[T](key, convert)
  def prop[T](key: String, convert: T => Prop.Value = (t: T) => t) = new PropBuilder[T](key, convert)
  def style[T](key: String) = new BasicStyleBuilder[T](key)
}

trait TagHelpers { self: Tags =>
  @deprecated("Use htmlTag(name) instead. For svg, you can use svgTag(name).", "")
  def tag(name: String): VNode= HtmlVNode(name, js.Array())
  def htmlTag(name: String): HtmlVNode = HtmlVNode(name, js.Array())
  def svgTag(name: String): SvgVNode = SvgVNode(name, js.Array())
}
