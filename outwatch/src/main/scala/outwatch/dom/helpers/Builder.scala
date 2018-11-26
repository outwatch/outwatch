package outwatch.dom.helpers

import monix.reactive.Observable
import outwatch.AsVDomModifier
import outwatch.dom._

import scala.language.dynamics

trait AttributeBuilder[-T, +A <: VDomModifier] extends Any {
  protected def name: String
  private[outwatch] def assign(value: T): A

  @inline def :=(value: T): A = assign(value)
  def :=?(value: Option[T]): Option[A] = value.map(assign)
  def <--[F[_] : AsValueObservable](valueStream: F[_ <: T]): ModifierStreamReceiver = {
    ModifierStreamReceiver(ValueObservable(valueStream).map(assign))
  }
}

object AttributeBuilder {
  @inline implicit def toAttribute[A <: VDomModifier](builder: AttributeBuilder[Boolean, A]): A = builder := true
}

// Attr

trait AccumulateAttrOps[T] { self: AttributeBuilder[T, BasicAttr] =>
  @inline def accum(s: String): AccumAttrBuilder[T] = accum(_ + s + _)
  @inline def accum(reducer: (Attr.Value, Attr.Value) => Attr.Value) = new AccumAttrBuilder[T](name, this, reducer)
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
  builder: AttributeBuilder[T, BasicAttr],
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

  @inline def accum: AccumStyleBuilder[T] = accum(",")
  @inline def accum(s: String): AccumStyleBuilder[T] = accum(_ + s + _)
  @inline def accum(reducer: (String, String) => String) = new AccumStyleBuilder[T](name, reducer)
}

final class BasicStyleBuilder[T](val name: String) extends AnyVal
                                                           with AttributeBuilder[T, BasicStyle]
                                                           with AccumulateStyleOps[T] {
  @inline private[outwatch] def assign(value: T) = BasicStyle(name, value.toString)

  @inline def delayed: DelayedStyleBuilder[T] = new DelayedStyleBuilder[T](name)
  @inline def remove: RemoveStyleBuilder[T] = new RemoveStyleBuilder[T](name)
  @inline def destroy: DestroyStyleBuilder[T] = new DestroyStyleBuilder[T](name)
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
  @inline def :=(key: Key.Value): Key = Key(key)
}

// Child / Children

object ChildStreamReceiverBuilder {
  def <--[T](valueStream: Observable[VDomModifier]): ModifierStreamReceiver =
    ModifierStreamReceiver(AsValueObservable.observable.as(valueStream))

  def <--[T](valueStream: Observable[T])(implicit r: AsVDomModifier[T]): ModifierStreamReceiver =
    ModifierStreamReceiver(AsValueObservable.observable.as(valueStream.map(r.asVDomModifier)))
}

object ChildrenStreamReceiverBuilder {
  def <--(childrenStream: Observable[Seq[VDomModifier]]): ModifierStreamReceiver =
    ModifierStreamReceiver(AsValueObservable.observable.as(childrenStream.map[VDomModifier](x => x)))

  def <--[T](childrenStream: Observable[Seq[T]])(implicit r: AsVDomModifier[T]): ModifierStreamReceiver =
    ModifierStreamReceiver(AsValueObservable.observable.as(childrenStream.map(_.map(r.asVDomModifier))))
}
