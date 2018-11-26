package outwatch.dom.helpers

import cats.effect.IO
import monix.execution.{Cancelable, Scheduler}
import monix.reactive.observers.Subscriber
import monix.reactive.{Observable, Observer, OverflowStrategy}
import org.scalajs.dom.{Element, Event, html, svg}
import outwatch.ConnectableObserver
import outwatch.dom._

import scala.concurrent.duration.FiniteDuration
import scala.scalajs.js


trait EmitterBuilder[+O, +R] { self =>

  def transform[T](tr: Observable[O] => Observable[T]): EmitterBuilder[T, R]
  def -->(observer: Observer[O]): R
  def map[T](f: O => T): EmitterBuilder[T, R]
  def filter(predicate: O => Boolean): EmitterBuilder[O, R]
  def collect[T](f: PartialFunction[O, T]): EmitterBuilder[T, R]

  @inline def foreach(action: O => Unit): R = -->(Sink.fromFunction(action))
  @inline def foreach(action: => Unit): R = foreach(_ => action)
  @inline def apply[T](value: T): EmitterBuilder[T, R] = map(_ => value)
  @inline def mapTo[T](value: => T): EmitterBuilder[T, R] = map(_ => value)
  @inline def apply[T](latest: Observable[T]): EmitterBuilder[T, R] = transform(_.withLatestFrom(latest)((_, u) => u))
  @inline def debounce(timeout: FiniteDuration): EmitterBuilder[O, R] = transform(_.debounce(timeout))
  @inline def async: EmitterBuilder[O, R] = transform(_.asyncBoundary(OverflowStrategy.Unbounded))
  @deprecated("Deprecated, use '.map' instead", "0.11.0")
  @inline def apply[T](f: O => T): EmitterBuilder[T, R] = map(f)
}

object EmitterBuilder {
  @inline def apply[E <: Event](eventType: String): CustomEmitterBuilder[E, VDomModifier] = ofModifier[E](obs => Emitter(eventType, event => obs.onNext(event.asInstanceOf[E])))

  @inline def fromObservable[E](observable: Observable[E]): EmitterBuilder[E, VDomModifier] = new ObservableEmitterBuilder[E, VDomModifier](observable, managedAction)

  def ofModifier[E](create: Observer[E] => VDomModifier): CustomEmitterBuilder[E, VDomModifier] = new CustomEmitterBuilder[E, VDomModifier]({
    case o: ConnectableObserver[E] => VDomModifier(managedAction(implicit scheduler => o.connect()), create(o))
    case o: Observer[E] => create(o)
  })

  def empty: EmptyEmitterBuilder[VDomModifier] = new EmptyEmitterBuilder[VDomModifier](VDomModifier.empty)

  implicit class EventActions[O <: Event, R](val builder: SyncEmitterBuilder[O, R]) extends AnyVal {
    def preventDefault: SyncEmitterBuilder[O, R] = builder.map { e => e.preventDefault; e }
    def stopPropagation: SyncEmitterBuilder[O, R] = builder.map { e => e.stopPropagation; e }
    def stopImmediatePropagation: SyncEmitterBuilder[O, R] = builder.map { e => e.stopImmediatePropagation; e }
  }

  implicit class TargetAsInput[O <: Event, R](builder: EmitterBuilder[O, R]) {
    object target {
      def value: EmitterBuilder[String, R] = builder.map(_.target.asInstanceOf[html.Input].value)
      def valueAsNumber: EmitterBuilder[Double, R] = builder.map(_.target.asInstanceOf[html.Input].valueAsNumber)
      def checked: EmitterBuilder[Boolean, R] = builder.map(_.target.asInstanceOf[html.Input].checked)
    }
    def value: EmitterBuilder[String, R] = builder.map(e => e.currentTarget.asInstanceOf[html.Input].value)
    def valueAsNumber: EmitterBuilder[Double, R] = builder.map(e => e.currentTarget.asInstanceOf[html.Input].valueAsNumber)
    def checked: EmitterBuilder[Boolean, R] = builder.map(e => e.currentTarget.asInstanceOf[html.Input].checked)
  }

  implicit class TypedElements[O <: Element, R](val builder: EmitterBuilder[O, R]) extends AnyVal {
    def asHtml: EmitterBuilder[html.Element, R] = builder.map(_.asInstanceOf[html.Element])
    def asSvg: EmitterBuilder[svg.Element, R] = builder.map(_.asInstanceOf[svg.Element])
  }

  implicit class TypedElementTuples[E <: Element, R](val builder: EmitterBuilder[(E,E), R]) extends AnyVal {
    def asHtml: EmitterBuilder[(html.Element, html.Element), R] = builder.map(_.asInstanceOf[(html.Element, html.Element)])
    def asSvg: EmitterBuilder[(svg.Element, svg.Element), R] = builder.map(_.asInstanceOf[(svg.Element, svg.Element)])
  }

  implicit class ModifierActions[O](val builder: EmitterBuilder[O, VDomModifier]) extends AnyVal {
    def useLatest[T](emitter: EmitterBuilder[T, VDomModifier]): EmitterBuilder[T, VDomModifier] = new CustomEmitterBuilder[T, VDomModifier]({ sink =>
      IO {
        var lastValue: js.UndefOr[T] = js.undefined
        VDomModifier(
          emitter foreach { lastValue = _ },
          builder.foreach { _ =>
            lastValue.foreach { t =>
              sink.onNext(t)
            }
          }
        )
      }
    })
  }
}

trait SyncEmitterBuilder[+O, +R] extends EmitterBuilder[O, R] {
  def transformSync[T](f: Option[O] => Option[T]): SyncEmitterBuilder[T, R]
  @inline def map[T](f: O => T): SyncEmitterBuilder[T, R] = transformSync(_.map(f))
  @inline def collect[T](f: PartialFunction[O, T]): SyncEmitterBuilder[T, R] = transformSync(_.collect(f))
  @inline def filter(predicate: O => Boolean): SyncEmitterBuilder[O, R] = transformSync(_.filter(predicate))
}
trait AsyncEmitterBuilder[+O, +R] extends EmitterBuilder[O, R] { self =>
  @inline def map[T](f: O => T): EmitterBuilder[T, R] = transform(_.map(f))
  @inline def filter(predicate: O => Boolean): EmitterBuilder[O, R] = transform(_.filter(predicate))
  @inline def collect[T](f: PartialFunction[O, T]): EmitterBuilder[T, R] = transform(_.collect(f))
}

final class CustomEmitterBuilder[E, +R] private[outwatch](create: Observer[E] => R) extends SyncEmitterBuilder[E, R] {
  def transform[T](tr: Observable[E] => Observable[T]): EmitterBuilder[T, R] = new TransformingEmitterBuilder[E, T, R](tr, create)
  def transformSync[T](tr: Option[E] => Option[T]): SyncEmitterBuilder[T, R] = new FunctionEmitterBuilder[E, T, R](tr, create)
  def -->(observer: Observer[E]): R = create(observer)
}

final class FunctionEmitterBuilder[E, +O, +R] private[outwatch](transformer: Option[E] => Option[O], create: Observer[E] => R) extends SyncEmitterBuilder[O, R] {
  def transform[T](tr: Observable[O] => Observable[T]): EmitterBuilder[T, R] = new TransformingEmitterBuilder[O, T, R](tr, observer => create(new ConnectableObserver[E](Sink.fromFunction(e => transformer(Some(e)).foreach(observer.onNext(_))), observer.connect()(_))))
  def transformSync[T](tr: Option[O] => Option[T]): SyncEmitterBuilder[T, R] = new FunctionEmitterBuilder(transformer andThen tr, create)
  def -->(observer: Observer[O]): R = create(observer.redirectMapMaybe(e => transformer(Some(e))))
}

final class TransformingEmitterBuilder[E, +O, +R] private[outwatch](transformer: Observable[E] => Observable[O], create: ConnectableObserver[E] => R) extends AsyncEmitterBuilder[O, R] {
  def transform[T](tr: Observable[O] => Observable[T]): EmitterBuilder[T, R] = new TransformingEmitterBuilder(transformer andThen tr, create)
  def -->(observer: Observer[O]): R = create(observer.redirect(transformer))
}

final class ObservableEmitterBuilder[+E, +R] private[outwatch](observable: Observable[E], create: (Scheduler => Cancelable) => R) extends AsyncEmitterBuilder[E, R] {
  def transform[T](tr: Observable[E] => Observable[T]): EmitterBuilder[T, R] = new ObservableEmitterBuilder(tr(observable), create)
  def -->(observer: Observer[E]): R = create(implicit scheduler => observable.subscribe(observer))
}

final class EmptyEmitterBuilder[R] private[outwatch](empty: R) extends SyncEmitterBuilder[Nothing, R] {
  override def transformSync[T](f: Option[Nothing] => Option[T]): SyncEmitterBuilder[T, R] = this
  override def transform[T](tr: Observable[Nothing] => Observable[T]): EmitterBuilder[T, R] = this
  override def -->(observer: Observer[Nothing]): R = empty
}
