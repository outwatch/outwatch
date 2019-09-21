package outwatch.reactive

//TODO: should have creataesource typeclass to create without lift overhead
@inline class SourceFactory[S[_] : LiftSource] {
  @inline def empty: S[Nothing] = SourceStream.empty.liftSource[S]
  @inline def apply[T](value: T): S[T] = SourceStream[T](value).liftSource[S]
  @inline def fromIterable[T](values: Iterable[T]): S[T] = SourceStream.fromIterable[T](values).liftSource[S]
  @inline def create[T](produce: SinkObserver[T] => Subscription): S[T] = SourceStream.create[T](produce).liftSource[S]
}
