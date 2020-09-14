package outwatch.helpers

import outwatch._
import colibri.{Source, Observable}

import scala.language.dynamics

trait AttributeBuilder[-T, +A <: Modifier] extends Any {
  def assign(value: T): A

  final def assignOption(value: Option[T]): Option[A] = value.map(assign)

  final def toggle(value: T): AttributeBuilder[Boolean, Modifier] = AttributeBuilder.ofModifier { enabled =>
    if (enabled) assign(value) else Modifier.empty
  }

  @inline final def :=(value: T): A = assign(value)

  @inline final def :=?(value: Option[T]): Option[A] = assignOption(value)

  final def <--[F[_] : Source](source: F[_ <: T]): Observable[A] = Observable.map(source)(assign)

  final def <--?[F[_] : Source](source: F[_ <: Option[T]]): Observable[Option[A]] = Observable.map(source)(assignOption)
}

object AttributeBuilder {
  @inline def ofModifier[T, A <: Modifier](create: T => A): AttributeBuilder[T, A] = new AttributeBuilder[T, A] {
    def assign(value: T): A = create(value)
  }
}

// Attr

@inline final class BasicAttrBuilder[T](val name: String, val encode: T => Attr.Value) extends AttributeBuilder[T, BasicAttr] {
  def assign(value: T) = BasicAttr(name, encode(value))

  @inline def accum(s: String): AccumAttrBuilder[T] = accum((v1, v2) => v1.toString + s + v2.toString)
  @inline def accum(reducer: (Attr.Value, Attr.Value) => Attr.Value) = new AccumAttrBuilder[T](name, encode, reducer)
}

@inline final class DynamicAttrBuilder[T](val name: String) extends Dynamic with AttributeBuilder[T, BasicAttr] {
  @inline def selectDynamic(s: String) = new DynamicAttrBuilder[T](name + "-" + s)
  @inline def assign(value: T) = BasicAttr(name, value.toString)

  @inline def accum(s: String): AccumAttrBuilder[T] = accum((v1, v2) => v1.toString + s + v2.toString)
  @inline def accum(reducer: (Attr.Value, Attr.Value) => Attr.Value) = new AccumAttrBuilder[T](name, _.toString, reducer)
}

@inline final class AccumAttrBuilder[T](
  val name: String,
  encode: T => Attr.Value,
  reduce: (Attr.Value, Attr.Value) => Attr.Value
) extends AttributeBuilder[T, AccumAttr] {
  def assign(value: T) = AccumAttr(name, encode(value), reduce)
}

// Props

@inline final class PropBuilder[T](val name: String, encode: T => Prop.Value) extends AttributeBuilder[T, Prop] {
  def assign(value: T) = Prop(name, encode(value))
}

// Styles

@inline final class BasicStyleBuilder[T](val name: String) extends AnyVal with AttributeBuilder[T, BasicStyle] {
  @inline def assign(value: T) = BasicStyle(name, value.toString)

  @inline def delayed: DelayedStyleBuilder[T] = new DelayedStyleBuilder[T](name)
  @inline def remove: RemoveStyleBuilder[T] = new RemoveStyleBuilder[T](name)
  @inline def destroy: DestroyStyleBuilder[T] = new DestroyStyleBuilder[T](name)

  @inline def accum: AccumStyleBuilder[T] = accum(";")
  @inline def accum(s: String): AccumStyleBuilder[T] = accum(_ + s + _)
  @inline def accum(reducer: (String, String) => String) = new AccumStyleBuilder[T](name, reducer)
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

@inline final class AccumStyleBuilder[T](val name: String, reducer: (String, String) => String) extends AttributeBuilder[T, AccumStyle] {
  def assign(value: T) = AccumStyle(name, value.toString, reducer)
}

object KeyBuilder {
  @inline def assign(key: Key.Value): Key = Key(key)
  @inline def :=(key: Key.Value): Key = assign(key)
}
