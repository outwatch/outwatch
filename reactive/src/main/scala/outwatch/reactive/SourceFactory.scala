package outwatch.reactive

import colibri._

//TODO: should have creataesource typeclass to create without lift overhead
@inline class SourceFactory[S[_] : LiftSource] {
  @inline def empty: S[Nothing] = Observable.empty.liftSource[S]
  @inline def apply[T](value: T): S[T] = Observable[T](value).liftSource[S]
  @inline def fromIterable[T](values: Iterable[T]): S[T] = Observable.fromIterable[T](values).liftSource[S]
  @inline def create[T](produce: Observer[T] => Cancelable): S[T] = Observable.create[T](produce).liftSource[S]
}
