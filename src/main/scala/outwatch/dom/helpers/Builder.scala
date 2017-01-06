package outwatch.dom.helpers

import scala.language.dynamics

import org.scalajs.dom._
import outwatch.Sink
import outwatch.dom._
import rxscalajs.{Observable, Subject}


case class ChildStreamReceiverBuilder() {
  def <--[T <: Any](valueStream: Observable[T]): ChildStreamReceiver = {
    ChildStreamReceiver(valueStream.map(anyToVNode))
  }

  private val anyToVNode = (any: Any) => any match {
    case vn: VNode => vn
    case _ => VDomModifier.StringNode(any.toString)
  }
}

case class ChildrenStreamReceiverBuilder() {
  def <--(childrenStream: Observable[Seq[VNode]]) = {
    ChildrenStreamReceiver(childrenStream)
  }
}


case class AttributeBuilder[T](attributeName: String){
  def :=(value: T) = Attribute(attributeName, value.toString)

  def <--(valueStream: Observable[T]) = {
    val attributeStream = valueStream.map(n => Attribute(attributeName, n.toString))
    AttributeStreamReceiver(attributeName, attributeStream)
  }
}

case class DynamicAttributeBuilder[T](parts: List[String]) extends Dynamic {
  private lazy val name: String = parts.reverse.mkString("-")

  def selectDynamic(s: String) = DynamicAttributeBuilder[T](s :: parts)

  def :=(value: T) = Attribute(name, value.toString)

  def <--(valueStream: Observable[T]) = {
    val attributeStream = valueStream.map(:=)
    AttributeStreamReceiver(name, attributeStream)
  }
}

case class BoolAttributeBuilder(attributeName: String) {
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
