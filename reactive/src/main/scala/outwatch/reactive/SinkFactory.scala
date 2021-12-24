package outwatch.reactive

import colibri._

//TODO: should have createsink typeclass to create without lift overhead
@inline final class SinkFactory[S[_] : LiftSink] {
  @inline def empty: S[Any] = LiftSink[S].lift(Observer.empty)
  @inline def create[A](consume: A => Unit): S[A] = LiftSink[S].lift(Observer.create(consume))
  @inline def create[A](consume: A => Unit, failure: Throwable => Unit): S[A] = LiftSink[S].lift(Observer.create(consume, failure))
}
