package outwatch.dom

import outwatch.dom.helpers._
import org.scalajs.dom
import cats.effect.IO

/** Trait containing the contents of the `Attributes` module, so they can be
  * mixed in to other objects if needed. This should contain "all" attributes
  * and mix in other traits (defined above) as needed to get full coverage.
  */
trait OutwatchAttributes
  extends OutWatchChildAttributes
  with SnabbdomKeyAttributes
  with OutWatchLifeCycleAttributes
  with EventTargetHelpers
  with TypedInputEventProps
  with AttributeHelpers

object OutwatchAttributes extends OutwatchAttributes

/** OutWatch specific attributes used to asign child nodes to a VNode. */
trait OutWatchChildAttributes {
  /** A special attribute that takes a stream of single child nodes. */
  lazy val child    = ChildStreamReceiverBuilder

  /** A special attribute that takes a stream of lists of child nodes. */
  lazy val children = ChildrenStreamReceiverBuilder
}

/** Outwatch component life cycle hooks. */
trait OutWatchLifeCycleAttributes {
  /**
    * Lifecycle hook for component insertion.
    *
    * This hook is invoked once the DOM element for a vnode has been inserted into the document
    * and the rest of the patch cycle is done.
    */
  lazy val insert   = InsertHookBuilder

  /** Lifecycle hook for component prepatch. */
  lazy val prepatch   = PrePatchHookBuilder

  /** Lifecycle hook for component updates. */
  lazy val update   = UpdateHookBuilder

  /**
    * Lifecycle hook for component postpatch.
    *
    *  This hook is invoked every time a node has been patched against an older instance of itself.
    */
  lazy val postpatch   = PostPatchHookBuilder

  /**
    * Lifecycle hook for component destruction.
    *
    * This hook is invoked on a virtual node when its DOM element is removed from the DOM
    * or if its parent is being removed from the DOM.
    */
  lazy val destroy  = DestroyHookBuilder
}

/** Snabbdom Key Attribute */
trait SnabbdomKeyAttributes {
  lazy val key = KeyBuilder
}

trait EventTargetHelpers {
  import org.scalajs.dom

  implicit class EventWithTargetAs(event: dom.Event) {
    def targetAs[Elem <: dom.EventTarget]: Elem = event.target.asInstanceOf[Elem]
  }

  implicit class EventTargetWith[Elem <: dom.EventTarget](elem: Elem) {
    def value(implicit tag: TagWithString[Elem]): String = tag.value(elem)
    def valueAsNumber(implicit tag: TagWithNumber[Elem]): Double = tag.valueAsNumber(elem)
    def checked(implicit tag: TagWithChecked[Elem]): Boolean = tag.checked(elem)
  }
}

trait TypedInputEventProps {
  /** The input event is fired when an element gets user input. */
  @deprecated("Use _.onChange.checked, which uses currentTarget. Or resort to onChange.targetAs[dom.html.Input].checked instead", "0.11.0")
  lazy val onInputChecked = Events.onChange.targetAs[dom.html.Input].checked

  /** The input event is fired when an element gets user input. */
  @deprecated("Use _.onInput.valueAsNumber, which uses currentTarget. Or resort to onInput.targetAs[dom.html.Input].valueAsNumber instead", "0.11.0")
  lazy val onInputNumber = Events.onInput.targetAs[dom.html.Input].valueAsNumber

  /** The input event is fired when an element gets user input. */
  @deprecated("Use _.onInput.value, which uses currentTarget. Or resort to onInput.targetAs[dom.html.Input].value instead", "0.11.0")
  lazy val onInputString = Events.onInput.targetAs[dom.html.Input].value
}

trait AttributeHelpers {
  lazy val data = new DynamicAttributeBuilder[Any]("data" :: Nil)

  def attr[T](key: String, convert: T => Attr.Value = (t: T) => t.toString : Attr.Value) = new AttributeBuilder[T](key, convert)
  def prop[T](key: String, convert: T => Prop.Value = (t: T) => t) = new PropertyBuilder[T](key, convert)
  def style[T](key: String) = new StyleBuilder[T](key)
}

trait TagHelpers {
  def tag/*[Elem <: dom.Element]*/(name: String): VTree[dom.Element] = IO.pure(VTree_(name, Seq.empty))
}
