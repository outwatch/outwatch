package outwatch

import cats.{Monoid, Functor, Bifunctor}
import cats.effect.{Sync => SyncCats, SyncIO}
import org.scalajs.dom.{Element, Event, html, svg}
import colibri._
import colibri.effect._

import scala.concurrent.Future
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
// onClick.map(_ => 1).doAction(doSomething(_)): VDomModifier
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

  @inline final def -->[F[_] : Sink](sink: F[_ >: O]): R = forwardTo(sink)

  @inline final def discard: R = forwardTo(Observer.empty)

  @inline final def foreach(action: O => Unit): R = forwardTo(Observer.create(action))
  @deprecated("Use .doAction(action) instead", "")
  @inline final def foreach(action: => Unit): R = doAction(action)
  @inline final def doAction(action: => Unit): R = foreach(_ => action)

  @deprecated("Use .foreachEffect(action) instead", "")
  @inline final def foreachSync[G[_] : RunSyncEffect](action: O => G[Unit]): R = mapSync(action).discard
  @deprecated("Use .doEffect(action) instead", "")
  @inline final def doSync[G[_] : RunSyncEffect](action: G[Unit]): R = foreachSync(_ => action)

  @deprecated("Use .foreachEffect(action) instead", "")
  @inline def foreachAsync[G[_] : RunEffect](action: O => G[Unit]): R = foreachEffect(action)
  @deprecated("Use .doEffect(action) instead", "")
  @inline def doAsync[G[_] : RunEffect](action: G[Unit]): R = doEffect(action)

  @inline def foreachEffect[G[_] : RunEffect](action: O => G[Unit]): R = mapEffect(action).discard
  @inline def doEffect[G[_] : RunEffect](action: G[Unit]): R = foreachEffect(_ => action)

  @inline final def map[T](f: O => T): EmitterBuilderExecution[T, R, Exec] = transformSinkWithExec(_.contramap(f))

  @inline final def collect[T](f: PartialFunction[O, T]): EmitterBuilderExecution[T, R, Exec] = transformSinkWithExec(_.contracollect(f))

  @inline final def filter(predicate: O => Boolean): EmitterBuilderExecution[O, R, Exec] = transformSinkWithExec(_.contrafilter(predicate))

  @inline final def mapFilter[T](f: O => Option[T]): EmitterBuilderExecution[T, R, Exec] = transformSinkWithExec(_.contramapFilter(f))

  @inline final def mapIterable[T](f: O => Iterable[T]): EmitterBuilderExecution[T, R, Exec] = transformSinkWithExec(_.contramapIterable(f))

  @inline final def as[T](value: => T): EmitterBuilderExecution[T, R, Exec] = map(_ => value)
  @inline final def asDelay[T](value: T): EmitterBuilderExecution[T, R, Exec] = map(_ => value)

  @deprecated("Use .as(value) instead", "")
  @inline final def use[T](value: T): EmitterBuilderExecution[T, R, Exec] = asDelay(value)
  @deprecated("Use .asDelay(value) instead", "")
  @inline final def useLazy[T](value: => T): EmitterBuilderExecution[T, R, Exec] = as(value)

  @deprecated("Use .asDelay(value) instead", "")
  @inline final def mapTo[T](value: => T): EmitterBuilderExecution[T, R, Exec] = asDelay(value)
  @deprecated("Use .as(value) instead", "")
  @inline final def apply[T](value: T): EmitterBuilderExecution[T, R, Exec] = use(value)
  @deprecated("Use .mapFuture(f) instead", "")
  @inline final def concatMapFuture[T](f: O => Future[T]): EmitterBuilder[T, R] = mapFuture(f)
  @deprecated("Use .mapEffect(f) instead", "")
  @inline final def concatMapAsync[G[_]: RunEffect, T](f: O => G[T]): EmitterBuilder[T, R] = mapEffect(f)

  @deprecated("Use .asEffect(value) instead", "")
  @inline final def useSync[G[_]: RunSyncEffect, T](value: G[T]): EmitterBuilderExecution[T, R, Exec] = transformWithExec(_.mapEffect(_ => value))

  @deprecated("Use .asEffect(value) instead", "")
  @inline final def useAsync[G[_]: RunEffect, T](value: G[T]): EmitterBuilder[T, R] = asEffect(value)
  @inline final def asEffect[G[_]: RunEffect, T](value: G[T]): EmitterBuilder[T, R] = mapEffect(_ => value)

  @deprecated("Use .asFuture(value) instead", "")
  @inline final def useFuture[T](value: => Future[T]): EmitterBuilder[T, R] = asFuture(value)
  @inline final def asFuture[T](value: => Future[T]): EmitterBuilder[T, R] = mapFuture(_ => value)

  @deprecated("Use .asEffectSingleOrDrop(value) instead", "")
  @inline final def useAsyncSingleOrDrop[G[_]: RunEffect, T](value: G[T]): EmitterBuilder[T, R] = asEffectSingleOrDrop(value)
  @inline final def asEffectSingleOrDrop[G[_]: RunEffect, T](value: G[T]): EmitterBuilder[T, R] = mapEffectSingleOrDrop(_ => value)

  @deprecated("Use .asFutureSingleOrDrop(value) instead", "")
  @inline final def useFutureSingleOrDrop[T](value: => Future[T]): EmitterBuilder[T, R] = asFutureSingleOrDrop(value)
  @inline final def asFutureSingleOrDrop[T](value: => Future[T]): EmitterBuilder[T, R] = mapFutureSingleOrDrop(_ => value)

  @inline final def apply[G[_] : Source, T](source: G[T]): EmitterBuilderExecution[T, R, Exec] = asLatest(source)

  @deprecated("Use .asLatest(value) instead", "")
  @inline final def useLatest[F[_] : Source, T](latest: F[T]): EmitterBuilderExecution[T, R, Exec] = asLatest(latest)
  @inline final def asLatest[F[_] : Source, T](latest: F[T]): EmitterBuilderExecution[T, R, Exec] = transformWithExec[T](source => source.withLatestMap(Observable.lift(latest))((_, u) => u))

  def withLatest[F[_] : Source, T](latest: F[T]): EmitterBuilderExecution[(O, T), R, Exec] =
    transformWithExec[(O, T)](source => source.withLatest(Observable.lift(latest)))

  def scan[T](seed: T)(f: (T, O) => T): EmitterBuilderExecution[T, R, Exec] =
    transformWithExec[T](source => source.scan(seed)(f))

  @deprecated("Use .asScan(seed)(f) instead", "")
  @inline final def useScan[T](seed: T)(f: T => T): EmitterBuilderExecution[T, R, Exec] = asScan(seed)(f)
  @inline final def asScan[T](seed: T)(f: T => T): EmitterBuilderExecution[T, R, Exec] = scan(seed)((t,_) => f(t))

  def scan0[T](seed: T)(f: (T, O) => T): EmitterBuilderExecution[T, R, Exec] =
    transformWithExec[T](source => source.scan0(seed)(f))

  @deprecated("Use .asScan0(seed)(f) instead", "")
  @inline final def useScan0[T](seed: T)(f: T => T): EmitterBuilderExecution[T, R, Exec] = asScan0(seed)(f)
  @inline final def asScan0[T](seed: T)(f: T => T): EmitterBuilderExecution[T, R, Exec] = scan0(seed)((t,_) => f(t))

  def debounce(duration: FiniteDuration): EmitterBuilder[O, R] =
    transformWithExec[O](source => source.debounce(duration))

  def debounceMillis(millis: Int): EmitterBuilder[O, R] =
    transformWithExec[O](source => source.debounceMillis(millis))

  def async: EmitterBuilder[O, R] =
    transformWithExec[O](source => source.async)

  def delay(duration: FiniteDuration): EmitterBuilder[O, R] =
    transformWithExec[O](source => source.delay(duration))

  def delayMillis(millis: Int): EmitterBuilder[O, R] =
    transformWithExec[O](source => source.delayMillis(millis))

  def mapFuture[T](f: O => Future[T]): EmitterBuilder[T, R] =
    transformWithExec[T](source => source.mapFuture(f))

  @deprecated("Use .mapEffect(f) instead", "")
  def mapAsync[G[_]: RunEffect, T](f: O => G[T]): EmitterBuilder[T, R] = mapEffect(f)
  def mapEffect[G[_]: RunEffect, T](f: O => G[T]): EmitterBuilder[T, R] = transformWithExec[T](source => source.mapEffect(f))

  def mapFutureSingleOrDrop[T](f: O => Future[T]): EmitterBuilder[T, R] =
    transformWithExec[T](source => source.mapFutureSingleOrDrop(f))

  @deprecated("Use .mapEffectSingleOrDrop(f) instead", "")
  def mapAsyncSingleOrDrop[G[_]: RunEffect, T](f: O => G[T]): EmitterBuilder[T, R] = mapEffectSingleOrDrop(f)
  def mapEffectSingleOrDrop[G[_]: RunEffect, T](f: O => G[T]): EmitterBuilder[T, R] = transformWithExec[T](source => source.mapEffectSingleOrDrop(f))

  @deprecated("Use .mapEffect(f) instead", "")
  def mapSync[G[_]: RunSyncEffect, T](f: O => G[T]): EmitterBuilderExecution[T, R, Exec] = transformWithExec[T](source => source.mapEffect(f))

  def transformLifted[F[_] : Source : LiftSource, OO >: O, T](f: F[OO] => F[T]): EmitterBuilder[T, R] =
    transformWithExec[T]((s: Observable[OO]) => Observable.lift(f(LiftSource[F].lift(s))))

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

  @inline final class Stream[+O, +R: SubscriptionOwner](source: Observable[O], result: R) extends EmitterBuilderExecution[O, R, Execution] {
    @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[O]): EmitterBuilderExecution[T, R, Execution] = new Stream(source.transformSink(f), result)
    @inline private[outwatch] def transformWithExec[T](f: Observable[O] => Observable[T]): EmitterBuilderExecution[T, R, Execution] = new Stream(f(source), result)
    @inline def forwardTo[F[_] : Sink](sink: F[_ >: O]): R = SubscriptionOwner[R].own(result)(() => source.unsafeSubscribe(Observer.lift(sink)))
  }

  @inline final class Custom[+O, +R: SubscriptionOwner, + Exec <: Execution](create: Observer[O] => R) extends EmitterBuilderExecution[O, R, Exec] {
    @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[O]): EmitterBuilderExecution[T, R, Exec] = new TransformSink(this, f)
    @inline private[outwatch] def transformWithExec[T](f: Observable[O] => Observable[T]): EmitterBuilderExecution[T, R, Exec] = new Transform(this, f)
    @inline def forwardTo[F[_] : Sink](sink: F[_ >: O]): R = create(Observer.lift(sink))
  }

  @inline final class TransformSink[+I, +O, +R: SubscriptionOwner, Exec <: Execution](base: EmitterBuilderExecution[I, R, Exec], transformF: Observer[O] => Observer[I]) extends EmitterBuilderExecution[O, R, Exec] {
    @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[O]): EmitterBuilderExecution[T, R, Exec] = new TransformSink(base, s => transformF(f(s)))
    @inline private[outwatch] def transformWithExec[T](f: Observable[O] => Observable[T]): EmitterBuilderExecution[T, R, Exec] = new Transform[I, T, R, Exec](base, s => f(s.transformSink(transformF)))
    @inline def forwardTo[F[_] : Sink](sink: F[_ >: O]): R = base.forwardTo(transformF(Observer.lift(sink)))
  }

  @inline final class Transform[+I, +O, +R: SubscriptionOwner, Exec <: Execution](base: EmitterBuilderExecution[I, R, Exec], transformF: Observable[I] => Observable[O]) extends EmitterBuilderExecution[O, R, Exec] {
    @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[O]): EmitterBuilderExecution[T, R, Exec] = new Transform[I, T, R, Exec](base, s => transformF(s).transformSink(f))
    @inline private[outwatch] def transformWithExec[T](f: Observable[O] => Observable[T]): EmitterBuilderExecution[T, R, Exec] = new Transform[I, T, R, Exec](base, s => f(transformF(s)))
    @inline def forwardTo[F[_] : Sink](sink: F[_ >: O]): R = forwardToInTransform(base, transformF, sink)
  }

  //TODO: we requiring Monoid here, but actually just want an empty. Would allycats be better with Empty?
  @inline def emptyOf[R: Monoid]: EmitterBuilderExecution[Nothing, R, Nothing] = new Empty[R](Monoid[R].empty)

  @inline def apply[E, R : SubscriptionOwner](create: Observer[E] => R): EmitterBuilder.Sync[E, R] = new Custom[E, R, SyncExecution](sink => create(sink))

  @inline def fromSourceOf[F[_] : Source, E, R : SubscriptionOwner : Monoid](source: F[E]): EmitterBuilder[E, R] = new Stream[E, R](Observable.lift(source), Monoid[R].empty)

  // shortcuts for modifiers with less type ascriptions
  @inline def empty: EmitterBuilderExecution[Nothing, VDomModifier, Nothing] = emptyOf[VDomModifier]
  @inline def ofModifier[E](create: Observer[E] => VDomModifier): EmitterBuilder.Sync[E, VDomModifier] = apply[E, VDomModifier](create)
  @inline def ofNode[E](create: Observer[E] => VNode): EmitterBuilder.Sync[E, VNode] = apply[E, VNode](create)
  @inline def fromSource[F[_] : Source, E](source: F[E]): EmitterBuilder[E, VDomModifier] = fromSourceOf[F, E, VDomModifier](source)

  def fromEvent[E <: Event](eventType: String): EmitterBuilder.Sync[E, VDomModifier] = apply[E, VDomModifier] { sink =>
    Emitter(eventType, e => sink.unsafeOnNext(e.asInstanceOf[E]))
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

  implicit def functor[R]: Functor[EmitterBuilder[*, R]] = new Functor[EmitterBuilder[*, R]] {
    def map[A, B](fa: EmitterBuilder[A,R])(f: A => B): EmitterBuilder[B,R] = fa.map(f)
  }

  implicit object bifunctor extends Bifunctor[EmitterBuilder] {
    def bimap[A, B, C, D](fab: EmitterBuilder[A,B])(f: A => C, g: B => D): EmitterBuilder[C,D] = fab.map(f).mapResult(g)
  }

  @inline implicit class HandlerIntegrationMonoid[O, R : Monoid, Exec <: Execution](builder: EmitterBuilderExecution[O, R, Exec]) {
    @deprecated
    @inline def handled(f: Observable[O] => R): SyncIO[R] = handledF[SyncIO](f)

    @deprecated
    @inline def handledF[F[_] : SyncCats](f: Observable[O] => R): F[R] = Functor[F].map(SyncCats[F].delay(Subject.replayLast[O]())) { handler =>
      Monoid[R].combine(builder.forwardTo(handler), f(handler))
    }
  }

  @inline implicit class HandlerIntegration[O, R, Exec <: Execution](builder: EmitterBuilderExecution[O, R, Exec]) {
    @deprecated
    @inline def handledWith(f: (R, Observable[O]) => R): SyncIO[R] = handledWithF[SyncIO](f)

    @deprecated
    @inline def handledWithF[F[_] : SyncCats](f: (R, Observable[O]) => R): F[R] = Functor[F].map(SyncCats[F].delay(Subject.replayLast[O]())) { handler =>
      f(builder.forwardTo(handler), handler)
    }
  }

  @inline implicit class EmitterOperations[O, R : Monoid : SubscriptionOwner, Exec <: Execution](builder: EmitterBuilderExecution[O, R, Exec]) {

    @inline def withLatestEmitter[T](emitter: EmitterBuilder[T, R]): EmitterBuilderExecution[(O,T), SyncIO[R], Exec] = combineWithLatestEmitter(builder, emitter)
    @inline def asLatestEmitter[T](emitter: EmitterBuilder[T, R]): EmitterBuilderExecution[T, SyncIO[R], Exec] = withLatestEmitter(emitter).map(_._2)

    @deprecated("Use asLatestEmitter(emitter) instead", "")
    @inline def useLatestEmitter[T](emitter: EmitterBuilder[T, R]): EmitterBuilderExecution[T, SyncIO[R], Exec] = asLatestEmitter(emitter)
  }

  @inline implicit class EventActions[O <: Event, R](val builder: EmitterBuilder.Sync[O, R]) extends AnyVal {
    def onlyOwnEvents: EmitterBuilder[O, R] = builder.filter(ev => ev.currentTarget == ev.target)
    def preventDefault: EmitterBuilder.Sync[O, R] = builder.map { e => e.preventDefault(); e }
    def stopPropagation: EmitterBuilder.Sync[O, R] = builder.map { e => e.stopPropagation(); e }
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
          latestEmitter.forwardTo(Observer.create[T](lastValue = _, sink.unsafeOnError)),
          sourceEmitter.forwardTo(Observer.create[O](
            { o =>
              lastValue.foreach { t =>
                sink.unsafeOnNext((o, t))
              }
            },
            sink.unsafeOnError
          ))
        )
      }
    })

  @noinline private def forwardToInTransform[F[_] : Sink, I, O, R: SubscriptionOwner](base: EmitterBuilder[I, R], transformF: Observable[I] => Observable[O], sink: F[_ >: O]): R = {
    val connectable = Observer.lift(sink).redirect(transformF)
    SubscriptionOwner[R].own(base.forwardTo(connectable.value))(connectable.connect)
  }
}
