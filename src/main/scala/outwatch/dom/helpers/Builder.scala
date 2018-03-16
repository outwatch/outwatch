package outwatch.dom.helpers

import cats.Applicative
import cats.effect.{Effect, IO, Sync}
import outwatch.StaticVNodeRender

import scala.language.dynamics
import outwatch.dom._

trait AttributeBuilder[F[+_], -T, +A <: Attribute] extends Any {
  protected def name: String
  private[outwatch] def assign(value: T): A

  def :=(value: T)(implicit F: Applicative[F]): F[A] = Applicative[F].pure(assign(value))
  def :=?(value: Option[T])(implicit F: Applicative[F]): Option[VDomModifierF[F]] = value.map(:=)
  def <--(valueStream: Observable[T])(implicit F: Effect[F]): F[AttributeStreamReceiver] = {
    Applicative[F].pure(AttributeStreamReceiver(name, valueStream.map(assign)))
  }
}

object AttributeBuilder {
  implicit def toAttribute[F[+_]: Applicative](builder: AttributeBuilder[F, Boolean, Attr]): F[Attribute] = builder := true
  implicit def toProperty[F[+_]: Applicative](builder: AttributeBuilder[F, Boolean, Prop]): F[Property] = builder := true
}

// Attr

trait AccumulateAttrOps[F[+_], T] { self: AttributeBuilder[F, T, BasicAttr] =>
  def accum(s: String): AccumAttrBuilder[F, T] = accum(_ + s + _)
  def accum(reducer: (Attr.Value, Attr.Value) => Attr.Value) = new AccumAttrBuilder[F, T](name, this, reducer)
}

final class BasicAttrBuilder[F[+_], T](val name: String, encode: T => Attr.Value) extends AttributeBuilder[F, T, BasicAttr]
                                                                                   with AccumulateAttrOps[F, T] {
  @inline private[outwatch] def assign(value: T) = BasicAttr(name, encode(value))
}

final class DynamicAttrBuilder[F[+_], T](parts: List[String]) extends Dynamic
                                                               with AttributeBuilder[F, T, BasicAttr]
                                                               with AccumulateAttrOps[F, T] {
  lazy val name: String = parts.reverse.mkString("-")

  def selectDynamic(s: String) = new DynamicAttrBuilder[F, T](s :: parts)

  @inline private[outwatch] def assign(value: T) = BasicAttr(name, value.toString)
}

final class AccumAttrBuilder[F[+_], T](
  val name: String,
  builder: AttributeBuilder[F, T, Attr],
  reduce: (Attr.Value, Attr.Value) => Attr.Value
) extends AttributeBuilder[F, T, AccumAttr] {
  @inline private[outwatch] def assign(value: T) = AccumAttr(name, builder.assign(value).value, reduce)
}

// Props

final class PropBuilder[F[+_], T](val name: String, encode: T => Prop.Value) extends AttributeBuilder[F, T, Prop] {
  @inline private[outwatch] def assign(value: T) = Prop(name, encode(value))
}

// Styles

trait AccumulateStyleOps[F[+_], T] extends Any { self: AttributeBuilder[F, T, BasicStyle] =>

  def accum: AccumStyleBuilder[F, T] = accum(",")
  def accum(s: String): AccumStyleBuilder[F, T] = accum(_ + s + _)
  def accum(reducer: (String, String) => String) = new AccumStyleBuilder[F, T](name, reducer)
}

final class BasicStyleBuilder[F[+_], T](val name: String) extends AnyVal
                                                           with AttributeBuilder[F, T, BasicStyle]
                                                           with AccumulateStyleOps[F, T] {
  @inline private[outwatch] def assign(value: T) = BasicStyle(name, value.toString)

  def delayed: DelayedStyleBuilder[F, T] = new DelayedStyleBuilder[F, T](name)
  def remove: RemoveStyleBuilder[F, T] = new RemoveStyleBuilder[F, T](name)
  def destroy: DestroyStyleBuilder[F, T] = new DestroyStyleBuilder[F, T](name)
}

final class DelayedStyleBuilder[F[+_], T](val name: String) extends AnyVal with AttributeBuilder[F, T, DelayedStyle] {
  @inline private[outwatch] def assign(value: T) = DelayedStyle(name, value.toString)
}

final class RemoveStyleBuilder[F[+_], T](val name: String) extends AnyVal with AttributeBuilder[F, T, RemoveStyle] {
  @inline private[outwatch] def assign(value: T) = RemoveStyle(name, value.toString)
}

final class DestroyStyleBuilder[F[+_], T](val name: String) extends AnyVal with AttributeBuilder[F, T, DestroyStyle] {
  @inline private[outwatch] def assign(value: T) = DestroyStyle(name, value.toString)
}

final class AccumStyleBuilder[F[+_], T](val name: String, reducer: (String, String) => String)
  extends AttributeBuilder[F, T, AccumStyle] {
  @inline private[outwatch] def assign(value: T) = AccumStyle(name, value.toString, reducer)
}


object KeyBuilder {
  def :=(key: Key.Value): IO[Key] = IO.pure(Key(key))
}

// Child / Children

object ChildStreamReceiverBuilder {
  def <--[F[+_]: Sync, T](valueStream: Observable[VNodeF[F]]): F[ChildStreamReceiver[F]] = Sync[F].pure(
    ChildStreamReceiver[F](valueStream)
  )

  def <--[F[+_]: Effect, T](valueStream: Observable[T])(implicit r: StaticVNodeRender[T]): F[ChildStreamReceiver[F]] =
    Sync[F].pure(ChildStreamReceiver(valueStream.map(r.render[F])))
}

object ChildrenStreamReceiverBuilder {
  def <--[F[+_]: Sync](childrenStream: Observable[Seq[VNodeF[F]]]): F[ChildrenStreamReceiver[F]] =
    Sync[F].pure(ChildrenStreamReceiver[F](childrenStream))

  def <--[F[+_]: Effect, T](childrenStream: Observable[Seq[T]])
                        (implicit r: StaticVNodeRender[T]): F[ChildrenStreamReceiver[F]] =
    Sync[F].pure(ChildrenStreamReceiver(childrenStream.map(_.map(r.render[F]))))
}
