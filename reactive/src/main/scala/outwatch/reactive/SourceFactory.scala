package outwatch.reactive

import colibri._

//TODO: should have creataesource typeclass to create without lift overhead
@inline class SourceFactory[S[_] : LiftSource] {
  @inline def empty: S[Nothing] = LiftSource[S].lift(Observable.empty)
  @inline def apply[T](value: T): S[T] = LiftSource[S].lift(Observable[T](value))
  @inline def fromIterable[T](values: Iterable[T]): S[T] = LiftSource[S].lift(Observable.fromIterable[T](values))
  @inline def create[T](produce: Observer[T] => Cancelable): S[T] = LiftSource[S].lift(Observable.create[T](produce))
}
