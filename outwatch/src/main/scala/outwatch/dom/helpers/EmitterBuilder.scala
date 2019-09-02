package outwatch.dom.helpers

import cats.{Monoid, Functor, Bifunctor}
import cats.implicits._
import monix.execution.{Cancelable, Scheduler}
import monix.execution.cancelables.CompositeCancelable
import monix.reactive.{Observable, Observer, OverflowStrategy}
import org.scalajs.dom.{Element, Event, html, svg}
import outwatch.dom._
import outwatch.ObserverBuilder

import scala.concurrent.duration.FiniteDuration
import scala.scalajs.js

trait EmitterBuilderExecution[+O, +R, +Exec <: EmitterBuilder.Execution] { self =>

  @inline def -->(observer: Observer[O]): EmitterBuilder.Result[R]

  @inline def map[T](f: O => T): EmitterBuilderExecution[T, R, Exec] = new EmitterBuilder.Map(this, f)
  @inline def collect[T](f: PartialFunction[O, T]): EmitterBuilderExecution[T, R, Exec] = new EmitterBuilder.Collect(this, f)
  @inline def filter(predicate: O => Boolean): EmitterBuilderExecution[O, R, Exec] = new EmitterBuilder.Filter(this, predicate)
  @inline def mapResult[S](f: R => S): EmitterBuilderExecution[O, S, Exec] = new EmitterBuilder.MapResult(this, f)
  @inline def transform[T](tr: Observable[O] => Observable[T]): EmitterBuilder[T, R] = new EmitterBuilder.Transform(this, tr)

  @inline def foreach(action: O => Unit): EmitterBuilder.Result[R] = -->(ObserverBuilder.fromFunction(action))
  @inline def foreach(action: => Unit): EmitterBuilder.Result[R] = foreach(_ => action)

  @inline def mapTo[T](value: => T): EmitterBuilderExecution[T, R, Exec] = map(_ => value)
  @inline def apply[T](value: T): EmitterBuilderExecution[T, R, Exec] = map(_ => value)
  @deprecated("Deprecated, use '.map' instead", "0.11.0")
  @inline def apply[T](f: O => T): EmitterBuilder[T, R] = map(f)

  @inline def apply[T](latest: Observable[T]): EmitterBuilder[T, R] = transform(_.withLatestFrom(latest)((_, u) => u))
  @inline def debounce(timeout: FiniteDuration): EmitterBuilder[O, R] = transform(_.debounce(timeout))
  @inline def async: EmitterBuilder[O, R] = transform(_.asyncBoundary(OverflowStrategy.Unbounded))
}

object EmitterBuilder {

  sealed trait Execution
  sealed trait SyncExecution extends Execution

  type Sync[+O, +R] = EmitterBuilderExecution[O, R, SyncExecution]

  @inline case class Result[+R](value: Option[R], subscription: Option[Scheduler => Cancelable]) {
    def map[S](f: R => S): Result[S] = copy[S](value.map(f))
    def withSubscription(newSubscription: Scheduler => Cancelable) =
      copy(subscription = Some(subscription.fold(newSubscription)(f => s => CompositeCancelable(f(s), newSubscription(s)))))
  }
  object Result {
    def empty: Result[Nothing] = Result(None, None)
    def apply[R](value: R): Result[R] = Result(Some(value), None)
    def apply(subscription: Scheduler => Cancelable): Result[Nothing] = Result(None, Some(subscription))
    def apply[R](value: R, subscription: Scheduler => Cancelable): Result[R] = Result(Some(value), Some(subscription))

    implicit def monoid[R : Monoid]: Monoid[Result[R]] = new Monoid[Result[R]] {
      def empty: Result[R] = Result.empty
      def combine(x: Result[R], y: Result[R]): Result[R] = Result(
        Monoid[Option[R]].combine(x.value, y.value),
        Monoid[Option[Scheduler => Cancelable]].combine(x.subscription, y.subscription)
      )
    }
  }

  @inline class Transform[+I, +O, +R](base: EmitterBuilder[I, R], transformF: Observable[I] => Observable[O]) extends EmitterBuilderExecution[O, R, Execution] {
    @inline def -->(observer: Observer[O]): Result[R] = {
      val redirected = observer.redirect(transformF)
      val result = base --> redirected
      result.withSubscription(implicit scheduler => redirected.connect())
    }
  }

  @inline class MapResult[+O, +I, +R, +Exec <: Execution](base: EmitterBuilder[O, I], mapF: I => R) extends EmitterBuilderExecution[O, R, Exec] {
    @inline def -->(observer: Observer[O]): Result[R] = (base --> observer).map(mapF)
  }

  @inline class Collect[+I, +O, +R, +Exec <: Execution](base: EmitterBuilder[I, R], collectF: PartialFunction[I, O]) extends EmitterBuilderExecution[O, R, Exec] {
    @inline def -->(observer: Observer[O]): Result[R] = base --> observer.redirectCollect(collectF)
  }

  @inline class Filter[+O, +R, +Exec <: Execution](base: EmitterBuilder[O, R], filterF: O => Boolean) extends EmitterBuilderExecution[O, R, Exec] {
    @inline def -->(observer: Observer[O]): Result[R] = base --> observer.redirectFilter(filterF)
  }

  @inline class Map[+I, +O, +R, +Exec <: Execution](base: EmitterBuilder[I, R], mapF: I => O) extends EmitterBuilderExecution[O, R, Exec] {
    @inline def -->(observer: Observer[O]): Result[R] = base --> observer.contramap(mapF)
  }

  @inline object Empty extends EmitterBuilderExecution[Nothing, Nothing, Nothing] {
    @inline def -->(observer: Observer[Nothing]): Result[Nothing] = Result.empty
  }

  @inline class Observed[+O](observable: Observable[O]) extends EmitterBuilderExecution[O, Nothing, Execution] {
    @inline def -->(observer: Observer[O]): Result[Nothing] = Result(implicit scheduler => observable subscribe observer)
  }

  @inline class Custom[+O, +R, + Exec <: Execution](create: Observer[O] => Result[R]) extends EmitterBuilderExecution[O, R, Exec] {
    @inline def -->(observer: Observer[O]): Result[R] = create(observer)
  }

  @inline def empty: EmitterBuilder.Sync[Nothing, Nothing] = Empty

  def apply[E, R](create: Observer[E] => R): EmitterBuilder.Sync[E, R] = new Custom[E, R, SyncExecution](observer => Result(create(observer)))

  def fromEvent[E <: Event](eventType: String): EmitterBuilder.Sync[E, VDomModifier] = apply[E, VDomModifier] { obs =>
    Emitter(eventType, event => { obs.onNext(event.asInstanceOf[E]); () })
  }

  @inline def fromObservable[E](observable: Observable[E]): EmitterBuilder[E, Nothing] = new Observed(observable)

  @deprecated("Use EmitterBuilder.fromEvent[E] instead", "0.11.0")
  def apply[E <: Event](eventType: String): EmitterBuilder.Sync[E, VDomModifier] = fromEvent[E](eventType)
  @deprecated("Use EmitterBuilder[E, VDomModifier] instead", "0.11.0")
  def ofModifier[E](create: Observer[E] => VDomModifier): EmitterBuilder.Sync[E, VDomModifier] = new Custom[E, VDomModifier, SyncExecution](observer => Result(create(observer)))
  @deprecated("Use EmitterBuilder[E, VNode] instead", "0.11.0")
  def ofNode[E](create: Observer[E] => VNode): EmitterBuilder.Sync[E, VNode] = new Custom[E, VNode, SyncExecution](observer => Result(create(observer)))
  @deprecated("Use EmitterBuilder[E, O] instead", "0.11.0")
  def custom[E, R](create: Observer[E] => R): EmitterBuilder.Sync[E, R] = new Custom[E, R, SyncExecution](observer => Result(create(observer)))

  implicit def monoid[T, R : Monoid, Exec <: Execution]: Monoid[EmitterBuilderExecution[T, R, Exec]] = new Monoid[EmitterBuilderExecution[T, R, Exec]] {
    def empty: EmitterBuilderExecution[T, R, Exec] = Empty
    def combine(x: EmitterBuilderExecution[T, R, Exec], y: EmitterBuilderExecution[T, R, Exec]): EmitterBuilderExecution[T, R, Exec] = new Custom[T, R, Exec](sink =>
      Monoid[Result[R]].combine(x --> sink, y --> sink)
    )
  }

  implicit def functor[T, R]: Functor[EmitterBuilder[?, R]] = new Functor[EmitterBuilder[?, R]] {
    def map[A, B](fa: EmitterBuilder[A,R])(f: A => B): EmitterBuilder[B,R] = fa.map(f)
  }

  implicit def bifunctor[T, R]: Bifunctor[EmitterBuilder] = new Bifunctor[EmitterBuilder] {
    def bimap[A, B, C, D](fab: EmitterBuilder[A,B])(f: A => C, g: B => D): EmitterBuilder[C,D] = fab.map(f).mapResult(g)
  }

  implicit class EventActions[O <: Event, R](val builder: EmitterBuilder.Sync[O, R]) extends AnyVal {
    def preventDefault: EmitterBuilder.Sync[O, R] = builder.map { e => e.preventDefault; e }
    def stopPropagation: EmitterBuilder.Sync[O, R] = builder.map { e => e.stopPropagation; e }
    def stopImmediatePropagation: EmitterBuilder.Sync[O, R] = builder.map { e => e.stopImmediatePropagation; e }
  }

  @inline implicit class TargetAsInput[O <: Event, R, Exec <: Execution](builder: EmitterBuilderExecution[O, R, Exec]) {
    object target {
      @inline def value: EmitterBuilderExecution[String, R, Exec] = builder.map(_.target.asInstanceOf[html.Input].value)
      @inline def valueAsNumber: EmitterBuilderExecution[Double, R, Exec] = builder.map(_.target.asInstanceOf[html.Input].valueAsNumber)
      @inline def checked: EmitterBuilderExecution[Boolean, R, Exec] = builder.map(_.target.asInstanceOf[html.Input].checked)
    }
  }

  implicit class CurrentTargetAsInput[O <: Event, R, Exec <: Execution](val builder: EmitterBuilderExecution[O, R, Exec]) extends AnyVal {
    def value: EmitterBuilderExecution[String, R, Exec] = builder.map(e => e.currentTarget.asInstanceOf[html.Input].value)
    def valueAsNumber: EmitterBuilderExecution[Double, R, Exec] = builder.map(e => e.currentTarget.asInstanceOf[html.Input].valueAsNumber)
    def checked: EmitterBuilderExecution[Boolean, R, Exec] = builder.map(e => e.currentTarget.asInstanceOf[html.Input].checked)
  }

  implicit class TypedElements[O <: Element, R](val builder: EmitterBuilder[O, R]) extends AnyVal {
    def asHtml: EmitterBuilder[html.Element, R] = builder.map(_.asInstanceOf[html.Element])
    def asSvg: EmitterBuilder[svg.Element, R] = builder.map(_.asInstanceOf[svg.Element])
  }

  implicit class TypedElementTuples[E <: Element, R, Exec <: Execution](val builder: EmitterBuilderExecution[(E,E), R, Exec]) extends AnyVal {
    def asHtml: EmitterBuilderExecution[(html.Element, html.Element), R, Exec] = builder.map(_.asInstanceOf[(html.Element, html.Element)])
    def asSvg: EmitterBuilderExecution[(svg.Element, svg.Element), R, Exec] = builder.map(_.asInstanceOf[(svg.Element, svg.Element)])
  }

  implicit class ModifierActions[O](val builder: EmitterBuilder[O, VDomModifier]) extends AnyVal {
      def withLatest[T](emitter: EmitterBuilder[T, VDomModifier]): EmitterBuilder[(O, T), VDomModifier] = new Custom[(O, T), VDomModifier, Execution]({ sink =>
      Result(VDomModifier.delay {
        var lastValue: js.UndefOr[T] = js.undefined
        VDomModifier(
          emitter foreach { lastValue = _ },
          builder.foreach { o =>
            lastValue.foreach { t =>
              sink.onNext((o, t))
            }
          }
        )
      })
    })

    def useLatest[T](emitter: EmitterBuilder[T, VDomModifier]): EmitterBuilder[T, VDomModifier] = withLatest(emitter).map(_._2)
  }
}
