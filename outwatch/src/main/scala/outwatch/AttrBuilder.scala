package outwatch

import colibri.{Observable, Source}

import scala.language.dynamics

trait AttrBuilder[-T, +A] extends Any {
  def assign(value: T): A

  final def assign(value: Option[T]): Option[A] = value.map(assign)

  @deprecated("Use assign instead", "")
  final def assignOption(value: Option[T]): Option[A] = assign(value)

  @inline final def :=(value: T): A                 = assign(value)
  @inline final def :=(value: Option[T]): Option[A] = assign(value)

  final def <--[F[_]: Source, T2 <: T](source: F[T2]): Observable[A] = Observable.lift(source).map(assign)
  final def <--[F[_]: Source, T2 <: Option[T]](
    source: F[T2],
    @annotation.unused dummy: Unit = (),
  ): Observable[Option[A]] = Observable.lift(source).map(assign)

  @deprecated("Use := instead", "")
  final def :=?(value: Option[T]): Option[A] = :=(value)
  @deprecated("Use <-- instead", "")
  final def <--?[F[_]: Source, T2 <: Option[T]](source: F[T2]): Observable[Option[A]] = <--(source)

  @inline final def asFunctionOf[R](value: R): AttrBuilder[R => T, A] = AttrBuilder(f => assign(f(value)))

  @inline final def mapAttr[RA](f: A => RA): AttrBuilder[T, RA] = AttrBuilder(t => f(assign(t)))
  // @inline final def contramap[S](f: S => T): AttrBuilder[S, A]    = AttrBuilder(s => assign(f(s)))
}

object AttrBuilder {
  @inline def apply[T, A](create: T => A): AttrBuilder[T, A] = new AttrBuilder[T, A] {
    def assign(value: T): A = create(value)
  }

  @inline def ofModifier[T](create: T => VMod): AttrBuilder[T, VMod]                   = ofModifierM[Any, T](create)
  @inline def ofVNode[T](create: T => VNode): AttrBuilder[T, VNode]                    = ofVNodeM[Any, T](create)
  @inline def ofModifierM[Env, T](create: T => VModM[Env]): AttrBuilder[T, VModM[Env]] = apply(create)
  @inline def ofVNodeM[Env, T](create: T => VNodeM[Env]): AttrBuilder[T, VNodeM[Env]]  = apply(create)

  @inline def access[Env] = new PartiallyAppliedAccess[Env]
  @inline class PartiallyAppliedAccess[Env] {
    @inline def apply[T, A[-_]](
      builder: Env => AttrBuilder[T, A[Any]],
    )(implicit acc: AccessEnvironment[A]): AttrBuilder[T, A[Env]] =
      AttrBuilder[T, A[Env]](t => AccessEnvironment[A].access[Env](env => builder(env).assign(t)))
  }
  @inline def accessM[Env] = new PartiallyAppliedAccessM[Env]
  @inline class PartiallyAppliedAccessM[Env] {
    @inline def apply[R, T, A[-_]](builder: Env => AttrBuilder[T, A[R]])(implicit
      acc: AccessEnvironment[A],
    ): AttrBuilder[T, A[Env with R]] = access(env => builder(env).provide(env))
  }

  @inline implicit class AccessEnvironmentOperations[Env, T, A[-_]](builder: AttrBuilder[T, A[Env]])(implicit
    acc: AccessEnvironment[A],
  ) {
    @inline final def provide(env: Env): AttrBuilder[T, A[Any]] =
      builder.mapAttr(r => AccessEnvironment[A].provide(r)(env))
    @inline final def provideSome[REnv](map: REnv => Env): AttrBuilder[T, A[REnv]] =
      builder.mapAttr(r => AccessEnvironment[A].provideSome(r)(map))

    @inline final def asAccessFunction[REnv]: AttrBuilder[REnv => T, A[Env with REnv]] =
      AttrBuilder.accessM[REnv](builder.asFunctionOf)
  }

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

  implicit class VModOps[T](private val self: AttrBuilder[T, VMod]) extends AnyVal {
    @deprecated("Use observable operators instead", "")
    def toggle(value: T): AttrBuilder[Boolean, VMod] = AttrBuilder.ofModifier { enabled =>
      if (enabled) self.assign(value) else VMod.empty
    }
  }
}

object KeyBuilder {
  @inline def assign(key: Key.Value): Key = Key(key)
  @inline def :=(key: Key.Value): Key     = assign(key)
}
