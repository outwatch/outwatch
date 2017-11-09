package outwatch.dom.helpers

import cats.effect.IO

import scala.language.dynamics
import outwatch.dom._
import rxscalajs.Observable

import scala.language.implicitConversions
import outwatch.dom.StringNode

trait ValueBuilder[T] extends Any {
  protected def attributeName: String
  protected def assign(value: T): outwatch.dom.Attribute

  def :=(value: T): IO[Attribute] = IO.pure(assign(value))
  def :=?(value: Option[T]): Option[VDomModifier] = value.map(:=)
  def <--(valueStream: Observable[T]): IO[AttributeStreamReceiver] = {
    IO.pure(AttributeStreamReceiver(attributeName, valueStream.map(assign)))
  }
}

final class AttributeBuilder[T](val attributeName: String) extends AnyVal with ValueBuilder[T] {
  @inline protected def assign(value: T) = Attribute(attributeName, value.toString)
}

final class PropertyBuilder[T](val attributeName: String) extends AnyVal with ValueBuilder[T] {
  @inline protected def assign(value: T) = Prop(attributeName, value.toString)
}

final class StyleBuilder(val attributeName: String) extends AnyVal with ValueBuilder[String] {
  @inline protected def assign(value: String) = Style(attributeName, value)
}

final class DynamicAttributeBuilder[T](parts: List[String]) extends Dynamic with ValueBuilder[T] {
  protected lazy val attributeName: String = parts.reverse.mkString("-")

  def selectDynamic(s: String) = new DynamicAttributeBuilder[T](s :: parts)

  @inline protected def assign(value: T) = Attribute(attributeName, value.toString)
}

final class BoolAttributeBuilder(val attributeName: String) extends AnyVal with ValueBuilder[Boolean] {
  @inline protected def assign(value: Boolean) = Attribute(attributeName, value)
}

object BoolAttributeBuilder {
  implicit def toAttribute(builder: BoolAttributeBuilder): Attribute = builder assign true
}

object KeyBuilder {
  def :=(key: String) = IO.pure(Key(key))
}

object ChildStreamReceiverBuilder {
  def <--[T <: Any](valueStream: Observable[T]): IO[ChildStreamReceiver] = {
    IO.pure(ChildStreamReceiver(valueStream.map(anyToVNode)))
  }

  private val anyToVNode: Any => VNode = {
    case vn: IO[_] => vn.asInstanceOf[VNode]
    case any => IO.pure(StringNode(any.toString))
  }
}

object ChildrenStreamReceiverBuilder {
  def <--(childrenStream: Observable[Seq[VNode]]) = {
    IO.pure(ChildrenStreamReceiver(childrenStream))
  }
}


