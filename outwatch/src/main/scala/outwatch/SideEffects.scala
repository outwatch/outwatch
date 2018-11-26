package outwatch

import monix.execution.Scheduler
import monix.reactive.Observer

trait SideEffects {
  @deprecated("use emitter.foreach(f) instead of emitter --> sideEffect(f).", "")
  def sideEffect[T](f: T => Unit)(implicit s: Scheduler): Observer[T] = Sink.fromFunction(f)
  @deprecated("use emitter.foreach(f) instead of emitter --> sideEffect(f).", "")
  def sideEffect[S, T](f: (S, T) => Unit)(implicit s: Scheduler): Observer[(S, T)] = Sink.fromFunction(f.tupled)
  @deprecated("use emitter.foreach(f) instead of emitter --> sideEffect(f).", "")
  def sideEffect(f: => Unit)(implicit s: Scheduler): Observer[Any] = Sink.fromFunction(_ => f)
}
