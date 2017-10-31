package outwatch.dom.helpers

import cats.effect.IO

import scala.language.dynamics
import outwatch.dom._
import rxscalajs.Observable

import scala.language.implicitConversions
import outwatch.dom.VDomModifier.StringNode

trait ValueBuilder[T] extends Any {
  def :=(value: T): VDomModifier
  def :=?(value: Option[T]): Option[VDomModifier] = value.map(:=)
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


final class AttributeBuilder[T](val attributeName: String) extends AnyVal with ValueBuilder[T] {

  private def assign(value: T) = Attribute(attributeName, value.toString)

  def :=(value: T) = IO.pure(assign(value))

  def <--(valueStream: Observable[T]) = {
    IO.pure(AttributeStreamReceiver(attributeName, valueStream.map(assign)))
  }
}

final class PropertyBuilder[T](val attributeName: String) extends AnyVal with ValueBuilder[T] {
  private def assign(value: T) = Prop(attributeName, value.toString)

  def :=(value: T) = IO.pure(assign(value))

  def <--(valueStream: Observable[T]) = {
    IO.pure(AttributeStreamReceiver(attributeName, valueStream.map(assign)))
  }
}

final class StyleBuilder(val attributeName: String) extends AnyVal with ValueBuilder[String] {
  private def assign(value: String) = Style(attributeName, value)

  def :=(value: String) = IO.pure(assign(value))

  def <--(valueStream: Observable[String]) = {
    IO.pure(AttributeStreamReceiver(attributeName, valueStream.map(assign)))
  }
}

final class DynamicAttributeBuilder[T](parts: List[String]) extends Dynamic with ValueBuilder[T] {
  private lazy val name: String = parts.reverse.mkString("-")

  def selectDynamic(s: String) = new DynamicAttributeBuilder[T](s :: parts)

  private def assign(value: T) = Attribute(name, value.toString)

  def :=(value: T) = IO.pure(assign(value))

  def <--(valueStream: Observable[T]) = {
    IO.pure(AttributeStreamReceiver(name, valueStream.map(assign)))
  }
}

final class BoolAttributeBuilder(val attributeName: String) extends AnyVal with ValueBuilder[Boolean] {

  private def assign(value: Boolean) = Attribute(attributeName, value)

  def :=(value: Boolean) = IO.pure(assign(value))

  def <--(valueStream: Observable[Boolean]) = {
    IO.pure(AttributeStreamReceiver(attributeName, valueStream.map(assign)))
  }
}

object BoolAttributeBuilder {
  implicit def toAttribute(builder: BoolAttributeBuilder): Attribute = builder assign true
}

object KeyBuilder {
  def :=(key: String) = IO.pure(Key(key))
}
