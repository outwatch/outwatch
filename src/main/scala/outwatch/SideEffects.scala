package outwatch

import monix.execution.Ack.Continue
import monix.execution.{Ack, Scheduler}
import monix.reactive.Observer

import scala.concurrent.Future

object SideEffects {
  def observerFromFunction[T](f:T => Unit):Observer[T] = new Observer[T] {
    override def onNext(elem: T): Future[Ack] = { f(elem); Continue }
    override def onError(ex: Throwable): Unit = throw ex
    override def onComplete(): Unit = {}
  }
}

trait SideEffects {
  @deprecated("use emitter.handleWith(f) instead of emitter --> sideEffect(f).", "")
  def sideEffect[T](f: T => Unit)(implicit s: Scheduler): Observer[T] = SideEffects.observerFromFunction(f)
  @deprecated("use emitter.handleWith(f) instead of emitter --> sideEffect(f).", "")
  def sideEffect[S, T](f: (S, T) => Unit)(implicit s: Scheduler): Observer[(S, T)] = SideEffects.observerFromFunction(f.tupled)
  @deprecated("use emitter.handleWith(f) instead of emitter --> sideEffect(f).", "")
  def sideEffect(f: => Unit)(implicit s: Scheduler): Observer[Any] = SideEffects.observerFromFunction(_ => f)
}
