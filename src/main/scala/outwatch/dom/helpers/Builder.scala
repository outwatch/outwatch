package outwatch.dom.helpers

import cats.effect.IO
import monix.reactive.Observable
import outwatch.AsVDomModifier

import scala.language.dynamics
import outwatch.dom._

trait AttributeBuilder[-T, +A <: Attribute] extends Any {
  protected def name: String
  private[outwatch] def assign(value: T): A

  def :=(value: T): IO[A] = IO.pure(assign(value))
  def :=?(value: Option[T]): Option[VDomModifier] = value.map(:=)
  def <--(valueStream: Observable[T]): IO[ModifierStreamReceiver] = {
    IO.pure(ModifierStreamReceiver(valueStream.map(v => IO.pure(assign(v)))))
  }
  def <--(valueStream: Observable[T], defaultValue: T): IO[ModifierStreamReceiver] = {
    IO.pure(ModifierStreamReceiver(valueStream.map(v => IO.pure(assign(v))), assign(defaultValue)))
  }
}

object AttributeBuilder {
  implicit def toAttribute(builder: AttributeBuilder[Boolean, Attr]): IO[Attribute] = builder := true
  implicit def toProperty(builder: AttributeBuilder[Boolean, Prop]): IO[Property] = builder := true
}

// Attr

trait AccumulateAttrOps[T] { self: AttributeBuilder[T, BasicAttr] =>
  def accum(s: String): AccumAttrBuilder[T] = accum(_ + s + _)
  def accum(reducer: (Attr.Value, Attr.Value) => Attr.Value) = new AccumAttrBuilder[T](name, this, reducer)
}

final class BasicAttrBuilder[T](val name: String, encode: T => Attr.Value) extends AttributeBuilder[T, BasicAttr]
                                                                                   with AccumulateAttrOps[T] {
  @inline private[outwatch] def assign(value: T) = BasicAttr(name, encode(value))
}

final class DynamicAttrBuilder[T](parts: List[String]) extends Dynamic
                                                               with AttributeBuilder[T, BasicAttr]
                                                               with AccumulateAttrOps[T] {
  lazy val name: String = parts.reverse.mkString("-")

  def selectDynamic(s: String) = new DynamicAttrBuilder[T](s :: parts)

  @inline private[outwatch] def assign(value: T) = BasicAttr(name, value.toString)
}

final class AccumAttrBuilder[T](
  val name: String,
  builder: AttributeBuilder[T, Attr],
  reduce: (Attr.Value, Attr.Value) => Attr.Value
) extends AttributeBuilder[T, AccumAttr] {
  @inline private[outwatch] def assign(value: T) = AccumAttr(name, builder.assign(value).value, reduce)
}

// Props

final class PropBuilder[T](val name: String, encode: T => Prop.Value) extends AttributeBuilder[T, Prop] {
  @inline private[outwatch] def assign(value: T) = Prop(name, encode(value))
}

// Styles

trait AccumulateStyleOps[T] extends Any { self: AttributeBuilder[T, BasicStyle] =>

  def accum: AccumStyleBuilder[T] = accum(",")
  def accum(s: String): AccumStyleBuilder[T] = accum(_ + s + _)
  def accum(reducer: (String, String) => String) = new AccumStyleBuilder[T](name, reducer)
}

final class BasicStyleBuilder[T](val name: String) extends AnyVal
                                                           with AttributeBuilder[T, BasicStyle]
                                                           with AccumulateStyleOps[T] {
  @inline private[outwatch] def assign(value: T) = BasicStyle(name, value.toString)

  def delayed: DelayedStyleBuilder[T] = new DelayedStyleBuilder[T](name)
  def remove: RemoveStyleBuilder[T] = new RemoveStyleBuilder[T](name)
  def destroy: DestroyStyleBuilder[T] = new DestroyStyleBuilder[T](name)
}

final class DelayedStyleBuilder[T](val name: String) extends AnyVal with AttributeBuilder[T, DelayedStyle] {
  @inline private[outwatch] def assign(value: T) = DelayedStyle(name, value.toString)
}

final class RemoveStyleBuilder[T](val name: String) extends AnyVal with AttributeBuilder[T, RemoveStyle] {
  @inline private[outwatch] def assign(value: T) = RemoveStyle(name, value.toString)
}

final class DestroyStyleBuilder[T](val name: String) extends AnyVal with AttributeBuilder[T, DestroyStyle] {
  @inline private[outwatch] def assign(value: T) = DestroyStyle(name, value.toString)
}

final class AccumStyleBuilder[T](val name: String, reducer: (String, String) => String)
  extends AttributeBuilder[T, AccumStyle] {
  @inline private[outwatch] def assign(value: T) = AccumStyle(name, value.toString, reducer)
}


object KeyBuilder {
  def :=(key: Key.Value): IO[Key] = IO.pure(Key(key))
}

// Child / Children

object ChildStreamReceiverBuilder {
  def <--[T](valueStream: Observable[VDomModifier]): IO[ModifierStreamReceiver] = IO.pure(
    ModifierStreamReceiver(valueStream)
  )

  def <--[T](valueStream: Observable[T])(implicit r: AsVDomModifier[T]): IO[ModifierStreamReceiver] = IO.pure(
    ModifierStreamReceiver(valueStream.map(r.asVDomModifier))
  )
}

object ChildrenStreamReceiverBuilder {
  def <--(childrenStream: Observable[Seq[VDomModifier]]): IO[ModifierStreamReceiver] = IO.pure(
    ModifierStreamReceiver(childrenStream.map[VDomModifier](x => x))
  )

  def <--[T](childrenStream: Observable[Seq[T]])(implicit r: AsVDomModifier[T]): IO[ModifierStreamReceiver] = IO.pure(
    ModifierStreamReceiver(childrenStream.map(_.map(r.asVDomModifier)))
  )
}
