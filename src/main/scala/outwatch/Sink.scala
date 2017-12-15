package outwatch

import cats.effect.IO
import monix.execution.Scheduler
import monix.execution.{Ack, Cancelable}
import monix.reactive.observers.{SafeSubscriber, Subscriber}
import monix.reactive.subjects.PublishSubject
import monix.reactive.{Observable, Observer}

import scala.concurrent.Future


sealed trait Sink[-T] extends Any {

  /**
    * Use this function with caution!
    * This function pipes all of the Observable's emissions into this Sink
    * Using this method is inherently impure and can cause memory leaks, if subscription
    * isn't handled correctly. For more guaranteed safety, use Sink.redirect() instead.
    */
  def <--(observable: Observable[T]): IO[Cancelable] = IO {
    observable.subscribe(observer)
  }

  private[outwatch] def observer: Subscriber[T]

  /**
    * Creates a new sink. That sink will transform the values it receives and then forward them along to this sink.
    * The transformation is described by a function from an Observable to another Observable, i.e. an operator on Observable.
    * This function applies the operator to the newly created sink and forwards the value to the original sink.
    * @param projection the operator to use
    * @tparam R the type of the resulting sink
    * @return the resulting sink, that will forward the values
    */
  def redirect[R](projection: Observable[R] => Observable[T]): Sink[R] = {
    Sink.redirect(this)(projection)
  }
  /**
    * Creates a new sink. That sink will transform each value it receives and then forward it along to the this sink.
    * The transformation is a simple map from one type to another, i.e. a 'map'.
    * This is equivalent to `contramap` on a `Contravariant` functor, since `Sink`s are contravariant in nature.
    * @param projection the mapping to perform before forwarding
    * @tparam R the type of the resulting sink
    * @return the resulting sink, that will forward the values
    */
  def redirectMap[R](projection: R => T): Sink[R] = {
    Sink.redirectMap(this)(projection)
  }
}

object Sink {

  private[outwatch] final case class ObservableSink[-I, +O](oldSink: Sink[I], stream: Observable[O]) extends Observable[O] with Sink[I] {
    override private[outwatch] def observer = oldSink.observer

    override def unsafeSubscribeFn(subscriber: Subscriber[O]): Cancelable = stream.unsafeSubscribeFn(subscriber)
  }

  private[outwatch] final case class SubjectSink[T]()(implicit scheduler: Scheduler) extends Observable[T] with Sink[T] {
    private val subject = PublishSubject[T]

    override private[outwatch] def observer = SafeSubscriber(Subscriber(subject, scheduler))

    override def unsafeSubscribeFn(subscriber: Subscriber[T]): Cancelable = subject.unsafeSubscribeFn(subscriber)
  }

  /**
    * Creates a new Sink from Scratch.
    * This function takes another function as its parameter that will be executed every time the Sink receives an emitted value.
    *
    * @param next the function to be executed on every emission
    * @param error the function to be executed on error
    * @param complete the function to be executed on completion
    * @tparam T the type parameter of the consumed elements.
    * @return a Sink that consumes elements of type T.
    */
  def create[T](next: T => IO[Future[Ack]],
                error: Throwable => IO[Unit] = _ => IO.pure(()),
                complete: () => IO[Unit] = () => IO.pure(())
               )(implicit s: Scheduler): Sink[T] = {
    val sink = ObserverSink(
      new Observer[T] {
        override def onNext(t: T): Future[Ack] = next(t).unsafeRunSync()
        override def onError(ex: Throwable): Unit = error(ex).unsafeRunSync()
        override def onComplete(): Unit = complete().unsafeRunSync()
      }
    )
    sink
  }


  private def completionObservable[T](sink: Sink[T]): Option[Observable[Unit]] = {
    sink match {
      case subject@SubjectSink() =>
        Some(subject.ignoreElements.defaultIfEmpty(()))
      case observable@ObservableSink(_, _) =>
        Some(observable.ignoreElements.defaultIfEmpty(()))
      case ObserverSink(_) =>
        None
    }
  }

  /**
    * Creates a new sink. This sink will transform the values it receives and then forward them along to the passed sink.
    * The transformation is described by a function from an Observable to another Observable, i.e. an operator on Observable.
    * This function applies the operator to the newly created sink and forwards the value to the original sink.
    * @param sink the Sink to forward values to
    * @param project the operator to use
    * @tparam R the type of the resulting sink
    * @tparam T the type of the original sink
    * @return the resulting sink, that will forward the values
    */
  def redirect[T,R](sink: Sink[T])(project: Observable[R] => Observable[T]): Sink[R] = {
    implicit val scheduler = sink.observer.scheduler
    val forward = SubjectSink[R]()

    completionObservable(sink)
      .fold(project(forward))(completed => project(forward).takeUntil(completed))
      .subscribe(sink.observer)

    forward
  }

  /**
    * Creates two new sinks. These sinks will transform the values it receives and then forward them along to the passed sink.
    * The transformation is described by a function from two Observables to another Observable, i.e. an operator on Observable.
    * (e.g. `merge`)
    * This function applies the operator to the newly created sinks and forwards the value to the original sink.
    * @param sink the Sink to forward values to
    * @param project the operator to use
    * @tparam R the type of one of the resulting sinks
    * @tparam U the type of the other of the resulting sinks
    * @tparam T the type of the original sink
    * @return the two resulting sinks, that will forward the values
    */
  def redirect2[T,U,R](sink: Sink[T])(project: (Observable[R], Observable[U]) => Observable[T]): (Sink[R], Sink[U]) = {
    implicit val scheduler = sink.observer.scheduler
    val r = SubjectSink[R]()
    val u = SubjectSink[U]()

    completionObservable(sink)
      .fold(project(r, u))(completed => project(r, u).takeUntil(completed))
      .subscribe(sink.observer)

    (r, u)
  }

  /**
    * Creates three new sinks. These sinks will transform the values it receives and then forward them along to the passed sink.
    * The transformation is described by a function from three Observables to another Observable, i.e. an operator on Observable.
    * (e.g. `combineLatest`)
    * This function applies the operator to the newly created sinks and forwards the value to the original sink.
    * @param sink the Sink to forward values to
    * @param project the operator to use
    * @tparam R the type of one of the resulting sinks
    * @tparam U the type of one of the other of the resulting sinks
    * @tparam V the type of the other of the resulting sinks
    * @tparam T the type of the original sink
    * @return the two resulting sinks, that will forward the values
    */
  def redirect3[T,U,V,R](sink: Sink[T])
                       (project: (Observable[R], Observable[U], Observable[V]) => Observable[T])
                       :(Sink[R], Sink[U], Sink[V]) = {
    implicit val scheduler = sink.observer.scheduler
    val r = SubjectSink[R]()
    val u = SubjectSink[U]()
    val v = SubjectSink[V]()

    completionObservable(sink)
      .fold(project(r, u, v))(completed => project(r, u, v).takeUntil(completed))
      .subscribe(sink.observer)

    (r, u, v)
  }

  /**
    * Creates a new sink. This sink will transform each value it receives and then forward it along to the passed sink.
    * The transformation is a simple map from one type to another, i.e. a 'map'.
    * This is equivalent to `contramap` on a `Contravariant` functor, since `Sink`s are contravariant in nature.
    * @param sink the Sink to forward values to
    * @param f the mapping to perform before forwarding
    * @tparam R the type of the resulting sink
    * @tparam T the type of the original sink
    * @return the resulting sink, that will forward the values
    */
  def redirectMap[T, R](sink: Sink[T])(f: R => T): Sink[R] = {
    redirect(sink)(_.map(f))
  }

}

final case class ObserverSink[-T](obs: Observer[T])(implicit s: Scheduler) extends Sink[T] {
  override val observer = Subscriber(obs, s)
}
