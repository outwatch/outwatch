package outwatch.dom.helpers

import cats.effect.Effect
import cats.effect.implicits._
import outwatch.StaticVNodeRender

import scala.language.dynamics
import outwatch.dom._

trait ValueBuilder[-T, +SELF <: Attribute] extends Any {
  protected def attributeName: String
  private[outwatch] def assign(value: T): SELF

  def :=[F[_]:Effect](value: T): F[SELF] = Effect[F].pure(assign(value))
  def :=?(value: Option[T]): Option[VDomModifier] = value.map(:=)
  def <--[F[_]:Effect](valueStream: Observable[T]): F[AttributeStreamReceiver] = {
    Effect[F].pure(AttributeStreamReceiver(attributeName, valueStream.map(assign)))
  }
}

object ValueBuilder {
  implicit def toAttribute[F[_]:Effect](builder: ValueBuilder[Boolean, Attr]): F[Attribute] = builder := true
  implicit def toProperty[F[_]:Effect](builder: ValueBuilder[Boolean, Prop]): F[Property] = builder := true
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

trait AccumulateStyleOps[T] extends Any { self: ValueBuilder[T, BasicStyle] =>

  def accum: AccumStyleBuilder[T] = accum(",")
  def accum(s: String): AccumStyleBuilder[T] = accum(_ + s + _)
  def accum(reducer: (String, String) => String) = new AccumStyleBuilder[T](attributeName, reducer)
}

// Styles
final class BasicStyleBuilder[T](val attributeName: String) extends AnyVal
                                                                    with ValueBuilder[T, BasicStyle]
                                                                    with AccumulateStyleOps[T] {
  @inline private[outwatch] def assign(value: T) = BasicStyle(attributeName, value.toString)

  def delayed: DelayedStyleBuilder[T] = new DelayedStyleBuilder[T](attributeName)
  def remove: RemoveStyleBuilder[T] = new RemoveStyleBuilder[T](attributeName)
  def destroy: DestroyStyleBuilder[T] = new DestroyStyleBuilder[T](attributeName)
}

final class DelayedStyleBuilder[T](val attributeName: String) extends AnyVal with ValueBuilder[T, DelayedStyle] {
  @inline private[outwatch] def assign(value: T) = DelayedStyle(attributeName, value.toString)
}

final class RemoveStyleBuilder[T](val attributeName: String) extends AnyVal with ValueBuilder[T, RemoveStyle] {
  @inline private[outwatch] def assign(value: T) = RemoveStyle(attributeName, value.toString)
}

final class DestroyStyleBuilder[T](val attributeName: String) extends AnyVal with ValueBuilder[T, DestroyStyle] {
  @inline private[outwatch] def assign(value: T) = DestroyStyle(attributeName, value.toString)
}

final class AccumStyleBuilder[T](val attributeName: String, reducer: (String, String) => String)
  extends ValueBuilder[T, AccumStyle] {
  @inline private[outwatch] def assign(value: T) = AccumStyle(attributeName, value.toString, reducer)
}


object KeyBuilder {
  def :=[F[_]:Effect](key: Key.Value): F[Key] = Effect[F].pure(Key(key))
}

// TODO: avoid nested IO?
object ChildStreamReceiverBuilder {
  def <--[T, F[_]:Effect](valueStream: Observable[T])(implicit r: StaticVNodeRender[T]): F[ChildStreamReceiver] = Effect[F].pure(
    ChildStreamReceiver(valueStream.map(r.render))
  )
}

object ChildrenStreamReceiverBuilder {
  def <--[F[_]:Effect](childrenStream: Observable[Seq[VNode]]): F[ChildrenStreamReceiver] = Effect[F].pure(
    ChildrenStreamReceiver(childrenStream)
  )
}
