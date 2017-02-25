package outwatch.dom.helpers

import scala.language.dynamics
import outwatch.dom._
import rxscalajs.Observable
import scala.language.implicitConversions


object ChildStreamReceiverBuilder {
  def <--[T <: Any](valueStream: Observable[T]): ChildStreamReceiver = {
    ChildStreamReceiver(valueStream.map(anyToVNode))
  }

  private val anyToVNode = (any: Any) => any match {
    case vn: VNode => vn
    case _ => VDomModifier.StringNode(any.toString)
  }
}

object ChildrenStreamReceiverBuilder {
  def <--(childrenStream: Observable[Seq[VNode]]) = {
    ChildrenStreamReceiver(childrenStream)
  }
}


final class AttributeBuilder[T](val attributeName: String) extends AnyVal {
  def :=(value: T) = Attribute(attributeName, value.toString)

  def <--(valueStream: Observable[T]) = {
    val attributeStream = valueStream.map(n => Attribute(attributeName, n.toString))
    AttributeStreamReceiver(attributeName, attributeStream)
  }
}

final class DynamicAttributeBuilder[T](parts: List[String]) extends Dynamic {
  private lazy val name: String = parts.reverse.mkString("-")

  def selectDynamic(s: String) = new DynamicAttributeBuilder[T](s :: parts)

  def :=(value: T) = Attribute(name, value.toString)

  def <--(valueStream: Observable[T]) = {
    val attributeStream = valueStream.map(:=)
    AttributeStreamReceiver(name, attributeStream)
  }
}

final case class BoolAttributeBuilder(val attributeName: String) extends AnyVal {
  def :=(value: Boolean) = Attribute(attributeName, toEmptyIfFalse(value))

  def <--(valueStream: Observable[Boolean]) = {
    AttributeStreamReceiver(attributeName, valueStream.map(b => {
      Attribute(attributeName, toEmptyIfFalse(b))
    }))
  }

  private def toEmptyIfFalse(b: Boolean) = if (b) b.toString else ""
}

object BoolAttributeBuilder {
  implicit def toAttribute(builder: BoolAttributeBuilder): Attribute =
    Attribute(builder.attributeName, "_")
}
