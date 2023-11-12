package outwatch

import cats.{Functor, Monoid}
import cats.effect.{Sync => SyncCats, SyncIO}
import org.scalajs.dom
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

trait EmitterBuilderExecution[+O, +R, +Exec <: EmitterBuilderExecution.Execution] {

  @inline def forwardTo[F[_]: Sink](sink: F[_ >: O]): R

  // this method keeps the current Execution but actually, the caller must decide,
  // whether this really keeps the execution type or might be async. Therefore private.
  @inline private[outwatch] def transformWithExec[T](
    f: Observable[O] => Observable[T],
  ): EmitterBuilderExecution[T, R, Exec]
  @inline private[outwatch] def transformSinkWithExec[T](
    f: Observer[T] => Observer[O],
  ): EmitterBuilderExecution[T, R, Exec]

  @inline final def -->[F[_]: Sink](sink: F[_ >: O]): R = forwardTo(sink)

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

  @inline def foreachAsyncSingleOrDrop[G[_]: RunEffect](action: O => G[Unit]): R = mapEffectSingleOrDrop(action).discard
  @inline def doAsyncSingleOrDrop[G[_]: RunEffect](action: G[Unit]): R           = foreachAsyncSingleOrDrop(_ => action)

  @inline def via[F[_]: Sink](sink: F[_ >: O]): EmitterBuilderExecution[O, R, Exec] =
    transformSinkWithExec[O](Observer.combine(_, Observer.lift(sink)))
  @inline def dispatchWith(dispatcher: EventDispatcher[O]): R = transform[Any](dispatcher.dispatch).discard

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

  @inline final def as[T](value: => T): EmitterBuilderExecution[T, R, Exec]   = map(_ => value)
  @inline final def asDelay[T](value: T): EmitterBuilderExecution[T, R, Exec] = map(_ => value)

  @deprecated("Use .as(value) instead", "")
  @inline final def use[T](value: T): EmitterBuilderExecution[T, R, Exec] = as(value)
  @deprecated("Use .asDelay(value) instead", "")
  @inline final def useLazy[T](value: => T): EmitterBuilderExecution[T, R, Exec] = asDelay(value)

  @deprecated("Use .asDelay(value) instead", "")
  @inline final def mapTo[T](value: => T): EmitterBuilderExecution[T, R, Exec] = asDelay(value)
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

  @deprecated("Use .asFuture(value) instead", "")
  @inline final def useFuture[T](value: => Future[T]): EmitterBuilder[T, R] = asFuture(value)
  @inline final def asFuture[T](value: => Future[T]): EmitterBuilder[T, R]  = mapFuture(_ => value)

  @deprecated("Use .asEffectSingleOrDrop(value) instead", "")
  @inline final def useAsyncSingleOrDrop[G[_]: RunEffect, T](value: G[T]): EmitterBuilder[T, R] = asEffectSingleOrDrop(
    value,
  )
  @inline final def asEffectSingleOrDrop[G[_]: RunEffect, T](value: G[T]): EmitterBuilder[T, R] =
    mapEffectSingleOrDrop(_ => value)

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

  def async: EmitterBuilder[O, R] =
    transformWithExec[O](source => source.async)

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
    new EmitterBuilderExecution.MapResult[O, R, S, Exec](this, f)
}

object EmitterBuilderExecution {
  sealed trait Execution
  sealed trait SyncExecution extends Execution

  @inline final object Empty extends EmitterBuilderExecution[Nothing, VModifier, Nothing] {
    @inline private[outwatch] def transformSinkWithExec[T](
      f: Observer[T] => Observer[Nothing],
    ): EmitterBuilderExecution[T, VModifier, Nothing] = this
    @inline private[outwatch] def transformWithExec[T](
      f: Observable[Nothing] => Observable[T],
    ): EmitterBuilderExecution[T, VModifier, Nothing] = this
    @inline def forwardTo[F[_]: Sink](sink: F[_ >: Nothing]): VModifier = VModifier.empty
  }

  @inline final class Stream[+O, +R: SubscriptionOwner](source: Observable[O], result: R)
      extends EmitterBuilderExecution[O, R, Execution] {
    @inline private[outwatch] def transformSinkWithExec[T](
      f: Observer[T] => Observer[O],
    ): EmitterBuilderExecution[T, R, Execution] = new Stream(source.transformSink(f), result)
    @inline private[outwatch] def transformWithExec[T](
      f: Observable[O] => Observable[T],
    ): EmitterBuilderExecution[T, R, Execution] = new Stream(f(source), result)
    @inline def forwardTo[F[_]: Sink](sink: F[_ >: O]): R =
      SubscriptionOwner[R].own(result)(() => source.unsafeSubscribe(Observer.lift(sink)))
  }

  @inline final class MapResult[+O, +I, +R, +Exec <: Execution](base: EmitterBuilderExecution[O, I, Exec], mapF: I => R)
      extends EmitterBuilderExecution[O, R, Exec] {
    @inline private[outwatch] def transformSinkWithExec[T](
      f: Observer[T] => Observer[O],
    ): EmitterBuilderExecution[T, R, Exec] = new MapResult(base.transformSinkWithExec(f), mapF)
    @inline private[outwatch] def transformWithExec[T](
      f: Observable[O] => Observable[T],
    ): EmitterBuilderExecution[T, R, Exec] = new MapResult(base.transformWithExec(f), mapF)
    @inline def forwardTo[F[_]: Sink](sink: F[_ >: O]): R = mapF(base.forwardTo(sink))
  }

  @inline final class Custom[+O, +R: SubscriptionOwner, +Exec <: Execution](create: Observer[O] => R)
      extends EmitterBuilderExecution[O, R, Exec] {
    @inline private[outwatch] def transformSinkWithExec[T](
      f: Observer[T] => Observer[O],
    ): EmitterBuilderExecution[T, R, Exec] = new TransformSink(this, f)
    @inline private[outwatch] def transformWithExec[T](
      f: Observable[O] => Observable[T],
    ): EmitterBuilderExecution[T, R, Exec] = new Transform(this, f)
    @inline def forwardTo[F[_]: Sink](sink: F[_ >: O]): R = create(Observer.lift(sink))
  }

  @inline final class TransformSink[+I, +O, +R: SubscriptionOwner, Exec <: Execution](
    base: EmitterBuilderExecution[I, R, Exec],
    transformF: Observer[O] => Observer[I],
  ) extends EmitterBuilderExecution[O, R, Exec] {
    @inline private[outwatch] def transformSinkWithExec[T](
      f: Observer[T] => Observer[O],
    ): EmitterBuilderExecution[T, R, Exec] = new TransformSink(base, s => transformF(f(s)))
    @inline private[outwatch] def transformWithExec[T](
      f: Observable[O] => Observable[T],
    ): EmitterBuilderExecution[T, R, Exec] = new Transform[I, T, R, Exec](base, s => f(s.transformSink(transformF)))
    @inline def forwardTo[F[_]: Sink](sink: F[_ >: O]): R = base.forwardTo(transformF(Observer.lift(sink)))
  }

  @inline final class Transform[+I, +O, +R: SubscriptionOwner, Exec <: Execution](
    base: EmitterBuilderExecution[I, R, Exec],
    transformF: Observable[I] => Observable[O],
  ) extends EmitterBuilderExecution[O, R, Exec] {
    @inline private[outwatch] def transformSinkWithExec[T](
      f: Observer[T] => Observer[O],
    ): EmitterBuilderExecution[T, R, Exec] = new Transform[I, T, R, Exec](base, s => transformF(s).transformSink(f))
    @inline private[outwatch] def transformWithExec[T](
      f: Observable[O] => Observable[T],
    ): EmitterBuilderExecution[T, R, Exec] = new Transform[I, T, R, Exec](base, s => f(transformF(s)))
    @inline def forwardTo[F[_]: Sink](sink: F[_ >: O]): R = forwardToInTransform(base, transformF, sink)
  }

  @inline final class Access[-Env, +O, R[-_], Exec <: Execution](base: Env => EmitterBuilderExecution[O, R[Any], Exec])(
    implicit acc: AccessEnvironment[R],
  ) extends EmitterBuilderExecution[O, R[Env], Exec] {
    @inline private[outwatch] def transformSinkWithExec[T](
      f: Observer[T] => Observer[O],
    ): EmitterBuilderExecution[T, R[Env], Exec] = new Access(env => base(env).transformSinkWithExec(f))
    @inline private[outwatch] def transformWithExec[T](
      f: Observable[O] => Observable[T],
    ): EmitterBuilderExecution[T, R[Env], Exec] = new Access(env => base(env).transformWithExec(f))
    @inline def forwardTo[F[_]: Sink](sink: F[_ >: O]): R[Env] =
      AccessEnvironment[R].access(env => base(env).forwardTo(sink))
  }

  @inline implicit def monoid[T, Exec <: Execution]: Monoid[EmitterBuilderExecution[T, VModifier, Exec]] =
    new EmitterBuilderMonoid[T, Exec]
  @inline final class EmitterBuilderMonoid[T, Exec <: Execution]
      extends Monoid[EmitterBuilderExecution[T, VModifier, Exec]] {
    @inline def empty: EmitterBuilderExecution[T, VModifier, Exec] = EmitterBuilder.empty
    @inline def combine(
      x: EmitterBuilderExecution[T, VModifier, Exec],
      y: EmitterBuilderExecution[T, VModifier, Exec],
    ): EmitterBuilderExecution[T, VModifier, Exec] = EmitterBuilder.combine(x, y)
    // @inline override def combineAll(x: Iterable[EmitterBuilderExecution[T, VModifier, Exec]]): EmitterBuilderExecution[T, VModifier, Exec] = EmitterBuilder.combineAll(x)
  }

  @inline implicit def functor[R, Exec <: Execution]: Functor[EmitterBuilderExecution[*, R, Exec]] =
    new EmitterBuilderFunctor[R, Exec]
  @inline final class EmitterBuilderFunctor[R, Exec <: Execution] extends Functor[EmitterBuilderExecution[*, R, Exec]] {
    @inline def map[A, B](fa: EmitterBuilderExecution[A, R, Exec])(f: A => B): EmitterBuilderExecution[B, R, Exec] =
      fa.map(f)
  }

  @inline implicit final class VModifierOperations[Env, O, Exec <: Execution](
    val builder: EmitterBuilderExecution[O, VModifierM[Env], Exec],
  ) extends AnyVal {
    @inline def handled[R](f: Observable[O] => VModifierM[R]): SyncIO[VModifierM[Env with R]] = handledF[SyncIO, R](f)
    @inline def handledF[F[_]: SyncCats, R](f: Observable[O] => VModifierM[R]): F[VModifierM[Env with R]] =
      handledWithF[F, Env with R]((r, o) => VModifierM(r, f(o)))
    @inline def handledWith[R](f: (VModifierM[Env], Observable[O]) => VModifierM[R]): SyncIO[VModifierM[R]] =
      handledWithF[SyncIO, R](f)
    @inline def handledWithF[F[_]: SyncCats, R](
      f: (VModifierM[Env], Observable[O]) => VModifierM[R],
    ): F[VModifierM[R]] = Functor[F].map(SyncCats[F].delay(Subject.replayLatest[O]())) { handler =>
      f(builder.forwardTo(handler), handler)
    }

    @deprecated("Use asLatestEmitter(emitter) instead", "")
    @inline def useLatestEmitter[T](
      emitter: EmitterBuilder[T, VModifierM[Env]],
    ): EmitterBuilderExecution[T, VModifierM[Env], Exec] = asLatestEmitter(emitter)
    @inline def withLatestEmitter[T](
      emitter: EmitterBuilder[T, VModifierM[Env]],
    ): EmitterBuilderExecution[(O, T), VModifierM[Env], Exec] = combineWithLatestEmitter(builder, emitter)
    @inline def asLatestEmitter[T](
      emitter: EmitterBuilder[T, VModifierM[Env]],
    ): EmitterBuilderExecution[T, VModifierM[Env], Exec] = withLatestEmitter(emitter).map(_._2)
  }

  @inline implicit final class AccessEnvironmentOperations[Env, O, R[-_], Exec <: Execution](
    val builder: EmitterBuilderExecution[O, R[Env], Exec],
  )(implicit acc: AccessEnvironment[R]) {
    @inline def provide(env: Env): EmitterBuilderExecution[O, R[Any], Exec] =
      builder.mapResult(r => AccessEnvironment[R].provide(r)(env))
    @inline def provideSome[REnv](map: REnv => Env): EmitterBuilderExecution[O, R[REnv], Exec] =
      builder.mapResult(r => AccessEnvironment[R].provideSome(r)(map))

    @inline def asAccess[REnv]: EmitterBuilderExecution[REnv, R[Env with REnv], Exec] =
      EmitterBuilder.accessM[REnv](builder.as(_))
    @inline def withAccess[REnv]: EmitterBuilderExecution[(O, REnv), R[Env with REnv], Exec] =
      EmitterBuilder.accessM[REnv](env => builder.map(_ -> env))
  }

  @inline implicit final class EventActions[O <: dom.Event, R](val builder: EmitterBuilder.Sync[O, R]) extends AnyVal {
    @inline def asElement: EmitterBuilder.Sync[dom.Element, R] = builder.map(_.currentTarget.asInstanceOf[dom.Element])
    @inline def asHtml: EmitterBuilder.Sync[dom.html.Element, R] =
      builder.map(_.currentTarget.asInstanceOf[dom.html.Element])
    @inline def asSvg: EmitterBuilder.Sync[dom.svg.Element, R] =
      builder.map(_.currentTarget.asInstanceOf[dom.svg.Element])

    @inline def onlyOwnEvents: EmitterBuilder.Sync[O, R]   = builder.filter(ev => ev.currentTarget == ev.target)
    @inline def preventDefault: EmitterBuilder.Sync[O, R]  = builder.map { e => e.preventDefault(); e }
    @inline def stopPropagation: EmitterBuilder.Sync[O, R] = builder.map { e => e.stopPropagation(); e }

    @inline def value: EmitterBuilder.Sync[String, R] =
      builder.map(e => e.currentTarget.asInstanceOf[dom.html.Input].value)
    @inline def valueAsNumber: EmitterBuilder.Sync[Double, R] =
      builder.map(e => e.currentTarget.asInstanceOf[dom.html.Input].valueAsNumber)
    @inline def checked: EmitterBuilder.Sync[Boolean, R] =
      builder.map(e => e.currentTarget.asInstanceOf[dom.html.Input].checked)

    @inline def target: EventActionsTargetOps[O, R] = new EventActionsTargetOps(builder)
  }

  @inline final class EventActionsTargetOps[O <: dom.Event, R](val builder: EmitterBuilder.Sync[O, R]) extends AnyVal {
    @inline def value: EmitterBuilder.Sync[String, R] = builder.map(_.target.asInstanceOf[dom.html.Input].value)
    @inline def valueAsNumber: EmitterBuilder.Sync[Double, R] =
      builder.map(_.target.asInstanceOf[dom.html.Input].valueAsNumber)
    @inline def checked: EmitterBuilder.Sync[Boolean, R] = builder.map(_.target.asInstanceOf[dom.html.Input].checked)
  }

  @inline implicit final class TypedElements[O <: dom.Element, R, Exec <: Execution](
    val builder: EmitterBuilderExecution[O, R, Exec],
  ) extends AnyVal {
    @inline def asHtml: EmitterBuilderExecution[dom.html.Element, R, Exec] =
      builder.asInstanceOf[EmitterBuilderExecution[dom.html.Element, R, Exec]]
    @inline def asSvg: EmitterBuilderExecution[dom.svg.Element, R, Exec] =
      builder.asInstanceOf[EmitterBuilderExecution[dom.svg.Element, R, Exec]]
  }

  @inline implicit final class TypedElementTuples[E <: dom.Element, R, Exec <: Execution](
    val builder: EmitterBuilderExecution[(E, E), R, Exec],
  ) extends AnyVal {
    @inline def asHtml: EmitterBuilderExecution[(dom.html.Element, dom.html.Element), R, Exec] =
      builder.asInstanceOf[EmitterBuilderExecution[(dom.html.Element, dom.html.Element), R, Exec]]
    @inline def asSvg: EmitterBuilderExecution[(dom.svg.Element, dom.svg.Element), R, Exec] =
      builder.asInstanceOf[EmitterBuilderExecution[(dom.svg.Element, dom.svg.Element), R, Exec]]
  }

  @noinline private def combineWithLatestEmitter[Env, O, T, Exec <: Execution](
    sourceEmitter: EmitterBuilderExecution[O, VModifierM[Env], Exec],
    latestEmitter: EmitterBuilder[T, VModifierM[Env]],
  ): EmitterBuilderExecution[(O, T), VModifierM[Env], Exec] =
    new Custom[(O, T), VModifierM[Env], Exec]({ sink =>
      VModifierM.delay {
        var lastValue: Option[T] = None
        VModifierM(
          latestEmitter.forwardTo(Observer.create[T](v => lastValue = Some(v), sink.unsafeOnError)),
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

  @noinline private def forwardToInTransform[F[_]: Sink, I, O, R: SubscriptionOwner](
    base: EmitterBuilder[I, R],
    transformF: Observable[I] => Observable[O],
    sink: F[_ >: O],
  ): R = {
    val connectable = Observer.lift(sink).redirect(transformF)
    SubscriptionOwner[R].own(base.forwardTo(connectable.value))(connectable.connect)
  }

  @inline implicit final class VModifierEventOperations[Exec <: Execution](
    val builder: EmitterBuilderExecution[VModifier, VModifier, Exec],
  ) extends AnyVal {
    @inline def render: VModifier = builder.handled(VModifier(_))
  }
}

object EmitterBuilder {
  import EmitterBuilderExecution._

  type Sync[+O, +R] = EmitterBuilderExecution[O, R, SyncExecution]

  @inline final def empty: EmitterBuilderExecution[Nothing, VModifier, Nothing] = Empty
  @inline final def fromSource[F[_]: Source, E](source: F[E]): EmitterBuilder[E, VModifier] =
    fromSourceOf[F, E, VModifier](source)
  @inline def fromSourceOf[F[_]: Source, E, R: SubscriptionOwner: Monoid](source: F[E]): EmitterBuilder[E, R] =
    new Stream[E, R](Observable.lift(source), Monoid[R].empty)

  final def fromEvent[E <: dom.Event](eventType: String): EmitterBuilder.Sync[E, VModifier] =
    EmitterBuilder[E, VModifier] { sink =>
      Emitter(eventType, e => sink.unsafeOnNext(e.asInstanceOf[E]))
    }

  @inline def apply[E, R: SubscriptionOwner](create: Observer[E] => R): EmitterBuilder.Sync[E, R] =
    new Custom[E, R, SyncExecution](sink => create(sink))
  @inline def ofModifier[E](create: Observer[E] => VModifier): EmitterBuilder.Sync[E, VModifier] =
    ofModifierM[Any, E](create)
  @inline def ofVNode[E](create: Observer[E] => VNode): EmitterBuilder.Sync[E, VNode] = ofVNodeM[Any, E](create)
  @inline def ofModifierM[Env, E](create: Observer[E] => VModifierM[Env]): EmitterBuilder.Sync[E, VModifierM[Env]] =
    apply[E, VModifierM[Env]](create)
  @inline def ofVNodeM[Env, E](create: Observer[E] => VNodeM[Env]): EmitterBuilder.Sync[E, VNodeM[Env]] =
    apply[E, VNodeM[Env]](create)

  @inline def combine[T, R: Monoid: SubscriptionOwner, Exec <: Execution](
    builders: EmitterBuilderExecution[T, R, Exec]*,
  ): EmitterBuilderExecution[T, R, Exec] = combineAll(builders)
  def combineAll[T, R: Monoid: SubscriptionOwner, Exec <: Execution](
    builders: Iterable[EmitterBuilderExecution[T, R, Exec]],
  ): EmitterBuilderExecution[T, R, Exec] = new Custom[T, R, Exec](sink =>
    Monoid[R].combineAll(builders.iterator.map(_ --> sink)),
  )

  @deprecated("Use EmitterBuilder.fromEvent[E] instead", "0.11.0")
  @inline def apply[E <: dom.Event](eventType: String): EmitterBuilder.Sync[E, VModifier] = fromEvent[E](eventType)
  @deprecated("Use EmitterBuilder[E, O] instead", "0.11.0")
  @inline def custom[E, R: SubscriptionOwner](create: Observer[E] => R): EmitterBuilder.Sync[E, R] = apply(create)
  @deprecated("Use EmitterBuilder.ofVNode[E] instead", "1.0.0")
  @inline def ofNode[E](create: Observer[E] => VNode): EmitterBuilder.Sync[E, VNode] = ofVNode[E](create)

  @inline def access[Env] = new PartiallyAppliedAccess[Env]
  @inline class PartiallyAppliedAccess[Env] {
    @inline def apply[O, T[-_], Exec <: Execution](emitter: Env => EmitterBuilderExecution[O, T[Any], Exec])(implicit
      acc: AccessEnvironment[T],
    ): EmitterBuilderExecution[O, T[Env], Exec] = new Access(env => emitter(env))
  }
  @inline def accessM[Env] = new PartiallyAppliedAccessM[Env]
  @inline class PartiallyAppliedAccessM[Env] {
    @inline def apply[R, O, T[-_], Exec <: Execution](
      emitter: Env => EmitterBuilderExecution[O, T[R], Exec],
    )(implicit acc: AccessEnvironment[T]): EmitterBuilderExecution[O, T[Env with R], Exec] =
      access[Env with R][O, T, Exec](env => emitter(env).provide(env))
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
