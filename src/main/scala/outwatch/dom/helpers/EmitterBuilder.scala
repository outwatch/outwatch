package outwatch.dom.helpers

import monix.execution.Scheduler
import monix.reactive.subjects.PublishSubject
import monix.reactive.{Observable, Observer, OverflowStrategy}
import org.scalajs.dom.{Element, Event, html, svg}
import outwatch.ProHandler
import outwatch.dom._

import scala.concurrent.duration.FiniteDuration


trait EmitterBuilder[+O, +R] extends Any {

  def transform[T](tr: Observable[O] => Observable[T]): EmitterBuilder[T, R]

  def -->(observer: Observer[O]): R = handleWith(observer)
  def handleWith(observer: Observer[O]): R
  def handleWith(action: O => Unit): R
  def handleWith(action: => Unit): R = handleWith(_ => action)

  def apply[T](value: T): EmitterBuilder[T, R] = map(_ => value)

  def mapTo[T](value: => T): EmitterBuilder[T, R] = map(_ => value)

  def apply[T](latest: Observable[T]): EmitterBuilder[T, R] = transform(_.withLatestFrom(latest)((_, u) => u))

  def debounce(timeout: FiniteDuration): EmitterBuilder[O, R] = transform(_.debounce(timeout))

  def async: EmitterBuilder[O, R] = transform(_.asyncBoundary(OverflowStrategy.Unbounded))

  @deprecated("Deprecated, use '.map' instead", "0.11.0")
  def apply[T](f: O => T): EmitterBuilder[T, R] = map(f)

  def map[T](f: O => T): EmitterBuilder[T, R]

  def filter(predicate: O => Boolean): EmitterBuilder[O, R]

  def collect[T](f: PartialFunction[O, T]): EmitterBuilder[T, R]
}

object EmitterBuilder {
  def apply[E <: Event](eventType: String): CustomEmitterBuilder[E, VDomModifier] = ofModifier[E](f => Emitter(eventType, event => f(event.asInstanceOf[E])))

  def withScheduler[E](create: (E => Unit) => VDomModifier)(implicit scheduler: Scheduler): CustomEmitterBuilder[E, VDomModifier] =
    CustomEmitterBuilder[E, VDomModifier] { f => SchedulerAction(s => create(f(s))) }

  def ofModifier[E](create: (E => Unit) => VDomModifier): CustomEmitterBuilder[E, VDomModifier] =
    CustomEmitterBuilder[E, VDomModifier] { f => SchedulerAction(s => create(f(s))) }

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

  implicit class TupleActions[A,B, R](val builder: EmitterBuilder[(A,B), R]) {
    def handleWith2(f: (A,B) => Unit): R = builder handleWith ({ case (a,b) => f(a,b) }: ((A, B)) => Unit)
  }
}

final case class TransformingEmitterBuilder[E, +O, +R](
  handler: ProHandler[E, O],
  create: (Scheduler => (E => Unit)) => R
) extends EmitterBuilder[O, R] {

  def transform[T](tr: Observable[O] => Observable[T]): EmitterBuilder[T, R] = copy(handler = handler.transformObservable(tr))

  def map[T](f: O => T): EmitterBuilder[T, R] = transform(_.map(f))
  def filter(predicate: O => Boolean): EmitterBuilder[O, R] = transform(_.filter(predicate))
  def collect[T](f: PartialFunction[O, T]): EmitterBuilder[T, R] = transform(_.collect(f))

  def handleWith(action: O => Unit): R = -->(Sink.fromFunction(action))
  def handleWith(observer: Observer[O]): R = create { implicit s =>
    val cancelable = handler.subscribe(observer)

    (e: E) => handler.onNext(e)
  }
}

trait SyncEmitterBuilder[+O, +R] extends Any with EmitterBuilder[O, R] {
  def transformSync[T](f: Option[O] => Option[T]): SyncEmitterBuilder[T, R]
  def map[T](f: O => T): SyncEmitterBuilder[T, R] = transformSync(_.map(f))
  def collect[T](f: PartialFunction[O, T]): SyncEmitterBuilder[T, R] = transformSync(_.collect(f))
  def filter(predicate: O => Boolean): SyncEmitterBuilder[O, R] = transformSync(_.filter(predicate))
}

final case class FunctionEmitterBuilder[E, +O, +R](
 transformer: Option[E] => Option[O],
 create: (Scheduler => (E => Unit)) => R
) extends SyncEmitterBuilder[O, R] {

  def transform[T](tr: Observable[O] => Observable[T]): EmitterBuilder[T, R] = TransformingEmitterBuilder[O, T, R](PublishSubject[O].transformObservable(tr), f => create { s => val g = f(s); (e: E) => transformer(Some(e)).foreach(g) })
  def transformSync[T](tr: Option[O] => Option[T]): SyncEmitterBuilder[T, R] = copy(transformer = (e: Option[E]) => tr(transformer(e)))

  def handleWith(action: Observer[O]): R = handleWith(action.onNext(_))
  def handleWith(action: O => Unit): R = create { _ =>
    (e: E) => transformer(Some(e)).foreach(action)
  }
}

final case class CustomEmitterBuilder[+E, +R](create: (Scheduler => (E => Unit)) => R) extends AnyVal with SyncEmitterBuilder[E, R] {
  def transform[T](tr: Observable[E] => Observable[T]): EmitterBuilder[T, R] = TransformingEmitterBuilder[E, T, R](PublishSubject[E].transformObservable(tr), create)
  def transformSync[T](tr: Option[E] => Option[T]): SyncEmitterBuilder[T, R] = FunctionEmitterBuilder[E, T, R](tr, create)

  def handleWith(observer: Observer[E]): R = handleWith(observer.onNext(_))
  def handleWith(action: E => Unit): R = create(_ => action)
}
