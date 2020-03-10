package outwatch

import cats.{Monoid, Functor, Bifunctor}
import cats.effect.{Effect, Sync => SyncCats, SyncIO}
import org.scalajs.dom.{Element, Event, html, svg}
import outwatch.reactive.handler
import colibri._
import colibri.effect._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

// The EmitterBuilder[O, R] allows you to build an R that produces values of type O.
// The builder gives you a declarative interface to describe transformations on the
// emitted values of type O and the result type R.
//
// Example onClick event:
// onClick: EmitterBuilder[ClickEvent, Emitter]

// The result emitter describes a registration of the click event on the embedding
// dom element. This produces click events that can be transformed:
// onClick.map(_ => 1): EmitterBuilder[Int, Emitter]

// We keep the same result, the registration for the click event, but map the emitted
// click events to integers. You can also map the result type:
// onClick.mapResult(emitter => VDomModifier(emitter, ???)): EmitterBuilder[Int, VDomModifier]
//
// Now you have conbined the emitter with another VDomModifier, so the combined modifier
// will later be rendered instead of only the emitter. Then you can describe the action
// that should be done when an event triggers:
//
// onClick.map(_ => 1).foreach(doSomething(_)): VDomModifier
//
// The EmitterBuilder result must be a SubscriptionOwner to handle the subscription
// from the emitterbuilder.
//


trait EmitterBuilderExecution[+O, +R, +Exec <: EmitterBuilder.Execution] {

  @inline def forwardTo[F[_] : Sink](sink: F[_ >: O]): R

  // this method keeps the current Execution but actually, the caller must decide,
  // whether this really keeps the execution type or might be async. Therefore private.
  @inline private[outwatch] def transformWithExec[T](f: Observable[O] => Observable[T]): EmitterBuilderExecution[T, R, Exec]
  @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[O]): EmitterBuilderExecution[T, R, Exec]

  @inline def -->[F[_] : Sink](sink: F[_ >: O]): R = forwardTo(sink)

  @inline def discard: R = forwardTo(Observer.empty)

  @inline def foreach(action: O => Unit): R = forwardTo(Observer.create(action))
  @inline def foreach(action: => Unit): R = foreach(_ => action)

  @inline def foreachSync[G[_] : RunSyncEffect](action: O => G[Unit]): R = mapSync(action).discard
  @inline def doSync[G[_] : RunSyncEffect](action: G[Unit]): R = foreachSync(_ => action)

  @inline def foreachAsync[G[_] : Effect](action: O => G[Unit]): R = concatMapAsync(action).discard
  @inline def doAsync[G[_] : Effect](action: G[Unit]): R = foreachAsync(_ => action)

  def map[T](f: O => T): EmitterBuilderExecution[T, R, Exec] = transformSinkWithExec(_.contramap(f))

  def collect[T](f: PartialFunction[O, T]): EmitterBuilderExecution[T, R, Exec] = transformSinkWithExec(_.contracollect(f))

  def filter(predicate: O => Boolean): EmitterBuilderExecution[O, R, Exec] = transformSinkWithExec(_.contrafilter(predicate))

  def mapFilter[T](f: O => Option[T]): EmitterBuilderExecution[T, R, Exec] = transformSinkWithExec(_.contramapFilter(f))

  @inline def use[T](value: T): EmitterBuilderExecution[T, R, Exec] = map(_ => value)
  @inline def useLazy[T](value: => T): EmitterBuilderExecution[T, R, Exec] = map(_ => value)

  @deprecated("Use .useLazy(value) instead", "")
  @inline def mapTo[T](value: => T): EmitterBuilderExecution[T, R, Exec] = useLazy(value)
  @deprecated("Use .use(value) instead", "")
  @inline def apply[T](value: T): EmitterBuilderExecution[T, R, Exec] = use(value)

  @inline def useSync[G[_]: RunSyncEffect, T](value: G[T]): EmitterBuilderExecution[T, R, Exec] = mapSync(_ => value)

  @inline def useAsync[G[_]: Effect, T](value: G[T]): EmitterBuilder[T, R] = concatMapAsync(_ => value)

  @inline def apply[G[_] : Source, T](source: G[T]): EmitterBuilderExecution[T, R, Exec] = useLatest(source)

  def useLatest[F[_] : Source, T](latest: F[T]): EmitterBuilderExecution[T, R, Exec] =
    transformWithExec[T](source => Observable.withLatestMap(source, latest)((_, u) => u))

  def withLatest[F[_] : Source, T](latest: F[T]): EmitterBuilderExecution[(O, T), R, Exec] =
    transformWithExec[(O, T)](source => Observable.withLatest(source, latest))

  def scan[T](seed: T)(f: (T, O) => T): EmitterBuilderExecution[T, R, Exec] =
    transformWithExec[T](source => Observable.scan(source)(seed)(f))

  def useScan[T](seed: T)(f: T => T): EmitterBuilderExecution[T, R, Exec] = scan(seed)((t,_) => f(t))

  def scan0[T](seed: T)(f: (T, O) => T): EmitterBuilderExecution[T, R, Exec] =
    transformWithExec[T](source => Observable.scan0(source)(seed)(f))

  def useScan0[T](seed: T)(f: T => T): EmitterBuilderExecution[T, R, Exec] = scan0(seed)((t,_) => f(t))

  def debounce(duration: FiniteDuration): EmitterBuilder[O, R] =
    transformWithExec[O](source => Observable.debounce(source)(duration))

  def debounceMillis(millis: Int): EmitterBuilder[O, R] =
    transformWithExec[O](source => Observable.debounceMillis(source)(millis))

  def async: EmitterBuilder[O, R] =
    transformWithExec[O](source => Observable.async(source))

  def delay(duration: FiniteDuration): EmitterBuilder[O, R] =
    transformWithExec[O](source => Observable.delay(source)(duration))

  def delayMillis(millis: Int): EmitterBuilder[O, R] =
    transformWithExec[O](source => Observable.delayMillis(source)(millis))

  def concatMapFuture[T](f: O => Future[T])(implicit ec: ExecutionContext): EmitterBuilder[T, R] =
    transformWithExec[T](source => Observable.concatMapFuture(source)(f))

  def concatMapAsync[G[_]: Effect, T](f: O => G[T]): EmitterBuilder[T, R] =
    transformWithExec[T](source => Observable.concatMapAsync(source)(f))

  def mapSync[G[_]: RunSyncEffect, T](f: O => G[T]): EmitterBuilderExecution[T, R, Exec] =
    transformWithExec[T](source => Observable.mapSync(source)(f))

  def transformLifted[F[_] : Source : LiftSource, OO >: O, T](f: F[OO] => F[T]): EmitterBuilder[T, R] =
    transformWithExec[T]((s: Observable[OO]) => Observable.lift(f(s.liftSource[F])))

  def transformLift[F[_] : Source, T](f: Observable[O] => F[T]): EmitterBuilder[T, R] =
    transformWithExec[T]((s: Observable[O]) => Observable.lift(f(s)))

  // do not expose transform with current exec but just normal Emitterbuilder. This tranform might be async
  @inline def transform[T](f: Observable[O] => Observable[T]): EmitterBuilder[T, R] = transformWithExec(f)
  @inline def transformSink[T](f: Observer[T] => Observer[O]): EmitterBuilder[T, R] = transformSinkWithExec(f)

  @inline def mapResult[S](f: R => S): EmitterBuilderExecution[O, S, Exec] = new EmitterBuilder.MapResult[O, R, S, Exec](this, f)
}

object EmitterBuilder {

  sealed trait Execution
  sealed trait SyncExecution extends Execution

  type Sync[+O, +R] = EmitterBuilderExecution[O, R, SyncExecution]

  @inline final class MapResult[+O, +I, +R, +Exec <: Execution](base: EmitterBuilder[O, I], mapF: I => R) extends EmitterBuilderExecution[O, R, Exec] {
    @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[O]): EmitterBuilderExecution[T, R, Exec] = new MapResult(base.transformSink(f), mapF)
    @inline private[outwatch] def transformWithExec[T](f: Observable[O] => Observable[T]): EmitterBuilderExecution[T, R, Exec] = new MapResult(base.transformWithExec(f), mapF)
    @inline def forwardTo[F[_] : Sink](sink: F[_ >: O]): R = mapF(base.forwardTo(sink))
  }

  @inline final class Empty[+R](empty: R) extends EmitterBuilderExecution[Nothing, R, Nothing] {
    @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[Nothing]): EmitterBuilderExecution[T, R, Nothing] = this
    @inline private[outwatch] def transformWithExec[T](f: Observable[Nothing] => Observable[T]): EmitterBuilderExecution[T, R, Nothing] = this
    @inline def forwardTo[F[_] : Sink](sink: F[_ >: Nothing]): R = empty
  }

  @inline final class Stream[S[_] : Source, +O, +R: SubscriptionOwner](source: S[O], result: R) extends EmitterBuilderExecution[O, R, Execution] {
    @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[O]): EmitterBuilderExecution[T, R, Execution] = new Stream(Observable.transformSink(source)(f), result)
    @inline private[outwatch] def transformWithExec[T](f: Observable[O] => Observable[T]): EmitterBuilderExecution[T, R, Execution] = new Stream(f(Observable.lift(source)), result)
    @inline def forwardTo[F[_] : Sink](sink: F[_ >: O]): R = SubscriptionOwner[R].own(result)(() => Source[S].subscribe(source)(sink))
  }

  @inline final class Custom[+O, +R: SubscriptionOwner, + Exec <: Execution](create: Observer[O] => R) extends EmitterBuilderExecution[O, R, Exec] {
    @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[O]): EmitterBuilderExecution[T, R, Exec] = new TransformSink(this, f)
    @inline private[outwatch] def transformWithExec[T](f: Observable[O] => Observable[T]): EmitterBuilderExecution[T, R, Exec] = new Transform(this, f)
    @inline def forwardTo[F[_] : Sink](sink: F[_ >: O]): R = create(Observer.lift(sink))
  }

  @inline final class TransformSink[+I, +O, +R: SubscriptionOwner, Exec <: Execution](base: EmitterBuilderExecution[I, R, Exec], transformF: Observer[O] => Observer[I]) extends EmitterBuilderExecution[O, R, Exec] {
    @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[O]): EmitterBuilderExecution[T, R, Exec] = new TransformSink(base, s => transformF(f(s)))
    @inline private[outwatch] def transformWithExec[T](f: Observable[O] => Observable[T]): EmitterBuilderExecution[T, R, Exec] = new Transform[I, T, R, Exec](base, s => f(Observable.transformSink(s)(transformF)))
    @inline def forwardTo[F[_] : Sink](sink: F[_ >: O]): R = base.forwardTo(transformF(Observer.lift(sink)))
  }

  @inline final class Transform[+I, +O, +R: SubscriptionOwner, Exec <: Execution](base: EmitterBuilderExecution[I, R, Exec], transformF: Observable[I] => Observable[O]) extends EmitterBuilderExecution[O, R, Exec] {
    @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[O]): EmitterBuilderExecution[T, R, Exec] = new Transform[I, T, R, Exec](base, s => Observable.transformSink(transformF(s))(f))
    @inline private[outwatch] def transformWithExec[T](f: Observable[O] => Observable[T]): EmitterBuilderExecution[T, R, Exec] = new Transform[I, T, R, Exec](base, s => f(transformF(s)))
    @inline def forwardTo[F[_] : Sink](sink: F[_ >: O]): R = forwardToInTransform(base, transformF, sink)
  }

  //TODO: we requiring Monoid here, but actually just want an empty. Would allycats be better with Empty?
  @inline def emptyOf[R: Monoid]: EmitterBuilderExecution[Nothing, R, Nothing] = new Empty[R](Monoid[R].empty)

  @inline def apply[E, R : SubscriptionOwner](create: Observer[E] => R): EmitterBuilder.Sync[E, R] = new Custom[E, R, SyncExecution](sink => create(sink))

  @inline def fromSourceOf[F[_] : Source, E, R : SubscriptionOwner : Monoid](source: F[E]): EmitterBuilder[E, R] = new Stream[F, E, R](source, Monoid[R].empty)

  // shortcuts for modifiers with less type ascriptions
  @inline def empty: EmitterBuilderExecution[Nothing, VDomModifier, Nothing] = emptyOf[VDomModifier]
  @inline def ofModifier[E](create: Observer[E] => VDomModifier): EmitterBuilder.Sync[E, VDomModifier] = apply[E, VDomModifier](create)
  @inline def ofNode[E](create: Observer[E] => VNode): EmitterBuilder.Sync[E, VNode] = apply[E, VNode](create)
  @inline def fromSource[F[_] : Source, E](source: F[E]): EmitterBuilder[E, VDomModifier] = fromSourceOf[F, E, VDomModifier](source)

  def fromEvent[E <: Event](eventType: String): EmitterBuilder.Sync[E, VDomModifier] = apply[E, VDomModifier] { sink =>
    Emitter(eventType, e => sink.onNext(e.asInstanceOf[E]))
  }

  @inline def combine[T, R : SubscriptionOwner : Monoid, Exec <: Execution](builders: EmitterBuilderExecution[T, R, Exec]*): EmitterBuilderExecution[T, R, Exec] = combineSeq(builders)

  def combineSeq[T, R : SubscriptionOwner : Monoid, Exec <: Execution](builders: Seq[EmitterBuilderExecution[T, R, Exec]]): EmitterBuilderExecution[T, R, Exec] = new Custom[T, R, Exec](sink =>
    Monoid[R].combineAll(builders.map(_.forwardTo(sink)))
  )

  @deprecated("Use EmitterBuilder.fromEvent[E] instead", "0.11.0")
  @inline def apply[E <: Event](eventType: String): EmitterBuilder.Sync[E, VDomModifier] = fromEvent[E](eventType)
  @deprecated("Use EmitterBuilder[E, O] instead", "0.11.0")
  @inline def custom[E, R : SubscriptionOwner](create: Observer[E] => R): EmitterBuilder.Sync[E, R] = apply[E, R](create)

  implicit def monoid[T, R : SubscriptionOwner : Monoid, Exec <: Execution]: Monoid[EmitterBuilderExecution[T, R, Exec]] = new Monoid[EmitterBuilderExecution[T, R, Exec]] {
    def empty: EmitterBuilderExecution[T, R, Exec] = EmitterBuilder.emptyOf[R]
    def combine(x: EmitterBuilderExecution[T, R, Exec], y: EmitterBuilderExecution[T, R, Exec]): EmitterBuilderExecution[T, R, Exec] = EmitterBuilder.combine(x, y)
  }

  implicit def functor[R]: Functor[EmitterBuilder[?, R]] = new Functor[EmitterBuilder[?, R]] {
    def map[A, B](fa: EmitterBuilder[A,R])(f: A => B): EmitterBuilder[B,R] = fa.map(f)
  }

  implicit object bifunctor extends Bifunctor[EmitterBuilder] {
    def bimap[A, B, C, D](fab: EmitterBuilder[A,B])(f: A => C, g: B => D): EmitterBuilder[C,D] = fab.map(f).mapResult(g)
  }

  @inline implicit class HandlerIntegrationMonoid[O, R : Monoid, Exec <: Execution](builder: EmitterBuilderExecution[O, R, Exec]) {
    @inline def handled(f: Observable[O] => R): SyncIO[R] = handledF[SyncIO](f)

    @inline def handledF[F[_] : SyncCats](f: Observable[O] => R): F[R] = Functor[F].map(handler.Handler.createF[F, O]) { handler =>
      Monoid[R].combine(builder.forwardTo(handler), f(handler))
    }
  }

  @inline implicit class HandlerIntegration[O, R, Exec <: Execution](builder: EmitterBuilderExecution[O, R, Exec]) {
    @inline def handledWith(f: (R, Observable[O]) => R): SyncIO[R] = handledWithF[SyncIO](f)

    @inline def handledWithF[F[_] : SyncCats](f: (R, Observable[O]) => R): F[R] = Functor[F].map(handler.Handler.createF[F, O]) { handler =>
      f(builder.forwardTo(handler), handler)
    }
  }

  @inline implicit class EmitterOperations[O, R : Monoid : SubscriptionOwner, Exec <: Execution](builder: EmitterBuilderExecution[O, R, Exec]) {

    @inline def withLatestEmitter[T](emitter: EmitterBuilder[T, R]): EmitterBuilderExecution[(O,T), SyncIO[R], Exec] = combineWithLatestEmitter(builder, emitter)

    @inline def useLatestEmitter[T](emitter: EmitterBuilder[T, R]): EmitterBuilderExecution[T, SyncIO[R], Exec] = combineWithLatestEmitter(builder, emitter).map(_._2)
  }

  @inline implicit class EventActions[O <: Event, R](val builder: EmitterBuilder.Sync[O, R]) extends AnyVal {
    def preventDefault: EmitterBuilder.Sync[O, R] = builder.map { e => e.preventDefault; e }
    def stopPropagation: EmitterBuilder.Sync[O, R] = builder.map { e => e.stopPropagation; e }
    def stopImmediatePropagation: EmitterBuilder.Sync[O, R] = builder.map { e => e.stopImmediatePropagation; e }
  }

  @inline implicit class TargetAsInput[O <: Event, R](builder: EmitterBuilder.Sync[O, R]) {
    object target {
      @inline def value: EmitterBuilder.Sync[String, R] = builder.map(_.target.asInstanceOf[html.Input].value)
      @inline def valueAsNumber: EmitterBuilder.Sync[Double, R] = builder.map(_.target.asInstanceOf[html.Input].valueAsNumber)
      @inline def checked: EmitterBuilder.Sync[Boolean, R] = builder.map(_.target.asInstanceOf[html.Input].checked)
    }
  }

  @inline implicit class CurrentTargetAsInput[O <: Event, R](val builder: EmitterBuilder.Sync[O, R]) extends AnyVal {
    def value: EmitterBuilder.Sync[String, R] = builder.map(e => e.currentTarget.asInstanceOf[html.Input].value)
    def valueAsNumber: EmitterBuilder.Sync[Double, R] = builder.map(e => e.currentTarget.asInstanceOf[html.Input].valueAsNumber)
    def checked: EmitterBuilder.Sync[Boolean, R] = builder.map(e => e.currentTarget.asInstanceOf[html.Input].checked)
  }

  @inline implicit class CurrentTargetAsElement[O <: Event, R](val builder: EmitterBuilder.Sync[O, R]) extends AnyVal {
    def asHtml: EmitterBuilder.Sync[html.Element, R] = builder.map(_.currentTarget.asInstanceOf[html.Element])
    def asSvg: EmitterBuilder.Sync[svg.Element, R] = builder.map(_.currentTarget.asInstanceOf[svg.Element])
    def asElement: EmitterBuilder.Sync[Element, R] = builder.map(_.currentTarget.asInstanceOf[Element])
  }

  @inline implicit class TypedElements[O <: Element, R](val builder: EmitterBuilder.Sync[O, R]) extends AnyVal {
    @inline def asHtml: EmitterBuilder.Sync[html.Element, R] = builder.asInstanceOf[EmitterBuilder.Sync[html.Element, R]]
    @inline def asSvg: EmitterBuilder.Sync[svg.Element, R] = builder.asInstanceOf[EmitterBuilder.Sync[svg.Element, R]]
  }

  @inline implicit class TypedElementTuples[E <: Element, R](val builder: EmitterBuilder.Sync[(E,E), R]) extends AnyVal {
    @inline def asHtml: EmitterBuilder.Sync[(html.Element, html.Element), R] = builder.asInstanceOf[EmitterBuilder.Sync[(html.Element, html.Element), R]]
    @inline def asSvg: EmitterBuilder.Sync[(svg.Element, svg.Element), R] = builder.asInstanceOf[EmitterBuilder.Sync[(svg.Element, svg.Element), R]]
  }

  @noinline private def combineWithLatestEmitter[O, T, R : Monoid : SubscriptionOwner, Exec <: Execution](sourceEmitter: EmitterBuilderExecution[O, R, Exec], latestEmitter: EmitterBuilder[T, R]): EmitterBuilderExecution[(O, T), SyncIO[R], Exec] =
    new Custom[(O, T), SyncIO[R], Exec]({ sink =>
      import scala.scalajs.js

      SyncIO {
        var lastValue: js.UndefOr[T] = js.undefined
        Monoid[R].combine(
          latestEmitter.forwardTo(Observer.create[T](lastValue = _, sink.onError)),
          sourceEmitter.forwardTo(Observer.create[O](
            { o =>
              lastValue.foreach { t =>
                sink.onNext((o, t))
              }
            },
            sink.onError
          ))
        )
      }
    })

  @noinline private def forwardToInTransform[F[_] : Sink, I, O, R: SubscriptionOwner](base: EmitterBuilder[I, R], transformF: Observable[I] => Observable[O], sink: F[_ >: O]): R = {
    val connectable = Observer.redirect[F, Observable, O, I](sink)(transformF)
    SubscriptionOwner[R].own(base.forwardTo(connectable.sink))(() => connectable.connect())
  }
}
