package outwatch

import colibri.{Observable, Source}

import scala.language.dynamics

trait AttrBuilder[-T, +A] extends Any {
  def assign(value: T): A

  final def assign(value: Option[T]): Option[A] = value.map(assign)

  @inline final def :=(value: T): A                 = assign(value)
  @inline final def :=(value: Option[T]): Option[A] = assign(value)

  final def <--[F[_]: Source, T2 <: T](source: F[T2]): Observable[A] = Observable.lift(source).map(assign)
  final def <--[F[_]: Source, T2 <: Option[T]](
    source: F[T2],
    @annotation.unused dummy: Unit = (),
  ): Observable[Option[A]] = Observable.lift(source).map(assign)
}

object AttrBuilder {
  @inline def apply[T, A](create: T => A): AttrBuilder[T, A] = new AttrBuilder[T, A] {
    def assign(value: T): A = create(value)
  }

  @inline def ofModifier[T](create: T => VMod): AttrBuilder[T, VMod] = apply[T, VMod](create)

  @inline def ofNode[T](create: T => VNode): AttrBuilder[T, VNode] = apply[T, VNode](create)

  // Attr

  @inline final class ToBasicAttr[T](val name: String, val encode: T => Attr.Value) extends AttrBuilder[T, BasicAttr] {
    def assign(value: T): BasicAttr = BasicAttr(name, encode(value))

    @inline def accum(s: String): ToAccumAttr[T]                       = accum((v1, v2) => v1.toString + s + v2.toString)
    @inline def accum(reducer: (Attr.Value, Attr.Value) => Attr.Value) = new ToAccumAttr[T](name, encode, reducer)
  }

  @inline final class ToDynamicAttr[T](val name: String) extends Dynamic with AttrBuilder[T, BasicAttr] {
    @inline def selectDynamic(s: String)    = new ToDynamicAttr[T](name + "-" + s)
    @inline def assign(value: T): BasicAttr = BasicAttr(name, value.toString)

    @inline def accum(s: String): ToAccumAttr[T] = accum((v1, v2) => v1.toString + s + v2.toString)
    @inline def accum(reducer: (Attr.Value, Attr.Value) => Attr.Value) =
      new ToAccumAttr[T](name, _.toString, reducer)
  }

  @inline final class ToAccumAttr[T](
    val name: String,
    encode: T => Attr.Value,
    reduce: (Attr.Value, Attr.Value) => Attr.Value,
  ) extends AttrBuilder[T, AccumAttr] {
    def assign(value: T): AccumAttr = AccumAttr(name, encode(value), reduce)
  }

  // Props

  @inline final class ToProp[T](val name: String, encode: T => Prop.Value) extends AttrBuilder[T, Prop] {
    def assign(value: T): Prop = Prop(name, encode(value))
  }

  // Styles

  @inline final class ToBasicStyle[T](val name: String) extends AnyVal with AttrBuilder[T, BasicStyle] {
    @inline def assign(value: T): BasicStyle = BasicStyle(name, value.toString)

    @inline def delayed: ToDelayedStyle[T] = new ToDelayedStyle[T](name)
    @inline def remove: ToRemoveStyle[T]   = new ToRemoveStyle[T](name)
    @inline def destroy: ToDestroyStyle[T] = new ToDestroyStyle[T](name)

    @inline def accum(s: String): ToAccumStyle[T]          = accum(_ + s + _)
    @inline def accum(reducer: (String, String) => String) = new ToAccumStyle[T](name, reducer)
  }

  @inline final class ToDelayedStyle[T](val name: String) extends AnyVal with AttrBuilder[T, DelayedStyle] {
    @inline def assign(value: T): DelayedStyle = DelayedStyle(name, value.toString)
  }

  @inline final class ToRemoveStyle[T](val name: String) extends AnyVal with AttrBuilder[T, RemoveStyle] {
    @inline def assign(value: T): RemoveStyle = RemoveStyle(name, value.toString)
  }

  @inline final class ToDestroyStyle[T](val name: String) extends AnyVal with AttrBuilder[T, DestroyStyle] {
    @inline def assign(value: T): DestroyStyle = DestroyStyle(name, value.toString)
  }

  @inline final class ToAccumStyle[T](val name: String, reducer: (String, String) => String)
      extends AttrBuilder[T, AccumStyle] {
    def assign(value: T): AccumStyle = AccumStyle(name, value.toString, reducer)
  }
}

object KeyBuilder {
  @inline def assign(key: Key.Value): Key = Key(key)
  @inline def :=(key: Key.Value): Key     = assign(key)
}
