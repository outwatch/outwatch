package outwatch.dom

import outwatch.dom.helpers._

/** Trait containing the contents of the `Attributes` module, so they can be
  * mixed in to other objects if needed. This should contain "all" attributes
  * and mix in other traits (defined above) as needed to get full coverage.
  */
trait OutwatchAttributes
  extends OutWatchChildAttributes
  with SnabbdomKeyAttributes
  with OutWatchLifeCycleAttributes
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
  /** Lifecycle hook for component insertion. */
  lazy val insert   = InsertHookBuilder

  /** Lifecycle hook for component updates. */
  lazy val update   = UpdateHookBuilder

  /** Lifecycle hook for component destruction. */
  lazy val destroy  = DestroyHookBuilder
}

/** Snabbdom Key Attribute */
trait SnabbdomKeyAttributes {
  lazy val key = KeyBuilder
}

trait TypedInputEventProps {
  import org.scalajs.dom

  /** The input event is fired when an element gets user input. */
  lazy val onInputChecked = DomEvents.onChange.map(_.target.asInstanceOf[dom.html.Input].checked)

  /** The input event is fired when an element gets user input. */
  lazy val onInputNumber  = DomEvents.onInput.map(_.target.asInstanceOf[dom.html.Input].valueAsNumber)

  /** The input event is fired when an element gets user input. */
  lazy val onInputString  = DomEvents.onInput.map(_.target.asInstanceOf[dom.html.Input].value)
}

trait AttributeHelpers {

  lazy val data = new DynamicAttributeBuilder[Any]("data" :: Nil)

  def attr[T](key: String, convert: T => Attribute.Value = (t: T) => t.toString : Attribute.Value) = new AttributeBuilder[T](key, convert)
  def prop[T](key: String, convert: T => String = (t: T) => t.toString) = new PropertyBuilder[T](key, convert)
  def style[T](key: String) = new StyleBuilder[T](key)
}
