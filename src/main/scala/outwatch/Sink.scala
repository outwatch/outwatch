package outwatch

import rxscalajs.facade.SubjectFacade
import rxscalajs.subscription.AnonymousSubscription
import rxscalajs.{Observable, Observer, Subject}

sealed trait Sink[-T] extends Any {

  /**
    * Use this function with caution!
    * This function pipes all of the Observable's emissions into this Sink
    * Using this method is inherently impure and can cause memory leaks, if subscription
    * isn't handled correctly. For more guaranteed safety, use Sink.redirect() instead.
    */
  def <--(observable: Observable[T]): AnonymousSubscription = {
    observable.subscribe(observer)
  }

  private[outwatch] def observer: Observer[T]

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
  private final case class SubjectSink[T]() extends Subject[T](new SubjectFacade) with Sink[T] {
    override private[outwatch] def observer = this
  }
  private final case class ObservableSink[T]
  (oldSink: Sink[T], stream: Observable[T]) extends Observable[T](stream.inner) with Sink[T] {
    override private[outwatch] def observer = oldSink.observer
  }

  /**
    * Creates a new Sink from Scratch.
    * This function takes another function as its parameter that will be executed every time the Sink receives an emitted value.
    * @param onNext the function to be executed on every emission
    * @tparam T the type parameter of the consumed elements.
    * @return a Sink that consumes elements of type T.
    */
  def create[T](onNext: T => Unit): Sink[T] = {
    val subject = Subject[T]
    subject.subscribe(onNext)
    ObserverSink(subject)
  }

  /**
    * Creates a Handler that is both Observable and Sink.
    * An Observable with Sink is an Observable that can also receive Events, i.e. it’s both a Source and a Sink of events.
    * If you’re familiar with Rx, they’re very similar to Subjects.
    * This function also allows you to create initial values for your newly created Handler.
    * This is equivalent to calling `startWithMany` with the given values.
    * @param seeds a sequence of initial values that the Handler will emit.
    * @tparam T the type parameter of the elements
    * @return the newly created Handler.
    */
  def createHandler[T](seeds: T*): Observable[T] with Sink[T] = {
    val handler = new SubjectSink[T]
    if (seeds.nonEmpty) {
      ObservableSink[T](handler, handler.startWithMany(seeds: _*))
    }
    else
      handler
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
    val forward = Sink.createHandler[R]()

    val projected = sink match {
      case subject@SubjectSink() => project(forward)
        .takeUntil(subject.ignoreElements.defaultIfEmpty(()))
      case observable@ObservableSink(_,_) => project(forward)
        .takeUntil(observable.ignoreElements.defaultIfEmpty(()))
      case ObserverSink(_) => project(forward)
    }
    projected.subscribe(sink.observer)

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
    val r = Sink.createHandler[R]()
    val u = Sink.createHandler[U]()

    val projected = sink match {
      case subject@SubjectSink() => project(r,u)
        .takeUntil(subject.ignoreElements.defaultIfEmpty(()))
      case observable@ObservableSink(_, _) => project(r,u)
        .takeUntil(observable.ignoreElements.defaultIfEmpty(()))
      case ObserverSink(_) => project(r,u)
    }
    projected.subscribe(sink.observer)

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
    val r = Sink.createHandler[R]()
    val u = Sink.createHandler[U]()
    val v = Sink.createHandler[V]()

    val projected = sink match {
      case subject@SubjectSink() => project(r,u,v)
        .takeUntil(subject.ignoreElements.defaultIfEmpty(()))
      case observable@ObservableSink(_, _) => project(r,u,v)
        .takeUntil(observable.ignoreElements.defaultIfEmpty(()))
      case ObserverSink(_) => project(r,u,v)
    }
    projected.subscribe(sink.observer)

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

final case class ObserverSink[-T](observer: Observer[T]) extends AnyVal with Sink[T]


