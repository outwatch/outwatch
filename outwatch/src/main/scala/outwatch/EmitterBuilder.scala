package outwatch

import cats.{Bifunctor, Functor, Monoid}
import cats.effect.{Sync => SyncCats, SyncIO}
import colibri._
import colibri.effect._

import org.scalajs.dom

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
// onClick.mapResult(emitter => VModifier(emitter, ???)): EmitterBuilder[Int, VModifier]
//
// Now you have conbined the emitter with another VModifier, so the combined modifier
// will later be rendered instead of only the emitter. Then you can describe the action
// that should be done when an event triggers:
//
// onClick.map(_ => 1).doAction(doSomething(_)): VModifier
//
// The EmitterBuilder result must be a SubscriptionOwner to handle the subscription
// from the emitterbuilder.
//

trait EmitterBuilderExecution[+O, +R, +Exec <: EmitterBuilder.Execution] {

  @inline def forwardTo[F[_]: Sink, O2 >: O](sink: F[O2]): R

  // this method keeps the current Execution but actually, the caller must decide,
  // whether this really keeps the execution type or might be async. Therefore private.
  @inline private[outwatch] def transformWithExec[T](
    f: Observable[O] => Observable[T],
  ): EmitterBuilderExecution[T, R, Exec]
  @inline private[outwatch] def transformSinkWithExec[T](
    f: Observer[T] => Observer[O],
  ): EmitterBuilderExecution[T, R, Exec]

  @inline final def -->(sink: Observer[O]): R                                                     = forwardTo(sink)
  @inline final def -->[F[_]: Sink, O2 >: O](sink: F[O2], @annotation.nowarn dummy: Unit = ()): R = forwardTo(sink)

  @inline final def discard: R = forwardTo(Observer.empty)

  @inline final def foreach(action: O => Unit): R = forwardTo(Observer.create(action))
  @deprecated("Use .doAction(action) instead", "")
  @inline final def foreach(action: => Unit): R  = doAction(action)
  @inline final def doAction(action: => Unit): R = foreach(_ => action)

  @deprecated("Use .foreachEffect(action) instead", "")
  @inline final def foreachSync[G[_]: RunSyncEffect](action: O => G[Unit]): R = mapSync(action).discard
  @deprecated("Use .doEffect(action) instead", "")
  @inline final def doSync[G[_]: RunSyncEffect](action: G[Unit]): R = foreachSync(_ => action)

  @deprecated("Use .foreachEffect(action) instead", "")
  @inline def foreachAsync[G[_]: RunEffect](action: O => G[Unit]): R = foreachEffect(action)
  @deprecated("Use .doEffect(action) instead", "")
  @inline def doAsync[G[_]: RunEffect](action: G[Unit]): R = doEffect(action)

  @inline def foreachEffect[G[_]: RunEffect](action: O => G[Unit]): R = mapEffect(action).discard
  @inline def doEffect[G[_]: RunEffect](action: G[Unit]): R           = foreachEffect(_ => action)

  @inline def foreachFuture(action: O => Future[Unit]): R = mapFuture(action).discard
  @inline def doFuture(action: Future[Unit]): R           = foreachFuture(_ => action)

  @deprecated("Use .foreachEffectSingleOrDrop(action) instead", "")
  @inline def foreachAsyncSingleOrDrop[G[_]: RunEffect](action: O => G[Unit]): R = foreachEffectSingleOrDrop(action)
  @deprecated("Use .doEffectSingleOrDrop(action) instead", "")
  @inline def doAsyncSingleOrDrop[G[_]: RunEffect](action: G[Unit]): R = doEffectSingleOrDrop(action)

  @inline def foreachEffectSingleOrDrop[G[_]: RunEffect](action: O => G[Unit]): R =
    mapEffectSingleOrDrop(action).discard
  @inline def doEffectSingleOrDrop[G[_]: RunEffect](action: G[Unit]): R =
    foreachEffectSingleOrDrop(_ => action)

  @inline def via[F[_]: Sink, O2 >: O](sink: F[O2]): EmitterBuilderExecution[O, R, Exec] =
    transformSinkWithExec[O](Observer.combine(_, Observer.lift(sink)))
  @inline def dispatchWith(dispatcher: EventDispatcher[O]): R = transform(dispatcher.dispatch).discard

  @inline final def map[T](f: O => T): EmitterBuilderExecution[T, R, Exec] = transformSinkWithExec(_.contramap(f))

  @inline final def collect[T](f: PartialFunction[O, T]): EmitterBuilderExecution[T, R, Exec] = transformSinkWithExec(
    _.contracollect(f),
  )

  @inline final def filter(predicate: O => Boolean): EmitterBuilderExecution[O, R, Exec] = transformSinkWithExec(
    _.contrafilter(predicate),
  )

  @inline final def mapFilter[T](f: O => Option[T]): EmitterBuilderExecution[T, R, Exec] = transformSinkWithExec(
    _.contramapFilter(f),
  )

  @inline final def mapIterable[T](f: O => Iterable[T]): EmitterBuilderExecution[T, R, Exec] = transformSinkWithExec(
    _.contramapIterable(f),
  )

  @inline final def as[T](value: T): EmitterBuilderExecution[T, R, Exec]        = map(_ => value)
  @inline final def asEval[T](value: => T): EmitterBuilderExecution[T, R, Exec] = map(_ => value)

  @deprecated("Use .as(value) instead", "")
  @inline final def use[T](value: T): EmitterBuilderExecution[T, R, Exec] = as(value)
  @deprecated("Use .asEval(value) instead", "")
  @inline final def useLazy[T](value: => T): EmitterBuilderExecution[T, R, Exec] = asEval(value)

  @deprecated("Use .asEval(value) instead", "")
  @inline final def mapTo[T](value: => T): EmitterBuilderExecution[T, R, Exec] = asEval(value)
  @deprecated("Use .as(value) instead", "")
  @inline final def apply[T](value: T): EmitterBuilderExecution[T, R, Exec] = use(value)
  @deprecated("Use .mapFuture(f) instead", "")
  @inline final def concatMapFuture[T](f: O => Future[T]): EmitterBuilder[T, R] = mapFuture(f)
  @deprecated("Use .mapEffect(f) instead", "")
  @inline final def concatMapAsync[G[_]: RunEffect, T](f: O => G[T]): EmitterBuilder[T, R] = mapEffect(f)

  @deprecated("Use .asEffect(value) instead", "")
  @inline final def useSync[G[_]: RunSyncEffect, T](value: G[T]): EmitterBuilderExecution[T, R, Exec] =
    transformWithExec(_.mapEffect(_ => value))

  @deprecated("Use .asEffect(value) instead", "")
  @inline final def useAsync[G[_]: RunEffect, T](value: G[T]): EmitterBuilder[T, R] = asEffect(value)
  @inline final def asEffect[G[_]: RunEffect, T](value: G[T]): EmitterBuilder[T, R] = mapEffect(_ => value)
  @inline final def withEffect[G[_]: RunEffect: Functor, T](value: G[T]): EmitterBuilder[(O, T), R] =
    mapEffect(x => Functor[G].map(value)(x -> _))

  @deprecated("Use .asFuture(value) instead", "")
  @inline final def useFuture[T](value: => Future[T]): EmitterBuilder[T, R] = asFuture(value)
  @inline final def asFuture[T](value: => Future[T]): EmitterBuilder[T, R]  = mapFuture(_ => value)
  @inline final def withFuture[T](value: => Future[T]): EmitterBuilder[(O, T), R] =
    mapFuture(x => value.map(x -> _)(ExecutionContext.parasitic))

  @deprecated("Use .asEffectSingleOrDrop(value) instead", "")
  @inline final def useAsyncSingleOrDrop[G[_]: RunEffect, T](value: G[T]): EmitterBuilder[T, R] = asEffectSingleOrDrop(
    value,
  )
  @inline final def asEffectSingleOrDrop[G[_]: RunEffect, T](value: G[T]): EmitterBuilder[T, R] =
    mapEffectSingleOrDrop(_ => value)
  @inline final def withEffectSingleOrDrop[G[_]: RunEffect: Functor, T](value: G[T]): EmitterBuilder[(O, T), R] =
    mapEffectSingleOrDrop(x => Functor[G].map(value)(x -> _))

  @deprecated("Use .asFutureSingleOrDrop(value) instead", "")
  @inline final def useFutureSingleOrDrop[T](value: => Future[T]): EmitterBuilder[T, R] = asFutureSingleOrDrop(value)
  @inline final def asFutureSingleOrDrop[T](value: => Future[T]): EmitterBuilder[T, R] =
    mapFutureSingleOrDrop(_ => value)

  @inline final def apply[G[_]: Source, T](source: G[T]): EmitterBuilderExecution[T, R, Exec] = asLatest(source)

  @deprecated("Use .asLatest(value) instead", "")
  @inline final def useLatest[F[_]: Source, T](latest: F[T]): EmitterBuilderExecution[T, R, Exec] = asLatest(latest)
  @inline final def asLatest[F[_]: Source, T](latest: F[T]): EmitterBuilderExecution[T, R, Exec] =
    transformWithExec[T](source => source.withLatestMap(Observable.lift(latest))((_, u) => u))

  def withLatest[F[_]: Source, T](latest: F[T]): EmitterBuilderExecution[(O, T), R, Exec] =
    transformWithExec[(O, T)](source => source.withLatest(Observable.lift(latest)))

  @inline final def asHead[F[_]: Source, T](source: F[T]): EmitterBuilder[T, R] = asEffect(
    Observable.lift(source).headIO,
  )
  @inline final def withHead[F[_]: Source, T](source: F[T]): EmitterBuilder[(O, T), R] = withEffect(
    Observable.lift(source).headIO,
  )

  @inline final def asSyncLatest[F[_]: Source, T](source: F[T]): EmitterBuilder[T, R] =
    asEffect(Observable.lift(source).syncLatestSyncIO).mapFilter(identity)
  @inline final def withSyncLatest[F[_]: Source, T](source: F[T]): EmitterBuilder[(O, T), R] =
    withEffect(Observable.lift(source).syncLatestSyncIO).mapFilter { case (o, t) => t.map(o -> _) }

  def scan[T](seed: T)(f: (T, O) => T): EmitterBuilderExecution[T, R, Exec] =
    transformWithExec[T](source => source.scan(seed)(f))

  @deprecated("Use .asScan(seed)(f) instead", "")
  @inline final def useScan[T](seed: T)(f: T => T): EmitterBuilderExecution[T, R, Exec] = asScan(seed)(f)
  @inline final def asScan[T](seed: T)(f: T => T): EmitterBuilderExecution[T, R, Exec]  = scan(seed)((t, _) => f(t))

  def scan0[T](seed: T)(f: (T, O) => T): EmitterBuilderExecution[T, R, Exec] =
    transformWithExec[T](source => source.scan0(seed)(f))

  @deprecated("Use .asScan0(seed)(f) instead", "")
  @inline final def useScan0[T](seed: T)(f: T => T): EmitterBuilderExecution[T, R, Exec] = asScan0(seed)(f)
  @inline final def asScan0[T](seed: T)(f: T => T): EmitterBuilderExecution[T, R, Exec]  = scan0(seed)((t, _) => f(t))

  def debounce(duration: FiniteDuration): EmitterBuilder[O, R] =
    transformWithExec[O](source => source.debounce(duration))

  def debounceMillis(millis: Int): EmitterBuilder[O, R] =
    transformWithExec[O](source => source.debounceMillis(millis))

  def async: EmitterBuilder[O, R] = asyncMacro

  def asyncMacro: EmitterBuilder[O, R] =
    transformWithExec[O](source => source.asyncMacro)

  def asyncMicro: EmitterBuilder[O, R] =
    transformWithExec[O](source => source.asyncMicro)

  def delay(duration: FiniteDuration): EmitterBuilder[O, R] =
    transformWithExec[O](source => source.delay(duration))

  def delayMillis(millis: Int): EmitterBuilder[O, R] =
    transformWithExec[O](source => source.delayMillis(millis))

  @deprecated("Use .mapEffect(f) instead", "")
  def mapAsync[G[_]: RunEffect, T](f: O => G[T]): EmitterBuilder[T, R] = mapEffect(f)
  def mapEffect[G[_]: RunEffect, T](f: O => G[T]): EmitterBuilder[T, R] =
    transformWithExec[T](source => source.mapEffect(f))

  def mapFuture[T](f: O => Future[T]): EmitterBuilder[T, R] = transformWithExec[T](source => source.mapFuture(f))

  def mapFutureSingleOrDrop[T](f: O => Future[T]): EmitterBuilder[T, R] =
    transformWithExec[T](source => source.mapFutureSingleOrDrop(f))

  @deprecated("Use .mapEffectSingleOrDrop(f) instead", "")
  def mapAsyncSingleOrDrop[G[_]: RunEffect, T](f: O => G[T]): EmitterBuilder[T, R] = mapEffectSingleOrDrop(f)
  def mapEffectSingleOrDrop[G[_]: RunEffect, T](f: O => G[T]): EmitterBuilder[T, R] =
    transformWithExec[T](source => source.mapEffectSingleOrDrop(f))

  @deprecated("Use .mapEffect(f) instead", "")
  def mapSync[G[_]: RunSyncEffect, T](f: O => G[T]): EmitterBuilderExecution[T, R, Exec] =
    transformWithExec[T](source => source.mapEffect(f))

  @deprecated("Use transform instead", "1.0.0")
  def transformLifted[F[_]: Source: LiftSource, OO >: O, T](f: F[OO] => F[T]): EmitterBuilder[T, R] =
    transformWithExec[T]((s: Observable[OO]) => Observable.lift(f(LiftSource[F].lift(s))))

  @deprecated("Use transform instead", "1.0.0")
  def transformLift[F[_]: Source, T](f: Observable[O] => F[T]): EmitterBuilder[T, R] =
    transformWithExec[T]((s: Observable[O]) => Observable.lift(f(s)))

  // do not expose transform with current exec but just normal Emitterbuilder. This tranform might be async
  @inline def transform[T](f: Observable[O] => Observable[T]): EmitterBuilder[T, R] = transformWithExec(f)
  @inline def transformSink[T](f: Observer[T] => Observer[O]): EmitterBuilder[T, R] = transformSinkWithExec(f)

  @inline def mapResult[S](f: R => S): EmitterBuilderExecution[O, S, Exec] =
    new EmitterBuilder.MapResult[O, R, S, Exec](this, f)
}

object EmitterBuilder {

  sealed trait Execution
  sealed trait SyncExecution extends Execution

  type Sync[+O, +R] = EmitterBuilderExecution[O, R, SyncExecution]

  @inline final class MapResult[+O, +I, +R, +Exec <: Execution](base: EmitterBuilder[O, I], mapF: I => R)
      extends EmitterBuilderExecution[O, R, Exec] {
    @inline private[outwatch] def transformSinkWithExec[T](
      f: Observer[T] => Observer[O],
    ): EmitterBuilderExecution[T, R, Exec] = new MapResult(base.transformSink(f), mapF)
    @inline private[outwatch] def transformWithExec[T](
      f: Observable[O] => Observable[T],
    ): EmitterBuilderExecution[T, R, Exec] = new MapResult(base.transformWithExec(f), mapF)
    @inline def forwardTo[F[_]: Sink, O2 >: O](sink: F[O2]): R = mapF(base.forwardTo(sink))
  }

  @inline final class Empty[+R](empty: R) extends EmitterBuilderExecution[Nothing, R, Nothing] {
    @inline private[outwatch] def transformSinkWithExec[T](
      f: Observer[T] => Observer[Nothing],
    ): EmitterBuilderExecution[T, R, Nothing] = this
    @inline private[outwatch] def transformWithExec[T](
      f: Observable[Nothing] => Observable[T],
    ): EmitterBuilderExecution[T, R, Nothing] = this
    @inline def forwardTo[F[_]: Sink, O2 >: Nothing](sink: F[O2]): R = empty
  }

  @inline final class Stream[+O, +R: SubscriptionOwner: SyncEmbed](source: Observable[O], result: R)
      extends EmitterBuilderExecution[O, R, Execution] {
    @inline private[outwatch] def transformSinkWithExec[T](
      f: Observer[T] => Observer[O],
    ): EmitterBuilderExecution[T, R, Execution] = new Stream(source.transformSink(f), result)
    @inline private[outwatch] def transformWithExec[T](
      f: Observable[O] => Observable[T],
    ): EmitterBuilderExecution[T, R, Execution] = new Stream(f(source), result)
    @inline def forwardTo[F[_]: Sink, O2 >: O](sink: F[O2]): R =
      SubscriptionOwner[R].own(result)(() => source.unsafeSubscribe(Observer.lift(sink)))
  }

  @inline final class Custom[+O, +R: SubscriptionOwner: SyncEmbed, +Exec <: Execution](create: Observer[O] => R)
      extends EmitterBuilderExecution[O, R, Exec] {
    @inline private[outwatch] def transformSinkWithExec[T](
      f: Observer[T] => Observer[O],
    ): EmitterBuilderExecution[T, R, Exec] = new TransformSink(this, f)
    @inline private[outwatch] def transformWithExec[T](
      f: Observable[O] => Observable[T],
    ): EmitterBuilderExecution[T, R, Exec] = new Transform(this, f)
    @inline def forwardTo[F[_]: Sink, O2 >: O](sink: F[O2]): R = create(Observer.lift(sink))
  }

  @inline final class TransformSink[+I, +O, +R: SubscriptionOwner: SyncEmbed, Exec <: Execution](
    base: EmitterBuilderExecution[I, R, Exec],
    transformF: Observer[O] => Observer[I],
  ) extends EmitterBuilderExecution[O, R, Exec] {
    @inline private[outwatch] def transformSinkWithExec[T](
      f: Observer[T] => Observer[O],
    ): EmitterBuilderExecution[T, R, Exec] = new TransformSink(base, s => transformF(f(s)))
    @inline private[outwatch] def transformWithExec[T](
      f: Observable[O] => Observable[T],
    ): EmitterBuilderExecution[T, R, Exec] = new Transform[I, T, R, Exec](base, s => f(s.transformSink(transformF)))
    @inline def forwardTo[F[_]: Sink, O2 >: O](sink: F[O2]): R = base.forwardTo(transformF(Observer.lift(sink)))
  }

  @inline final class Transform[+I, +O, +R: SubscriptionOwner: SyncEmbed, Exec <: Execution](
    base: EmitterBuilderExecution[I, R, Exec],
    transformF: Observable[I] => Observable[O],
  ) extends EmitterBuilderExecution[O, R, Exec] {
    @inline private[outwatch] def transformSinkWithExec[T](
      f: Observer[T] => Observer[O],
    ): EmitterBuilderExecution[T, R, Exec] = new Transform[I, T, R, Exec](base, s => transformF(s).transformSink(f))
    @inline private[outwatch] def transformWithExec[T](
      f: Observable[O] => Observable[T],
    ): EmitterBuilderExecution[T, R, Exec] = new Transform[I, T, R, Exec](base, s => f(transformF(s)))
    @inline def forwardTo[F[_]: Sink, O2 >: O](sink: F[O2]): R = forwardToInTransform(base, transformF, sink)
  }

  // TODO: we requiring Monoid here, but actually just want an empty. Would allycats be better with Empty?
  @inline def emptyOf[R: Monoid]: EmitterBuilderExecution[Nothing, R, Nothing] = new Empty[R](Monoid[R].empty)

  @inline def apply[E, R: SubscriptionOwner: SyncEmbed](create: Observer[E] => R): EmitterBuilder.Sync[E, R] =
    new Custom[E, R, SyncExecution](sink => create(sink))

  @inline def fromSourceOf[F[_]: Source, E, R: SubscriptionOwner: SyncEmbed: Monoid](
    source: F[E],
  ): EmitterBuilder[E, R] = new Stream[E, R](Observable.lift(source), Monoid[R].empty)

  // shortcuts for modifiers with less type ascriptions
  @inline def empty: EmitterBuilderExecution[Nothing, VModifier, Nothing] = emptyOf[VModifier]
  @inline def ofModifier[E](create: Observer[E] => VModifier): EmitterBuilder.Sync[E, VModifier] =
    apply[E, VModifier](create)
  @inline def ofNode[E](create: Observer[E] => VNode): EmitterBuilder.Sync[E, VNode] = apply[E, VNode](create)
  @inline def fromSource[F[_]: Source, E](source: F[E]): EmitterBuilder[E, VModifier] =
    fromSourceOf[F, E, VModifier](source)

  def fromEvent[E <: dom.Event](eventType: String): EmitterBuilder.Sync[E, VModifier] = apply[E, VModifier] { sink =>
    Emitter(eventType, e => sink.unsafeOnNext(e.asInstanceOf[E]))
  }

  @inline def combine[T, R: SubscriptionOwner: SyncEmbed: Monoid, Exec <: Execution](
    builders: EmitterBuilderExecution[T, R, Exec]*,
  ): EmitterBuilderExecution[T, R, Exec] = combineSeq(builders)

  def combineSeq[T, R: SubscriptionOwner: SyncEmbed: Monoid, Exec <: Execution](
    builders: Seq[EmitterBuilderExecution[T, R, Exec]],
  ): EmitterBuilderExecution[T, R, Exec] = new Custom[T, R, Exec](sink =>
    Monoid[R].combineAll(builders.map(_.forwardTo(sink))),
  )

  @deprecated("Use EmitterBuilder.fromEvent[E] instead", "0.11.0")
  @inline def apply[E <: dom.Event](eventType: String): EmitterBuilder.Sync[E, VModifier] = fromEvent[E](eventType)
  @deprecated("Use EmitterBuilder[E, O] instead", "0.11.0")
  @inline def custom[E, R: SubscriptionOwner: SyncEmbed](create: Observer[E] => R): EmitterBuilder.Sync[E, R] =
    apply[E, R](create)

  implicit def monoid[T, R: SubscriptionOwner: SyncEmbed: Monoid, Exec <: Execution]
    : Monoid[EmitterBuilderExecution[T, R, Exec]] = new Monoid[EmitterBuilderExecution[T, R, Exec]] {
    def empty: EmitterBuilderExecution[T, R, Exec] = EmitterBuilder.emptyOf[R]
    def combine(
      x: EmitterBuilderExecution[T, R, Exec],
      y: EmitterBuilderExecution[T, R, Exec],
    ): EmitterBuilderExecution[T, R, Exec] = EmitterBuilder.combine(x, y)
  }

  implicit def functor[R]: Functor[EmitterBuilder[*, R]] = new Functor[EmitterBuilder[*, R]] {
    def map[A, B](fa: EmitterBuilder[A, R])(f: A => B): EmitterBuilder[B, R] = fa.map(f)
  }

  implicit object bifunctor extends Bifunctor[EmitterBuilder] {
    def bimap[A, B, C, D](fab: EmitterBuilder[A, B])(f: A => C, g: B => D): EmitterBuilder[C, D] =
      fab.map(f).mapResult(g)
  }

  @inline implicit class HandlerIntegrationMonoid[O, R: Monoid, Exec <: Execution](
    builder: EmitterBuilderExecution[O, R, Exec],
  ) {
    @inline def handled(f: Observable[O] => R): SyncIO[R] = handledF[SyncIO](f)

    @inline def handledF[F[_]: SyncCats](f: Observable[O] => R): F[R] =
      Functor[F].map(SyncCats[F].delay(Subject.replayLatest[O]())) { handler =>
        Monoid[R].combine(builder.forwardTo(handler), f(handler))
      }
  }

  @inline implicit class HandlerIntegration[O, R, Exec <: Execution](builder: EmitterBuilderExecution[O, R, Exec]) {
    @inline def handledWith(f: (R, Observable[O]) => R): SyncIO[R] = handledWithF[SyncIO](f)

    @inline def handledWithF[F[_]: SyncCats](f: (R, Observable[O]) => R): F[R] =
      Functor[F].map(SyncCats[F].delay(Subject.replayLatest[O]())) { handler =>
        f(builder.forwardTo(handler), handler)
      }
  }

  @inline implicit final class VModifierEventOperations[Exec <: Execution](
    val builder: EmitterBuilderExecution[VModifier, VModifier, Exec],
  ) extends AnyVal {
    @inline def render: VModifier = builder.handled(VModifier(_))
  }

  @inline implicit class EmitterOperations[O, R: Monoid: SubscriptionOwner: SyncEmbed, Exec <: Execution](
    builder: EmitterBuilderExecution[O, R, Exec],
  ) {

    @inline def withLatestEmitter[T](emitter: EmitterBuilder[T, R]): EmitterBuilderExecution[(O, T), SyncIO[R], Exec] =
      combineWithLatestEmitter(builder, emitter)
    @inline def asLatestEmitter[T](emitter: EmitterBuilder[T, R]): EmitterBuilderExecution[T, SyncIO[R], Exec] =
      withLatestEmitter(emitter).map(_._2)

    @deprecated("Use asLatestEmitter(emitter) instead", "")
    @inline def useLatestEmitter[T](emitter: EmitterBuilder[T, R]): EmitterBuilderExecution[T, SyncIO[R], Exec] =
      asLatestEmitter(emitter)
  }

  @inline implicit class EventActions[O <: dom.Event, R](val builder: EmitterBuilder.Sync[O, R]) extends AnyVal {
    def onlyOwnEvents: EmitterBuilder[O, R]        = builder.filter(ev => ev.currentTarget == ev.target)
    def preventDefault: EmitterBuilder.Sync[O, R]  = builder.map { e => e.preventDefault(); e }
    def stopPropagation: EmitterBuilder.Sync[O, R] = builder.map { e => e.stopPropagation(); e }
  }

  @inline implicit class TargetAsInput[O <: dom.Event, R](builder: EmitterBuilder.Sync[O, R]) {
    object target {
      @inline def value: EmitterBuilder.Sync[String, R] = builder.map(_.target.asInstanceOf[dom.html.Input].value)
      @inline def valueAsNumber: EmitterBuilder.Sync[Double, R] =
        builder.map(_.target.asInstanceOf[dom.html.Input].valueAsNumber)
      @inline def checked: EmitterBuilder.Sync[Boolean, R] = builder.map(_.target.asInstanceOf[dom.html.Input].checked)
      @inline def asHtml: EmitterBuilder.Sync[dom.html.Element, R] =
        builder.map(_.target.asInstanceOf[dom.html.Element])
      @inline def asSvg: EmitterBuilder.Sync[dom.svg.Element, R] = builder.map(_.target.asInstanceOf[dom.svg.Element])
      @inline def asElement: EmitterBuilder.Sync[dom.Element, R] = builder.map(_.target.asInstanceOf[dom.Element])
    }
  }

  @inline implicit class CurrentTargetOnEvent[O <: dom.Event, R](val builder: EmitterBuilder.Sync[O, R])
      extends AnyVal {
    @inline def value: EmitterBuilder.Sync[String, R] = builder.map(_.currentTarget.asInstanceOf[dom.html.Input].value)
    @inline def valueAsNumber: EmitterBuilder.Sync[Double, R] =
      builder.map(_.currentTarget.asInstanceOf[dom.html.Input].valueAsNumber)
    @inline def checked: EmitterBuilder.Sync[Boolean, R] =
      builder.map(_.currentTarget.asInstanceOf[dom.html.Input].checked)
    @inline def asHtml: EmitterBuilder.Sync[dom.html.Element, R] =
      builder.map(_.currentTarget.asInstanceOf[dom.html.Element])
    @inline def asSvg: EmitterBuilder.Sync[dom.svg.Element, R] =
      builder.map(_.currentTarget.asInstanceOf[dom.svg.Element])
    @inline def asElement: EmitterBuilder.Sync[dom.Element, R] = builder.map(_.currentTarget.asInstanceOf[dom.Element])
  }

  @inline implicit class TypedElements[O <: dom.Element, R](val builder: EmitterBuilder.Sync[O, R]) extends AnyVal {
    @inline def asHtml: EmitterBuilder.Sync[dom.html.Element, R] =
      builder.asInstanceOf[EmitterBuilder.Sync[dom.html.Element, R]]
    @inline def asSvg: EmitterBuilder.Sync[dom.svg.Element, R] =
      builder.asInstanceOf[EmitterBuilder.Sync[dom.svg.Element, R]]
  }

  @inline implicit class TypedElementTuples[E <: dom.Element, R](val builder: EmitterBuilder.Sync[(E, E), R])
      extends AnyVal {
    @inline def asHtml: EmitterBuilder.Sync[(dom.html.Element, dom.html.Element), R] =
      builder.asInstanceOf[EmitterBuilder.Sync[(dom.html.Element, dom.html.Element), R]]
    @inline def asSvg: EmitterBuilder.Sync[(dom.svg.Element, dom.svg.Element), R] =
      builder.asInstanceOf[EmitterBuilder.Sync[(dom.svg.Element, dom.svg.Element), R]]
  }

  @noinline private def combineWithLatestEmitter[O, T, R: Monoid: SubscriptionOwner, Exec <: Execution](
    sourceEmitter: EmitterBuilderExecution[O, R, Exec],
    latestEmitter: EmitterBuilder[T, R],
  ): EmitterBuilderExecution[(O, T), SyncIO[R], Exec] =
    new Custom[(O, T), SyncIO[R], Exec]({ sink =>
      import scala.scalajs.js

      SyncIO {
        var lastValue: js.UndefOr[T] = js.undefined
        Monoid[R].combine(
          latestEmitter.forwardTo(Observer.create[T](lastValue = _, sink.unsafeOnError)),
          sourceEmitter.forwardTo(
            Observer.create[O](
              { o =>
                lastValue.foreach { t =>
                  sink.unsafeOnNext((o, t))
                }
              },
              sink.unsafeOnError,
            ),
          ),
        )
      }
    })

  @noinline private def forwardToInTransform[F[_]: Sink, I, O, O2 >: O, R: SubscriptionOwner: SyncEmbed](
    base: EmitterBuilder[I, R],
    transformF: Observable[I] => Observable[O],
    sink: F[O2],
  ): R = SyncEmbed[R].delay {
    val connectable = Observer.lift(sink).redirect(transformF)
    SubscriptionOwner[R].own(base.forwardTo(connectable.value))(connectable.connect)
  }
}

trait EventDispatcher[-T] {
  def dispatch(source: Observable[T]): Observable[Any]
}
object EventDispatcher {
  def ofModelUpdate[M, T](subject: Subject[M], update: (T, M) => M) = new EventDispatcher[T] {
    def dispatch(source: Observable[T]) = source.withLatestMap(subject)(update).via(subject)
  }
}
