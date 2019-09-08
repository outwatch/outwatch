package outwatch.dom.helpers

import outwatch.dom._
import outwatch.reactive.{Source, SourceStream}

import scala.language.dynamics

trait AttributeBuilder[-T, +A <: VDomModifier] extends Any {
  def assign(value: T): A

  @inline def :=(value: T): A = assign(value)

  def :=?(value: Option[T]): Option[A] = value.map(assign)
  def <--[F[_] : Source](source: F[_ <: T]): VDomModifier = VDomModifier(SourceStream.map(source)(assign))
}

object AttributeBuilder {
  @inline implicit def toAttribute[A <: VDomModifier](builder: AttributeBuilder[Boolean, A]): A = builder := true
}

// Attr

trait AccumulateAttrOps[T] { self: AttributeBuilder[T, BasicAttr] =>
  protected def name: String
  @inline def accum(s: String): AccumAttrBuilder[T] = accum((v1, v2) => s"$v1$s$v2")

  @inline def accum(reducer: (Attr.Value, Attr.Value) => Attr.Value) = new AccumAttrBuilder[T](name, this, reducer)
}

@inline final class BasicAttrBuilder[T](val name: String, encode: T => Attr.Value) extends AttributeBuilder[T, BasicAttr]
                                                                                   with AccumulateAttrOps[T] {
  @inline def assign(value: T) = BasicAttr(name, encode(value))
}

@inline final class DynamicAttrBuilder[T](parts: List[String]) extends Dynamic
                                                               with AttributeBuilder[T, BasicAttr]
                                                               with AccumulateAttrOps[T] {
  def name: String = parts.reverse.mkString("-")

  def selectDynamic(s: String) = new DynamicAttrBuilder[T](s :: parts)

  @inline def assign(value: T) = BasicAttr(name, value.toString)
}

@inline final class AccumAttrBuilder[T](
  val name: String,
  builder: AttributeBuilder[T, BasicAttr],
  reduce: (Attr.Value, Attr.Value) => Attr.Value
) extends AttributeBuilder[T, AccumAttr] {
  @inline def assign(value: T) = AccumAttr(name, builder.assign(value).value, reduce)
}

// Props

@inline final class PropBuilder[T](val name: String, encode: T => Prop.Value) extends AttributeBuilder[T, Prop] {
  @inline def assign(value: T) = Prop(name, encode(value))
}

// Styles

trait AccumulateStyleOps[T] extends Any { self: AttributeBuilder[T, BasicStyle] =>
  protected def name: String
  @inline def accum: AccumStyleBuilder[T] = accum(",")
  @inline def accum(s: String): AccumStyleBuilder[T] = accum(_ + s + _)
  @inline def accum(reducer: (String, String) => String) = new AccumStyleBuilder[T](name, reducer)
}

@inline final class BasicStyleBuilder[T](val name: String) extends AnyVal
                                                           with AttributeBuilder[T, BasicStyle]
                                                           with AccumulateStyleOps[T] {
  @inline def assign(value: T) = BasicStyle(name, value.toString)

  @inline def delayed: DelayedStyleBuilder[T] = new DelayedStyleBuilder[T](name)
  @inline def remove: RemoveStyleBuilder[T] = new RemoveStyleBuilder[T](name)
  @inline def destroy: DestroyStyleBuilder[T] = new DestroyStyleBuilder[T](name)
}

@inline final class DelayedStyleBuilder[T](val name: String) extends AnyVal with AttributeBuilder[T, DelayedStyle] {
  @inline def assign(value: T) = DelayedStyle(name, value.toString)
}

@inline final class RemoveStyleBuilder[T](val name: String) extends AnyVal with AttributeBuilder[T, RemoveStyle] {
  @inline def assign(value: T) = RemoveStyle(name, value.toString)
}

@inline final class DestroyStyleBuilder[T](val name: String) extends AnyVal with AttributeBuilder[T, DestroyStyle] {
  @inline def assign(value: T) = DestroyStyle(name, value.toString)
}

@inline final class AccumStyleBuilder[T](val name: String, reducer: (String, String) => String)
  extends AttributeBuilder[T, AccumStyle] {
  @inline def assign(value: T) = AccumStyle(name, value.toString, reducer)
}

object KeyBuilder {
  @inline def assign(key: Key.Value): Key = Key(key)
  @inline def :=(key: Key.Value): Key = assign(key)
}
