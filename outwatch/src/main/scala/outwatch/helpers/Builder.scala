package outwatch.helpers

import colibri.{Observable, ObservableLike}
import outwatch._
import cats.Monoid

import scala.language.dynamics

trait AttributeBuilder[-T, +A] extends Any {
  def assign(value: T): A

  @inline final def :=(source: Option[T]): Option[A] = source.map(assign)
  @inline final def :=(value: T): A                  = assign(value)

  @inline final def <--[F[+_]: ObservableLike](
    source: F[Option[T]],
    @annotation.nowarn dummy: Unit = (),
  ): Observable[Option[A]] = ObservableLike[F].toObservable(source).map(_.map(assign(_)))
  @inline final def <--[F[_]: ObservableLike](source: F[_ <: T]): Observable[A] =
    ObservableLike[F].toObservable(source).map(assign(_))

  @inline final def mapResult[RA](f: A => RA): AttributeBuilder[T, RA] = AttributeBuilder(t => f(assign(t)))
  @inline final def contramap[S](f: S => T): AttributeBuilder[S, A]    = AttributeBuilder(s => assign(f(s)))

  @inline final def use[R](value: R): AttributeBuilder[R => T, A] = AttributeBuilder(f => assign(f(value)))

  @deprecated("Use builder := option instead", "1.0.0")
  @inline final def assignOption(value: Option[T]): Option[A] = this := value
  @deprecated("Use builder := option instead", "1.0.0")
  @inline final def :=?(value: Option[T]): Option[A] = this := value
  @deprecated("Use builder <-- source instead", "1.0.0")
  @inline final def <--?[F[+_]: ObservableLike](source: F[Option[T]]): Observable[Option[A]] = this <-- source
}

object AttributeBuilder {
  @inline def apply[T, A](create: T => A): AttributeBuilder[T, A] = new AttributeBuilderApply[T, A](create)
  @inline private class AttributeBuilderApply[T, A](create: T => A) extends AttributeBuilder[T, A] {
    @inline def assign(value: T): A = create(value)
  }

  @inline def ofModifier[T](create: T => VModifier): AttributeBuilder[T, VModifier]                   = ofModifierM[Any, T](create)
  @inline def ofVNode[T](create: T => VNode): AttributeBuilder[T, VNode]                              = ofVNodeM[Any, T](create)
  @inline def ofModifierM[Env, T](create: T => VModifierM[Env]): AttributeBuilder[T, VModifierM[Env]] = apply(create)
  @inline def ofVNodeM[Env, T](create: T => VNodeM[Env]): AttributeBuilder[T, VNodeM[Env]]            = apply(create)

  @inline def access[Env] = new PartiallyAppliedAccess[Env]
  @inline class PartiallyAppliedAccess[Env] {
    @inline def apply[T, A[-_]](
      builder: Env => AttributeBuilder[T, A[Any]],
    )(implicit acc: AccessEnvironment[A]): AttributeBuilder[T, A[Env]] =
      AttributeBuilder[T, A[Env]](t => AccessEnvironment[A].access[Env](env => builder(env).assign(t)))
  }
  @inline def accessM[Env] = new PartiallyAppliedAccessM[Env]
  @inline class PartiallyAppliedAccessM[Env] {
    @inline def apply[R, T, A[-_]](builder: Env => AttributeBuilder[T, A[R]])(implicit
      acc: AccessEnvironment[A],
    ): AttributeBuilder[T, A[Env with R]] = access(env => builder(env).provide(env))
  }

  @inline implicit class AccessEnvironmentOperations[Env, T, A[-_]](builder: AttributeBuilder[T, A[Env]])(implicit
    acc: AccessEnvironment[A],
  ) {
    @inline final def provide(env: Env): AttributeBuilder[T, A[Any]] =
      builder.mapResult(r => AccessEnvironment[A].provide(r)(env))
    @inline final def provideSome[REnv](map: REnv => Env): AttributeBuilder[T, A[REnv]] =
      builder.mapResult(r => AccessEnvironment[A].provideSome(r)(map))

    @inline final def useAccess[REnv]: AttributeBuilder[REnv => T, A[Env with REnv]] =
      AttributeBuilder.accessM[REnv](builder.use)
  }

  @inline implicit class MonoidOperations[T, A: Monoid](builder: AttributeBuilder[T, A]) {
    @inline final def toggle(value: => T): AttributeBuilder[Boolean, A] = AttributeBuilder { enabled =>
      if (enabled) builder.assign(value) else Monoid.empty
    }
  }
}

// Attr

@inline final class BasicAttrBuilder[T](val name: String, encode: T => Attr.Value)
    extends AttributeBuilder[T, BasicAttr] {
  def assign(value: T) = BasicAttr(name, encode(value))

  @inline def accum(s: String): AccumAttrBuilder[T]                  = accum((v1, v2) => v1.toString + s + v2.toString)
  @inline def accum(reducer: (Attr.Value, Attr.Value) => Attr.Value) = new AccumAttrBuilder[T](name, encode, reducer)
}

@inline final class DynamicAttrBuilder[T](val name: String) extends Dynamic with AttributeBuilder[T, BasicAttr] {
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
  @inline def remove: RemoveStyleBuilder[T]   = new RemoveStyleBuilder[T](name)
  @inline def destroy: DestroyStyleBuilder[T] = new DestroyStyleBuilder[T](name)

  @inline def accum(s: String): AccumStyleBuilder[T]     = accum(_ + s + _)
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

@inline final class AccumStyleBuilder[T](val name: String, reducer: (String, String) => String)
    extends AttributeBuilder[T, AccumStyle] {
  def assign(value: T) = AccumStyle(name, value.toString, reducer)
}

object KeyBuilder {
  @inline def assign(key: Key.Value): Key = Key(key)
  @inline def :=(key: Key.Value): Key     = assign(key)
}
