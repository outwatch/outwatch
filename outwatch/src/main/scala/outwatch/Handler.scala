package outwatch

import cats.effect.IO
import monix.execution.{Ack, Cancelable, Scheduler}
import monix.reactive.observers.Subscriber
import monix.reactive.subjects.{BehaviorSubject, ReplaySubject}
import monix.reactive.{Observable, Observer}
import outwatch.dom.helpers.BehaviorProHandler
import outwatch.dom.{AsObserver, AsValueObservable, ObservableWithInitialValue, ValueObservable}

import scala.concurrent.Future

object Handler {
  @inline def empty[T]:IO[Handler[T]] = create[T]

  @inline def create[T]:IO[Handler[T]] = IO(unsafe[T])
  @inline def create[T](seed:T):IO[Handler[T]] = IO(unsafe[T](seed))

  def unsafe[T]:Handler[T] = new BehaviorProHandler[T](None)
  def unsafe[T](seed:T):Handler[T] = new BehaviorProHandler[T](Some(seed))

  @inline def apply[T,F[_]: AsValueObservable : AsObserver](handler: F[T]): Handler[T] = ProHandler(AsObserver(handler), ValueObservable.from(handler))
}

object ProHandler {
  def create[I,O](seed: I, f: I => O): IO[ProHandler[I,O]] = for {
    handler <- Handler.create[I](seed)
  } yield handler.mapObservable[O](f)

  def create[I,O](f: I => O): IO[ProHandler[I,O]] = for {
    handler <- Handler.create[I]
  } yield handler.mapObservable[O](f)

  @inline def apply[I,O,F[_]: AsValueObservable](observer:Observer[I], observable: F[O]):ProHandler[I,O] = apply(observer, ValueObservable.from(observable))
  def apply[I,O](observer:Observer[I], valueObservable: ValueObservable[O]):ProHandler[I,O] = new ValueObservable[O] with Observer[I] {
    override def onNext(elem: I): Future[Ack] = observer.onNext(elem)
    override def onError(ex: Throwable): Unit = observer.onError(ex)
    override def onComplete(): Unit = observer.onComplete()
    override def value(): ObservableWithInitialValue[O] = valueObservable.value()
  }
  @inline def connectable[I,O,F[_]: AsValueObservable](observer:Observer[I] with ReactiveConnectable, observable: F[O]):ProHandler[I,O] with ReactiveConnectable = connectable(observer, ValueObservable.from(observable))
  def connectable[I,O](observer: Observer[I] with ReactiveConnectable, valueObservable: ValueObservable[O]):ProHandler[I,O] with ReactiveConnectable = new ValueObservable[O] with Observer[I] with ReactiveConnectable {
    override def onNext(elem: I): Future[Ack] = observer.onNext(elem)
    override def onError(ex: Throwable): Unit = observer.onError(ex)
    override def onComplete(): Unit = observer.onComplete()
    override def connect()(implicit scheduler: Scheduler): Cancelable = observer.connect()
    override def value(): ObservableWithInitialValue[O] = valueObservable.value()
  }
}
