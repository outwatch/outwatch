package outwatch.reactive

//TODO: should have createsink typeclass to create without lift overhead
@inline final class SinkFactory[S[_] : LiftSink] {
  @inline def empty: S[Any] = SinkObserver.empty.lift[S]
  @inline def create[A](consume: A => Unit, failure: Throwable => Unit): S[A] = SinkObserver.create(consume, failure).lift[S]
}
