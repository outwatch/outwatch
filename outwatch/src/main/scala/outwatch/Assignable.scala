package outwatch

import colibri.{Source, Observable}
// import cats.{Id, Functor}
import cats.{Id}
import outwatch.helpers.AttributeBuilder

trait Assignable[-F[_]] {
  def assign[T](builder: AttributeBuilder[T, ModifierM[Any]])(input: F[T]): ModifierM[Any]
}

object Assignable {
  @inline def apply[F[_]](implicit assign: Assignable[F]): Assignable[F] = assign

  implicit object id extends Assignable[Id] {
    @inline def assign[T](builder: AttributeBuilder[T, ModifierM[Any]])(input: Id[T]): ModifierM[Any] = builder.assign(input)
  }

  implicit object observable extends Assignable[Observable] {
    @inline def assign[T](builder: AttributeBuilder[T, ModifierM[Any]])(input: Observable[T]): ModifierM[Any] = input.map(builder.assign)
  }

  implicit def functor[F[_]: Source]: Assignable[F] = new Assignable[F] {
    @inline def assign[T](builder: AttributeBuilder[T, ModifierM[Any]])(input: F[T]): ModifierM[Any] = Observable.map(input)(builder.assign)
  }

  // implicit def functor2[F[_]: Functor, G[_]: Functor]: Assignable[Lambda[T => F[G[T]]]] = new Assignable[Lambda[T => F[G[T]]]] {
  //   @inline def assign[T](builder: AttributeBuilder[T, ModifierM[Any]])(input: F[G[T]]): ModifierM[Any] = Functor[F].map(input)(x => Functor[G].map(x)(builder.assign))
  // }
}
