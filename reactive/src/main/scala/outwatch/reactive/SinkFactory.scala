package outwatch.reactive

//TODO: should have createsink typeclass to create without lift overhead
@inline final class SinkFactory[S[_] : LiftSink] {
  @inline def empty: S[Any] = SinkObserver.empty.liftSink[S]
  @inline def create[A](consume: A => Unit, failure: Throwable => Unit = UnhandledErrorReporter.errorSubject.onError(_)): S[A] = SinkObserver.create(consume, failure).liftSink[S]
}
