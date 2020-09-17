package outwatch

import cats.{Monoid, MonoidK, Functor, Bifunctor}
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
// onClick.mapResult(emitter => Modifier(emitter, ???)): EmitterBuilder[Int, RModifier]
//
// Now you have conbined the emitter with another Modifier, so the combined modifier
// will later be rendered instead of only the emitter. Then you can describe the action
// that should be done when an event triggers:
//
// onClick.map(_ => 1).foreach(doSomething(_)): Modifier
//
// The EmitterBuilder result must be an RModifier[_].
//


trait REmitterBuilderExecution[-Env, +O, +R <: RModifier[Env], +Exec <: EmitterBuilder.Execution] {

  @inline def forwardTo[F[_] : Sink](sink: F[_ >: O]): R

  // this method keeps the current Execution but actually, the caller must decide,
  // whether this really keeps the execution type or might be async. Therefore private.
  @inline private[outwatch] def transformWithExec[T](f: Observable[O] => Observable[T]): REmitterBuilderExecution[Env, T, R, Exec]
  @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[O]): REmitterBuilderExecution[Env, T, R, Exec]

  @inline final def -->[F[_] : Sink](sink: F[_ >: O]): R = forwardTo(sink)

  @inline final def discard: R = forwardTo(Observer.empty)

  @inline final def foreach(action: O => Unit): R = forwardTo(Observer.create(action))
  @inline final def foreach(action: => Unit): R = foreach(_ => action)

  @inline final def foreachSync[G[_] : RunSyncEffect](action: O => G[Unit]): R = mapSync(action).discard
  @inline final def doSync[G[_] : RunSyncEffect](action: G[Unit]): R = foreachSync(_ => action)

  @inline final def foreachAsync[G[_] : Effect](action: O => G[Unit]): R = concatMapAsync(action).discard
  @inline final def doAsync[G[_] : Effect](action: G[Unit]): R = foreachAsync(_ => action)

  final def map[T](f: O => T): REmitterBuilderExecution[Env, T, R, Exec] = transformSinkWithExec(_.contramap(f))

  final def collect[T](f: PartialFunction[O, T]): REmitterBuilderExecution[Env, T, R, Exec] = transformSinkWithExec(_.contracollect(f))

  final def filter(predicate: O => Boolean): REmitterBuilderExecution[Env, O, R, Exec] = transformSinkWithExec(_.contrafilter(predicate))

  final def mapFilter[T](f: O => Option[T]): REmitterBuilderExecution[Env, T, R, Exec] = transformSinkWithExec(_.contramapFilter(f))

  @inline final def use[T](value: T): REmitterBuilderExecution[Env, T, R, Exec] = map(_ => value)
  @inline final def useLazy[T](value: => T): REmitterBuilderExecution[Env, T, R, Exec] = map(_ => value)

  @deprecated("Use .useLazy(value) instead", "")
  @inline final def mapTo[T](value: => T): REmitterBuilderExecution[Env, T, R, Exec] = useLazy(value)
  @deprecated("Use .use(value) instead", "")
  @inline final def apply[T](value: T): REmitterBuilderExecution[Env, T, R, Exec] = use(value)

  @inline final def useSync[G[_]: RunSyncEffect, T](value: G[T]): REmitterBuilderExecution[Env, T, R, Exec] = mapSync(_ => value)

  @inline final def useAsync[G[_]: Effect, T](value: G[T]): REmitterBuilder[Env, T, R] = concatMapAsync(_ => value)

  @inline final def apply[G[_] : Source, T](source: G[T]): REmitterBuilderExecution[Env, T, R, Exec] = useLatest(source)

  final def useLatest[F[_] : Source, T](latest: F[T]): REmitterBuilderExecution[Env, T, R, Exec] =
    transformWithExec[T](source => Observable.withLatestMap(source, latest)((_, u) => u))

  final def withLatest[F[_] : Source, T](latest: F[T]): REmitterBuilderExecution[Env, (O, T), R, Exec] =
    transformWithExec[(O, T)](source => Observable.withLatest(source, latest))

  final def scan[T](seed: T)(f: (T, O) => T): REmitterBuilderExecution[Env, T, R, Exec] =
    transformWithExec[T](source => Observable.scan(source)(seed)(f))

  final def useScan[T](seed: T)(f: T => T): REmitterBuilderExecution[Env, T, R, Exec] = scan(seed)((t,_) => f(t))

  final def scan0[T](seed: T)(f: (T, O) => T): REmitterBuilderExecution[Env, T, R, Exec] =
    transformWithExec[T](source => Observable.scan0(source)(seed)(f))

  final def useScan0[T](seed: T)(f: T => T): REmitterBuilderExecution[Env, T, R, Exec] = scan0(seed)((t,_) => f(t))

  final def debounce(duration: FiniteDuration): REmitterBuilder[Env, O, R] =
    transformWithExec[O](source => Observable.debounce(source)(duration))

  final def debounceMillis(millis: Int): REmitterBuilder[Env, O, R] =
    transformWithExec[O](source => Observable.debounceMillis(source)(millis))

  final def async: REmitterBuilder[Env, O, R] =
    transformWithExec[O](source => Observable.async(source))

  final def delay(duration: FiniteDuration): REmitterBuilder[Env, O, R] =
    transformWithExec[O](source => Observable.delay(source)(duration))

  final def delayMillis(millis: Int): REmitterBuilder[Env, O, R] =
    transformWithExec[O](source => Observable.delayMillis(source)(millis))

  final def concatMapFuture[T](f: O => Future[T])(implicit ec: ExecutionContext): REmitterBuilder[Env, T, R] =
    transformWithExec[T](source => Observable.concatMapFuture(source)(f))

  final def concatMapAsync[G[_]: Effect, T](f: O => G[T]): REmitterBuilder[Env, T, R] =
    transformWithExec[T](source => Observable.concatMapAsync(source)(f))

  final def mapSync[G[_]: RunSyncEffect, T](f: O => G[T]): REmitterBuilderExecution[Env, T, R, Exec] =
    transformWithExec[T](source => Observable.mapSync(source)(f))

  final def transformLifted[F[_] : Source : LiftSource, OO >: O, T](f: F[OO] => F[T]): REmitterBuilder[Env, T, R] =
    transformWithExec[T]((s: Observable[OO]) => Observable.lift(f(s.liftSource[F])))

  final def transformLift[F[_] : Source, T](f: Observable[O] => F[T]): REmitterBuilder[Env, T, R] =
    transformWithExec[T]((s: Observable[O]) => Observable.lift(f(s)))

  // do not expose transform with current exec but just normal Emitterbuilder. This tranform might be async
  @inline final def transform[T](f: Observable[O] => Observable[T]): REmitterBuilder[Env, T, R] = transformWithExec(f)
  @inline final def transformSink[T](f: Observer[T] => Observer[O]): REmitterBuilder[Env, T, R] = transformSinkWithExec(f)

  @inline final def mapResult[SEnv, S <: RModifier[SEnv]](f: R => S): REmitterBuilderExecution[SEnv, O, S, Exec] = new EmitterBuilder.MapResult[Env, SEnv, O, R, S, Exec](this, f)

  @inline final def provide(env: Env): EmitterBuilderExecution[O, Modifier, Exec] = new EmitterBuilder.Provide[Env, O, Exec](this, env)
}

object EmitterBuilder {

  sealed trait Execution
  sealed trait SyncExecution extends Execution

  type RSync[-Env, +O, +R <: RModifier[Env]] = REmitterBuilderExecution[Env, O, R, SyncExecution]
  type Sync[+O, +R <: Modifier] = RSync[Any, O, R]

  @inline object Empty extends EmitterBuilderExecution[Nothing, Modifier, Nothing] {
    @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[Nothing]): EmitterBuilderExecution[T, Modifier, Nothing] = this
    @inline private[outwatch] def transformWithExec[T](f: Observable[Nothing] => Observable[T]): EmitterBuilderExecution[T, Modifier, Nothing] = this
    @inline def forwardTo[F[_] : Sink](sink: F[_ >: Nothing]): Modifier = Modifier.empty
  }

  @inline final class MapResult[-IEnv, -Env, +O, +I <: RModifier[IEnv], +R <: RModifier[Env], +Exec <: Execution](base: REmitterBuilder[IEnv, O, I], mapF: I => R) extends REmitterBuilderExecution[Env, O, R, Exec] {
    @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[O]): REmitterBuilderExecution[Env, T, R, Exec] = new MapResult(base.transformSink(f), mapF)
    @inline private[outwatch] def transformWithExec[T](f: Observable[O] => Observable[T]): REmitterBuilderExecution[Env, T, R, Exec] = new MapResult(base.transformWithExec(f), mapF)
    @inline def forwardTo[F[_] : Sink](sink: F[_ >: O]): R = mapF(base.forwardTo(sink))
  }

  @inline final class Stream[S[_] : Source, +O](source: S[O]) extends EmitterBuilderExecution[O, Modifier, Execution] {
    @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[O]): EmitterBuilderExecution[T, Modifier, Execution] = new Stream(Observable.transformSink(source)(f))
    @inline private[outwatch] def transformWithExec[T](f: Observable[O] => Observable[T]): EmitterBuilderExecution[T, Modifier, Execution] = new Stream(f(Observable.lift(source)))
    @inline def forwardTo[F[_] : Sink](sink: F[_ >: O]): Modifier = Modifier.managedFunction(() => Source[S].subscribe(source)(sink))
  }

  @inline final class Custom[-Env, +O, +R <: RModifier[Env], +Exec <: Execution](create: Observer[O] => R) extends REmitterBuilderExecution[Env, O, R, Exec] {
    @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[O]): REmitterBuilderExecution[Env, T, R, Exec] = new TransformSink(this, f)
    @inline private[outwatch] def transformWithExec[T](f: Observable[O] => Observable[T]): REmitterBuilderExecution[Env, T, R, Exec] = ??? //new Transform(this, f)
    @inline def forwardTo[F[_] : Sink](sink: F[_ >: O]): R = create(Observer.lift(sink))
  }

  @inline final class TransformSink[-Env, +I, +O, +R <: RModifier[Env], Exec <: Execution](base: REmitterBuilderExecution[Env, I, R, Exec], transformF: Observer[O] => Observer[I]) extends REmitterBuilderExecution[Env, O, R, Exec] {
    @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[O]): REmitterBuilderExecution[Env, T, R, Exec] = new TransformSink(base, s => transformF(f(s)))
    @inline private[outwatch] def transformWithExec[T](f: Observable[O] => Observable[T]): REmitterBuilderExecution[Env, T, R, Exec] = new Transform[Env, I, T, R, Exec](base, s => f(Observable.transformSink(s)(transformF)))
    @inline def forwardTo[F[_] : Sink](sink: F[_ >: O]): R = base.forwardTo(transformF(Observer.lift(sink)))
  }

  @inline final class Transform[-Env, +I, +O, +R <: RModifier[Env], Exec <: Execution](base: REmitterBuilderExecution[Env, I, R, Exec], transformF: Observable[I] => Observable[O]) extends REmitterBuilderExecution[Env, O, R, Exec] {
    @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[O]): REmitterBuilderExecution[Env, T, R, Exec] = new Transform[Env, I, T, R, Exec](base, s => Observable.transformSink(transformF(s))(f))
    @inline private[outwatch] def transformWithExec[T](f: Observable[O] => Observable[T]): REmitterBuilderExecution[Env, T, R, Exec] = new Transform[Env, I, T, R, Exec](base, s => f(transformF(s)))
    @inline def forwardTo[F[_] : Sink](sink: F[_ >: O]): R = ??? //forwardToInTransform(base, transformF, sink)
  }

  @inline final class Access[-Env, +O, Exec <: Execution](base: Env => EmitterBuilderExecution[O, Modifier, Exec]) extends REmitterBuilderExecution[Env, O, RModifier[Env], Exec] {
    @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[O]): REmitterBuilderExecution[Env, T, RModifier[Env], Exec] = new Access(env => base(env).transformSinkWithExec(f))
    @inline private[outwatch] def transformWithExec[T](f: Observable[O] => Observable[T]): REmitterBuilderExecution[Env, T, RModifier[Env], Exec] = new Access(env => base(env).transformWithExec(f))
    @inline def forwardTo[F[_] : Sink](sink: F[_ >: O]): RModifier[Env] = RModifier.access(env => base(env).forwardTo(sink))
  }

  @inline final class Provide[-Env, +O, Exec <: Execution](base: REmitterBuilderExecution[Env, O, RModifier[Env], Exec], env: Env) extends EmitterBuilderExecution[O, Modifier, Exec] {
    @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[O]): EmitterBuilderExecution[T, Modifier, Exec] = new Provide(base.transformSinkWithExec(f), env)
    @inline private[outwatch] def transformWithExec[T](f: Observable[O] => Observable[T]): EmitterBuilderExecution[T, Modifier, Exec] = new Provide(base.transformWithExec(f), env)
    @inline def forwardTo[F[_] : Sink](sink: F[_ >: O]): Modifier = base.forwardTo(sink).provide(env)
  }

  @inline def empty: EmitterBuilderExecution[Nothing, Modifier, Nothing] = Empty
  @inline def apply[E, R <: Modifier](create: Observer[E] => R): EmitterBuilder.Sync[E, R] = applyR(create)
  @inline def applyR[Env, E, R <: RModifier[Env]](create: Observer[E] => R): EmitterBuilder.RSync[Env, E, R] = new Custom[Env, E, R, SyncExecution](sink => create(sink))
  @inline def ofModifier[E](create: Observer[E] => Modifier): EmitterBuilder.Sync[E, Modifier] = ofModifierR[Any, E](create)
  @inline def ofModifierR[Env, E](create: Observer[E] => RModifier[Env]): EmitterBuilder.RSync[Env, E, RModifier[Env]] = applyR[Env, E, RModifier[Env]](create)
  @inline def ofNode[E](create: Observer[E] => VNode): EmitterBuilder.Sync[E, VNode] = ofNodeR[Any, E](create)
  @inline def ofNodeR[Env, E](create: Observer[E] => RVNode[Env]): EmitterBuilder.RSync[Env, E, RVNode[Env]] = applyR[Env, E, RVNode[Env]](create)
  @inline def fromSource[F[_] : Source, E](source: F[E]): EmitterBuilder[E, Modifier] = new Stream[F, E](source)

  @inline def access[Env] = new PartiallyAppliedAccess[Env]
  @inline class PartiallyAppliedAccess[Env] {
    @inline def apply[O, Exec <: Execution](emitter: Env => EmitterBuilderExecution[O, Modifier, Exec]): REmitterBuilderExecution[Env, O, RModifier[Env], Exec] = new Access[Env, O, Exec](emitter)
  }

  def fromEvent[E <: Event](eventType: String): EmitterBuilder.Sync[E, Modifier] = apply { sink =>
    Emitter(eventType, e => sink.onNext(e.asInstanceOf[E]))
  }

  @inline def combine[Env, T, Exec <: Execution](builders: REmitterBuilderExecution[Env, T, RModifier[Env], Exec]*): REmitterBuilderExecution[Env, T, RModifier[Env], Exec] = combineSeq(builders)

  def combineSeq[Env, T, Exec <: Execution](builders: Seq[REmitterBuilderExecution[Env, T, RModifier[Env], Exec]]): REmitterBuilderExecution[Env, T, RModifier[Env], Exec] = new Custom[Env, T, RModifier[Env], Exec](sink =>
    RModifier.composite(builders.map(_ --> sink))
  )

  @deprecated("Use EmitterBuilder.fromEvent[E] instead", "0.11.0")
  @inline def apply[E <: Event](eventType: String): EmitterBuilder.Sync[E, Modifier] = fromEvent[E](eventType)
  @deprecated("Use EmitterBuilder[E, O] instead", "0.11.0")
  @inline def custom[E, R <: Modifier](create: Observer[E] => R): EmitterBuilder.Sync[E, R] = apply(create)

  implicit def monoid[Env, T, Exec <: Execution]: Monoid[REmitterBuilderExecution[Env, T, RModifier[Env], Exec]] = new Monoid[REmitterBuilderExecution[Env, T, RModifier[Env], Exec]] {
    def empty: REmitterBuilderExecution[Env, T, RModifier[Env], Exec] = EmitterBuilder.empty
    def combine(x: REmitterBuilderExecution[Env, T, RModifier[Env], Exec], y: REmitterBuilderExecution[Env, T, RModifier[Env], Exec]): REmitterBuilderExecution[Env, T, RModifier[Env], Exec] = EmitterBuilder.combine(x, y)
  }

  implicit def functor[Env, R <: RModifier[Env], Exec <: Execution]: Functor[REmitterBuilderExecution[Env, ?, R, Exec]] = new Functor[REmitterBuilderExecution[Env, ?, R, Exec]] {
    def map[A, B](fa: REmitterBuilderExecution[Env, A, R, Exec])(f: A => B): REmitterBuilderExecution[Env, B, R, Exec] = fa.map(f)
  }

  @inline implicit class HandlerIntegration[Env, O, Exec <: Execution](val builder: REmitterBuilderExecution[Env, O, RModifier[Env], Exec]) extends AnyVal {
    @inline def handled(f: Observable[O] => RModifier[Env]): SyncIO[RModifier[Env]] = handledF[SyncIO](f)

    @inline def handledF[F[_] : SyncCats](f: Observable[O] => RModifier[Env]): F[RModifier[Env]] = ??? //handledWithF[F]((r, o) => RModifier[Env](r, o))

    @inline def handledWith(f: (RModifier[Env], Observable[O]) => RModifier[Env]): SyncIO[RModifier[Env]] = handledWithF[SyncIO](f)

    @inline def handledWithF[F[_] : SyncCats](f: (RModifier[Env], Observable[O]) => RModifier[Env]): F[RModifier[Env]] = Functor[F].map(handler.Handler.createF[F, O]) { handler =>
      f(builder.forwardTo(handler), handler)
    }
  }

  @inline implicit class EmitterOperations[Env, O, Exec <: Execution](val builder: REmitterBuilderExecution[Env, O, RModifier[Env], Exec]) extends AnyVal {
    @inline def withLatestEmitter[T](emitter: REmitterBuilder[Env, T, RModifier[Env]]): REmitterBuilderExecution[Env, (O,T), RModifier[Env], Exec] = combineWithLatestEmitter(builder, emitter)

    @inline def useLatestEmitter[T](emitter: REmitterBuilder[Env, T, RModifier[Env]]): REmitterBuilderExecution[Env, T, RModifier[Env], Exec] = combineWithLatestEmitter(builder, emitter).map(_._2)
  }

  @inline implicit class EventActions[Env, O <: Event, R <: RModifier[Env]](val builder: EmitterBuilder.RSync[Env, O, R]) extends AnyVal {
    @inline def onlyOwnEvents: EmitterBuilder.RSync[Env, O, R] = builder.filter(ev => ev.currentTarget == ev.target)
    @inline def preventDefault: EmitterBuilder.RSync[Env, O, R] = builder.map { e => e.preventDefault; e }
    @inline def stopPropagation: EmitterBuilder.RSync[Env, O, R] = builder.map { e => e.stopPropagation; e }

    @inline def value: EmitterBuilder.RSync[Env, String, R] = builder.map(e => e.currentTarget.asInstanceOf[html.Input].value)
    @inline def valueAsNumber: EmitterBuilder.RSync[Env, Double, R] = builder.map(e => e.currentTarget.asInstanceOf[html.Input].valueAsNumber)
    @inline def checked: EmitterBuilder.RSync[Env, Boolean, R] = builder.map(e => e.currentTarget.asInstanceOf[html.Input].checked)

    @inline def asHtml: EmitterBuilder.RSync[Env, html.Element, R] = builder.map(_.currentTarget.asInstanceOf[html.Element])
    @inline def asSvg: EmitterBuilder.RSync[Env, svg.Element, R] = builder.map(_.currentTarget.asInstanceOf[svg.Element])
    @inline def asElement: EmitterBuilder.RSync[Env, Element, R] = builder.map(_.currentTarget.asInstanceOf[Element])
  }

  @inline implicit class TargetAsInput[Env, O <: Event, R <: RModifier[Env]](builder: EmitterBuilder.RSync[Env, O, R]) {
    object target {
      @inline def value: EmitterBuilder.RSync[Env, String, R] = builder.map(_.target.asInstanceOf[html.Input].value)
      @inline def valueAsNumber: EmitterBuilder.RSync[Env, Double, R] = builder.map(_.target.asInstanceOf[html.Input].valueAsNumber)
      @inline def checked: EmitterBuilder.RSync[Env, Boolean, R] = builder.map(_.target.asInstanceOf[html.Input].checked)
    }
  }

  @inline implicit class TypedElements[Env, O <: Element, R <: RModifier[Env], Exec <: Execution](val builder: REmitterBuilderExecution[Env, O, R, Exec]) extends AnyVal {
    @inline def asHtml: REmitterBuilderExecution[Env, html.Element, R, Exec] = builder.asInstanceOf[REmitterBuilderExecution[Env, html.Element, R, Exec]]
    @inline def asSvg: REmitterBuilderExecution[Env, svg.Element, R, Exec] = builder.asInstanceOf[REmitterBuilderExecution[Env, svg.Element, R, Exec]]
  }

  @inline implicit class TypedElementTuples[Env, E <: Element, R <: RModifier[Env], Exec <: Execution](val builder: REmitterBuilderExecution[Env, (E,E), R, Exec]) extends AnyVal {
    @inline def asHtml: REmitterBuilderExecution[Env, (html.Element, html.Element), R, Exec] = builder.asInstanceOf[REmitterBuilderExecution[Env, (html.Element, html.Element), R, Exec]]
    @inline def asSvg: REmitterBuilderExecution[Env, (svg.Element, svg.Element), R, Exec] = builder.asInstanceOf[REmitterBuilderExecution[Env, (svg.Element, svg.Element), R, Exec]]
  }

  @noinline private def combineWithLatestEmitter[Env, O, T, Exec <: Execution](sourceEmitter: REmitterBuilderExecution[Env, O, RModifier[Env], Exec], latestEmitter: REmitterBuilder[Env, T, RModifier[Env]]): REmitterBuilderExecution[Env, (O, T), RModifier[Env], Exec] =
    new Custom[Env, (O, T), RModifier[Env], Exec]({ sink =>
      import scala.scalajs.js

      RModifier.delay {
        var lastValue: Option[T] = None
        RModifier(
          latestEmitter.forwardTo(Observer.create[T](v => lastValue = Some(v), sink.onError)),
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


  // @noinline private def forwardToInTransform[Env, F[_] : Sink, I, O](base: REmitterBuilder[Env, I, RModifier[Env]], transformF: Observable[I] => Observable[O], sink: F[_ >: O]): RModifier[Env] = {
  //   val connectable = Observer.redirect[F, Observable, O, I](sink)(transformF)
  //   RModifier(base.forwardTo(connectable.sink), managedFunction(() => connectable.connect()))
  // }
}
