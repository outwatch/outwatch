package outwatch.dom.helpers

import cats.Applicative
import cats.effect.Effect
import outwatch.StaticVNodeRenderFactory
import outwatch.dom._

import scala.language.dynamics

trait BuilderFactory[F[+_]] extends VDomModifierFactory[F] with StaticVNodeRenderFactory[F] {
  implicit val effectF: Effect[F]

  trait AttributeBuilder[-T, +A <: Attribute] {
    protected def name: String
    private[outwatch] def assign(value: T): A

    def :=(value: T): F[A] = Applicative[F].pure(assign(value))
    def :=?(value: Option[T]): Option[VDomModifierF] = value.map(:=)
    def <--(valueStream: Observable[T]): F[AttributeStreamReceiver] = {
      Applicative[F].pure(AttributeStreamReceiver(name, valueStream.map(assign)))
    }
  }

  object AttributeBuilder {
    implicit def toAttribute(builder: AttributeBuilder[Boolean, Attr]): F[Attribute] = builder := true
    implicit def toProperty(builder: AttributeBuilder[Boolean, Prop]): F[Property] = builder := true
  }

  // Attr

  trait AccumulateAttrOps[T] { self: AttributeBuilder[T, BasicAttr] =>
    def accum(s: String): AccumAttrBuilder[T] = accum(_ + s + _)
    def accum(reducer: (Attr.Value, Attr.Value) => Attr.Value) = new AccumAttrBuilder[T](name, this, reducer)
  }

  final class BasicAttrBuilder[T](val name: String, encode: T => Attr.Value)
    extends AttributeBuilder[T, BasicAttr]
      with AccumulateAttrOps[T] {
    @inline private[outwatch] def assign(value: T) = BasicAttr(name, encode(value))
  }

  final class DynamicAttrBuilder[T](parts: List[String])
    extends Dynamic
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

  final class PropBuilder[T](val name: String, encode: T => Prop.Value)
    extends AttributeBuilder[T, Prop] {
    @inline private[outwatch] def assign(value: T) = Prop(name, encode(value))
  }

  // Styles

  trait AccumulateStyleOps[T] extends Any { self: AttributeBuilder[T, BasicStyle] =>

    def accum: AccumStyleBuilder[T] = accum(",")
    def accum(s: String): AccumStyleBuilder[T] = accum(_ + s + _)
    def accum(reducer: (String, String) => String) = new AccumStyleBuilder[T](name, reducer)
  }

  final class BasicStyleBuilder[T](val name: String)
    extends AttributeBuilder[T, BasicStyle]
      with AccumulateStyleOps[T] {
    @inline private[outwatch] def assign(value: T) = BasicStyle(name, value.toString)

    def delayed: DelayedStyleBuilder[T] = new DelayedStyleBuilder[T](name)
    def remove: RemoveStyleBuilder[T] = new RemoveStyleBuilder[T](name)
    def destroy: DestroyStyleBuilder[T] = new DestroyStyleBuilder[T](name)
  }

  final class DelayedStyleBuilder[T](val name: String)
    extends AttributeBuilder[T, DelayedStyle] {
    @inline private[outwatch] def assign(value: T) = DelayedStyle(name, value.toString)
  }

  final class RemoveStyleBuilder[T](val name: String)
    extends AttributeBuilder[T, RemoveStyle] {
    @inline private[outwatch] def assign(value: T) = RemoveStyle(name, value.toString)
  }

  final class DestroyStyleBuilder[T](val name: String)
    extends AttributeBuilder[T, DestroyStyle] {
    @inline private[outwatch] def assign(value: T) = DestroyStyle(name, value.toString)
  }

  final class AccumStyleBuilder[T](val name: String, reducer: (String, String) => String)
    extends AttributeBuilder[T, AccumStyle] {
    @inline private[outwatch] def assign(value: T) = AccumStyle(name, value.toString, reducer)
  }


  trait KeyBuilder {
    def :=(key: Key.Value): F[Key] = effectF.pure(Key(key))
  }

  // Child / Children

  object ChildStreamReceiverBuilder {
    def <--[T](valueStream: Observable[VNodeF]): F[ChildStreamReceiver] = effectF.pure(
      ChildStreamReceiver(valueStream)
    )

    def <--[T](valueStream: Observable[T])(implicit r: StaticVNodeRender[T]): F[ChildStreamReceiver] =
      effectF.pure(ChildStreamReceiver(valueStream.map(r.render)))
  }

  object ChildrenStreamReceiverBuilder {
    def <--(childrenStream: Observable[Seq[VNodeF]]): F[ChildrenStreamReceiver] =
      effectF.pure(ChildrenStreamReceiver(childrenStream))

    def <--[T](childrenStream: Observable[Seq[T]])
                          (implicit r: StaticVNodeRender[T]): F[ChildrenStreamReceiver] =
      effectF.pure(ChildrenStreamReceiver(childrenStream.map(_.map(r.render))))
  }
}
