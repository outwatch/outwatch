package outwatch.dom.helpers

import scala.language.dynamics
import outwatch.dom._
import rxscalajs.Observable
import scala.language.implicitConversions

trait ValueBuilder[T] extends Any {
  def :=(value: T): VDomModifier
  def :=?(value: Option[T]): Option[VDomModifier] = value.map(:=)
}

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


final class AttributeBuilder[T](val attributeName: String) extends AnyVal with ValueBuilder[T] {
  def :=(value: T) = Attribute(attributeName, value.toString)

  def <--(valueStream: Observable[T]) = {
    AttributeStreamReceiver(attributeName, valueStream.map(:=))
  }
}

final class PropertyBuilder[T](val attributeName: String) extends AnyVal with ValueBuilder[T] {
  def :=(value: T) = Prop(attributeName, value.toString)

  def <--(valueStream: Observable[T]) = {
    AttributeStreamReceiver(attributeName, valueStream.map(:=))
  }
}

final class StyleBuilder(val attributeName: String) extends AnyVal with ValueBuilder[String] {
  def :=(value: String) = Style(attributeName, value)

  def <--(valueStream: Observable[String]) = {
    AttributeStreamReceiver(attributeName, valueStream.map(:=))
  }
}

final class DynamicAttributeBuilder[T](parts: List[String]) extends Dynamic with ValueBuilder[T] {
  private lazy val name: String = parts.reverse.mkString("-")

  def selectDynamic(s: String) = new DynamicAttributeBuilder[T](s :: parts)

  def :=(value: T) = Attribute(name, value.toString)

  def <--(valueStream: Observable[T]) = {
    AttributeStreamReceiver(name, valueStream.map(:=))
  }
}

final class BoolAttributeBuilder(val attributeName: String) extends AnyVal with ValueBuilder[Boolean] {
  def :=(value: Boolean) = Attribute(attributeName, value)

  def <--(valueStream: Observable[Boolean]) = {
    AttributeStreamReceiver(attributeName, valueStream.map(:=))
  }
}

object BoolAttributeBuilder {
  implicit def toAttribute(builder: BoolAttributeBuilder): Attribute = builder := true
}

object KeyBuilder {
  def :=(key: String) = Key(key)
}
