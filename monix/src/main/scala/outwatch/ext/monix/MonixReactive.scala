package outwatch.ext.monix

import _root_.monix.eval.Coeval
import _root_.monix.execution.{Ack, Scheduler, Cancelable}
import _root_.monix.reactive.{OverflowStrategy, Observable, Observer}
import _root_.monix.reactive.subjects.Var

import outwatch.effect._
import outwatch.reactive._

trait MonixReactive {

  // Sink

  implicit object monixVariableSink extends Sink[Var] {
    def onNext[A](sink: Var[A])(value: A): Unit = { sink := value; () }

    def onError[A](sink: Var[A])(error: Throwable): Unit = throw error
  }

  //TODO: unsafe because of backpressure and ignored ACK
  implicit object monixObserverSink extends Sink[Observer] {
    def onNext[A](sink: Observer[A])(value: A): Unit = {
      sink.onNext(value)
      ()
    }

    def onError[A](sink: Observer[A])(error: Throwable): Unit = {
      sink.onError(error)
      ()
    }
  }

  implicit object monixObserverLiftSink extends LiftSink[Observer.Sync] {
    def lift[G[_] : Sink, A](sink: G[A]): Observer.Sync[A] = new Observer.Sync[A] {
      def onNext(value: A): Ack = { Sink[G].onNext(sink)(value); Ack.Continue }
      def onError(error: Throwable): Unit = Sink[G].onError(sink)(error)
      def onComplete(): Unit = ()
    }
  }

  // Source

  implicit def monixObservableSource(implicit scheduler: Scheduler): Source[Observable] = new Source[Observable] {
    def subscribe[G[_] : Sink, A](source: Observable[A])(sink: G[_ >: A]): Subscription = {
      val sub = source.subscribe(
        { v => Sink[G].onNext(sink)(v); Ack.Continue },
        Sink[G].onError(sink)
      )
      Subscription(sub.cancel)
    }
  }

  implicit object monixObservableLiftSource extends LiftSource[Observable] {
    def lift[G[_] : Source, A](source: G[A]): Observable[A] = Observable.create[A](OverflowStrategy.Unbounded) { observer =>
      val sub = Source[G].subscribe(source)(observer)
      Cancelable(() => sub.cancel())
    }
  }

  // Subscription
  implicit object monixCancelSubscription extends CancelSubscription[Cancelable] {
    def cancel(subscription: Cancelable): Unit = subscription.cancel()
  }

  // Handler
  type MonixProHandler[-I, +O] = Observable[O] with Observer[I]
  type MonixHandler[T] = MonixProHandler[T,T]

  implicit object monixCreateHandler extends CreateHandler[MonixHandler] {
    def variable[A]: MonixHandler[A] = MonixHandler.create[A]
    def variable[A](seed: A): MonixHandler[A] = MonixHandler.create[A](seed)
    def publisher[A]: MonixHandler[A] = MonixHandler.publish[A]
  }

  implicit object monixCreateProHandler extends CreateProHandler[MonixProHandler] {
    def apply[I,O](f: I => O): MonixProHandler[I,O] = MonixProHandler.create(f)
    def apply[I,O](seed: I)(f: I => O): MonixProHandler[I,O] = MonixProHandler.create(seed)(f)
    @inline def from[SI[_] : Sink, SO[_] : Source, I,O](sink: SI[I], source: SO[O]): MonixProHandler[I, O] = MonixProHandler(LiftSink[Observer].lift(sink), LiftSource[Observable].lift(source))
  }

  val handler = HandlerEnvironment[Observer, Observable, MonixHandler, MonixProHandler]

  implicit object coeval extends RunSyncEffect[Coeval] {
    @inline def unsafeRun[T](effect: Coeval[T]): T = effect.apply()
  }
}
