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
  @deprecated("Use onChange(_.currentTarget.checked) instead. Note this uses currentTarget instead of target, for typing target resort to asInstanceOf", "0.11.0")
  lazy val onInputChecked = Events.onChange.onElement[dom.Element].map(_.target.asInstanceOf[dom.html.Input].checked)

  /** The input event is fired when an element gets user input. */
  @deprecated("Use onInput(_.currentTarget.valueAsNumber) instead (note this uses currentTarget instead of target, for typing target resort to asInstanceOf", "0.11.0")
  lazy val onInputNumber = Events.onInput.onElement[dom.Element].map(_.target.asInstanceOf[dom.html.Input].valueAsNumber)

  /** The input event is fired when an element gets user input. */
  @deprecated("Use onInput(_.currentTarget.value) instead (note this uses currentTarget instead of target, for typing target resort to asInstanceOf", "0.11.0")
  lazy val onInputString = Events.onInput.onElement[dom.Element].map(_.target.asInstanceOf[dom.html.Input].value)
}

trait AttributeHelpers {

  lazy val data = new DynamicAttributeBuilder[Any]("data" :: Nil)

  def attr[T](key: String, convert: T => Attr.Value = (t: T) => t.toString : Attr.Value) = new AttributeBuilder[T](key, convert)
  def prop[T](key: String, convert: T => Prop.Value = (t: T) => t) = new PropertyBuilder[T](key, convert)
  def style[T](key: String) = new StyleBuilder[T](key)
}
