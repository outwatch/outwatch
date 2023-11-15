package outwatch.helpers

import colibri.{Observable, ObservableLike}
import outwatch._
import cats.Monoid

import scala.language.dynamics

trait AttrBuilder[-T, +A] extends Any {
  def assign(value: T): A

  @inline final def :=(source: Option[T]): Option[A] = source.map(assign)
  @inline final def :=(value: T): A                  = assign(value)

  @inline final def <--[F[+_]: ObservableLike](
    source: F[Option[T]],
    @annotation.nowarn dummy: Unit = (),
  ): Observable[Option[A]] = ObservableLike[F].toObservable(source).map(_.map(assign(_)))
  @inline final def <--[F[_]: ObservableLike](source: F[_ <: T]): Observable[A] =
    ObservableLike[F].toObservable(source).map(assign(_))

  @inline final def mapResult[RA](f: A => RA): AttrBuilder[T, RA] = AttrBuilder(t => f(assign(t)))
  @inline final def contramap[S](f: S => T): AttrBuilder[S, A]    = AttrBuilder(s => assign(f(s)))

  @inline final def use[R](value: R): AttrBuilder[R => T, A] = AttrBuilder(f => assign(f(value)))

  @deprecated("Use builder := option instead", "1.0.0")
  @inline final def assignOption(value: Option[T]): Option[A] = this := value
  @deprecated("Use builder := option instead", "1.0.0")
  @inline final def :=?(value: Option[T]): Option[A] = this := value
  @deprecated("Use builder <-- source instead", "1.0.0")
  @inline final def <--?[F[+_]: ObservableLike](source: F[Option[T]]): Observable[Option[A]] = this <-- source
}

object AttrBuilder {
  @inline def apply[T, A](create: T => A): AttrBuilder[T, A] = new AttrBuilderApply[T, A](create)
  @inline private class AttrBuilderApply[T, A](create: T => A) extends AttrBuilder[T, A] {
    @inline def assign(value: T): A = create(value)
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
      builder.mapResult(r => AccessEnvironment[A].provide(r)(env))
    @inline final def provideSome[REnv](map: REnv => Env): AttrBuilder[T, A[REnv]] =
      builder.mapResult(r => AccessEnvironment[A].provideSome(r)(map))

    @inline final def useAccess[REnv]: AttrBuilder[REnv => T, A[Env with REnv]] =
      AttrBuilder.accessM[REnv](builder.use)
  }

  @inline implicit class MonoidOperations[T, A: Monoid](builder: AttrBuilder[T, A]) {
    @inline final def toggle(value: => T): AttrBuilder[Boolean, A] = AttrBuilder { enabled =>
      if (enabled) builder.assign(value) else Monoid.empty
    }
  }
}

// Attr

@inline final class BasicAttrBuilder[T](val name: String, encode: T => Attr.Value)
    extends AttrBuilder[T, BasicAttr] {
  def assign(value: T) = BasicAttr(name, encode(value))

  @inline def accum(s: String): AccumAttrBuilder[T]                  = accum((v1, v2) => v1.toString + s + v2.toString)
  @inline def accum(reducer: (Attr.Value, Attr.Value) => Attr.Value) = new AccumAttrBuilder[T](name, encode, reducer)
}

@inline final class DynamicAttrBuilder[T](val name: String) extends Dynamic with AttrBuilder[T, BasicAttr] {
  @inline def selectDynamic(s: String) = new DynamicAttrBuilder[T](name + "-" + s)
  @inline def assign(value: T)         = BasicAttr(name, value.toString)

  @inline def accum(s: String): AccumAttrBuilder[T] = accum((v1, v2) => v1.toString + s + v2.toString)
  @inline def accum(reducer: (Attr.Value, Attr.Value) => Attr.Value) =
    new AccumAttrBuilder[T](name, _.toString, reducer)
}

@inline final class AccumAttrBuilder[T](
  val name: String,
  encode: T => Attr.Value,
  reduce: (Attr.Value, Attr.Value) => Attr.Value,
) extends AttrBuilder[T, AccumAttr] {
  def assign(value: T) = AccumAttr(name, encode(value), reduce)
}

// Props

@inline final class PropBuilder[T](val name: String, encode: T => Prop.Value) extends AttrBuilder[T, Prop] {
  def assign(value: T) = Prop(name, encode(value))
}

// Styles

@inline final class BasicStyleBuilder[T](val name: String) extends AnyVal with AttrBuilder[T, BasicStyle] {
  @inline def assign(value: T) = BasicStyle(name, value.toString)

  @inline def delayed: DelayedStyleBuilder[T] = new DelayedStyleBuilder[T](name)
  @inline def remove: RemoveStyleBuilder[T]   = new RemoveStyleBuilder[T](name)
  @inline def destroy: DestroyStyleBuilder[T] = new DestroyStyleBuilder[T](name)

  @inline def accum(s: String): AccumStyleBuilder[T]     = accum(_ + s + _)
  @inline def accum(reducer: (String, String) => String) = new AccumStyleBuilder[T](name, reducer)
}

@inline final class DelayedStyleBuilder[T](val name: String) extends AnyVal with AttrBuilder[T, DelayedStyle] {
  @inline def assign(value: T) = DelayedStyle(name, value.toString)
}

@inline final class RemoveStyleBuilder[T](val name: String) extends AnyVal with AttrBuilder[T, RemoveStyle] {
  @inline def assign(value: T) = RemoveStyle(name, value.toString)
}

@inline final class DestroyStyleBuilder[T](val name: String) extends AnyVal with AttrBuilder[T, DestroyStyle] {
  @inline def assign(value: T) = DestroyStyle(name, value.toString)
}

@inline final class AccumStyleBuilder[T](val name: String, reducer: (String, String) => String)
    extends AttrBuilder[T, AccumStyle] {
  def assign(value: T) = AccumStyle(name, value.toString, reducer)
}

object KeyBuilder {
  @inline def assign(key: Key.Value): Key = Key(key)
  @inline def :=(key: Key.Value): Key     = assign(key)
}
