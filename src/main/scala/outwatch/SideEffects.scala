package outwatch

import monix.execution.Scheduler
import monix.execution.Ack.Continue
import cats.effect.IO

trait SideEffects {
  def sideEffect[T](f: T => Unit)(implicit s: Scheduler): Sink[T] = Sink.create[T] { e => IO { f(e); Continue } }(s)
  def sideEffect[S,T](f: (S,T) => Unit)(implicit s: Scheduler): Sink[(S,T)] = Sink.create[(S,T)] { e => IO { f(e._1, e._2); Continue } }(s)
  def sideEffect(f: => Unit)(implicit s: Scheduler): Sink[Any] = Sink.create[Any] { e => IO { f; Continue } }(s)
}
