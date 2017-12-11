package outwatch.dom.helpers

import cats.effect.IO
import outwatch.StaticVNodeRender

import scala.language.dynamics
import outwatch.dom._

trait ValueBuilder[-T, +SELF <: Attribute] extends Any {
  protected def attributeName: String
  private[outwatch] def assign(value: T): SELF

  def :=(value: T): IO[SELF] = IO.pure(assign(value))
  def :=?(value: Option[T]): Option[VDomModifier] = value.map(:=)
  def <--(valueStream: Observable[T]): IO[AttributeStreamReceiver] = {
    IO.pure(AttributeStreamReceiver(attributeName, valueStream.map(assign)))
  }
}
object ValueBuilder {
  implicit def toAttribute(builder: ValueBuilder[Boolean, Attr]): IO[Attribute] = builder := true
  implicit def toProperty(builder: ValueBuilder[Boolean, Prop]): IO[Property] = builder := true
}


trait AccumulateOps[T] { self: ValueBuilder[T, BasicAttr] =>
  def accum(s: String): AccumAttributeBuilder[T] = accum(_ + s + _)
  def accum(reducer: (Attr.Value, Attr.Value) => Attr.Value) = new AccumAttributeBuilder[T](attributeName, this, reducer)
}

final class AttributeBuilder[T](val attributeName: String, encode: T => Attr.Value) extends ValueBuilder[T, BasicAttr]
                                                                                            with AccumulateOps[T] {
  @inline private[outwatch] def assign(value: T) = BasicAttr(attributeName, encode(value))
}

final class DynamicAttributeBuilder[T](parts: List[String]) extends Dynamic
                                                                    with ValueBuilder[T, BasicAttr]
                                                                    with AccumulateOps[T] {
  lazy val attributeName: String = parts.reverse.mkString("-")

  def selectDynamic(s: String) = new DynamicAttributeBuilder[T](s :: parts)

  @inline private[outwatch] def assign(value: T) = BasicAttr(attributeName, value.toString)
}

final class AccumAttributeBuilder[T](
  val attributeName: String,
  builder: ValueBuilder[T, Attr],
  reduce: (Attr.Value, Attr.Value) => Attr.Value
) extends ValueBuilder[T, AccumAttr] {
  @inline private[outwatch] def assign(value: T) = AccumAttr(attributeName, builder.assign(value).value, reduce)
}


final class PropertyBuilder[T](val attributeName: String, encode: T => Prop.Value) extends ValueBuilder[T, Prop] {
  @inline private[outwatch] def assign(value: T) = Prop(attributeName, encode(value))
}


final class StyleBuilder[T](val attributeName: String) extends AnyVal with ValueBuilder[T, Style] {
  @inline private[outwatch] def assign(value: T) = Style(attributeName, value.toString)
}


object KeyBuilder {
  def :=(key: Key.Value): IO[Key] = IO.pure(Key(key))
}

// TODO: avoid nested IO?
object ChildStreamReceiverBuilder {
  def <--[T](valueStream: Observable[T])(implicit r: StaticVNodeRender[T]): IO[ChildStreamReceiver] = IO.pure (
    ChildStreamReceiver(valueStream.map(r.render))
  )
}

object ChildrenStreamReceiverBuilder {
  def <--(childrenStream: Observable[Seq[VNode]]): IO[ChildrenStreamReceiver] = IO.pure (
    ChildrenStreamReceiver(childrenStream)
  )
}
