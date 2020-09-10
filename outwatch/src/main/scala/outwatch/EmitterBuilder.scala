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
// The EmitterBuilder result must be a SubscriptionOwner to handle the subscription
// from the emitterbuilder.
//


trait EmitterBuilderExecution[-Env, +O, +R[-_], +Exec <: EmitterBuilder.Execution] {

  @inline def forwardTo[F[_] : Sink](sink: F[_ >: O]): R[Env]

  // this method keeps the current Execution but actually, the caller must decide,
  // whether this really keeps the execution type or might be async. Therefore private.
  @inline private[outwatch] def transformWithExec[T](f: Observable[O] => Observable[T]): EmitterBuilderExecution[Env, T, R, Exec]
  @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[O]): EmitterBuilderExecution[Env, T, R, Exec]

  @inline final def -->[F[_] : Sink](sink: F[_ >: O]): R[Env] = forwardTo(sink)

  @inline final def discard: R[Env] = forwardTo(Observer.empty)

  @inline final def foreach(action: O => Unit): R[Env] = forwardTo(Observer.create(action))
  @inline final def foreach(action: => Unit): R[Env] = foreach(_ => action)

  @inline final def foreachSync[G[_] : RunSyncEffect](action: O => G[Unit]): R[Env] = mapSync(action).discard
  @inline final def doSync[G[_] : RunSyncEffect](action: G[Unit]): R[Env] = foreachSync(_ => action)

  @inline final def foreachAsync[G[_] : Effect](action: O => G[Unit]): R[Env] = concatMapAsync(action).discard
  @inline final def doAsync[G[_] : Effect](action: G[Unit]): R[Env] = foreachAsync(_ => action)

  final def map[T](f: O => T): EmitterBuilderExecution[Env, T, R, Exec] = transformSinkWithExec(_.contramap(f))

  final def collect[T](f: PartialFunction[O, T]): EmitterBuilderExecution[Env, T, R, Exec] = transformSinkWithExec(_.contracollect(f))

  final def filter(predicate: O => Boolean): EmitterBuilderExecution[Env, O, R, Exec] = transformSinkWithExec(_.contrafilter(predicate))

  final def mapFilter[T](f: O => Option[T]): EmitterBuilderExecution[Env, T, R, Exec] = transformSinkWithExec(_.contramapFilter(f))

  @inline final def use[T](value: T): EmitterBuilderExecution[Env, T, R, Exec] = map(_ => value)
  @inline final def useLazy[T](value: => T): EmitterBuilderExecution[Env, T, R, Exec] = map(_ => value)

  @deprecated("Use .useLazy(value) instead", "")
  @inline final def mapTo[T](value: => T): EmitterBuilderExecution[Env, T, R, Exec] = useLazy(value)
  @deprecated("Use .use(value) instead", "")
  @inline final def apply[T](value: T): EmitterBuilderExecution[Env, T, R, Exec] = use(value)

  @inline final def useSync[G[_]: RunSyncEffect, T](value: G[T]): EmitterBuilderExecution[Env, T, R, Exec] = mapSync(_ => value)

  @inline final def useAsync[G[_]: Effect, T](value: G[T]): REmitterBuilder[Env, T, R] = concatMapAsync(_ => value)

  @inline final def apply[G[_] : Source, T](source: G[T]): EmitterBuilderExecution[Env, T, R, Exec] = useLatest(source)

  final def useLatest[F[_] : Source, T](latest: F[T]): EmitterBuilderExecution[Env, T, R, Exec] =
    transformWithExec[T](source => Observable.withLatestMap(source, latest)((_, u) => u))

  final def withLatest[F[_] : Source, T](latest: F[T]): EmitterBuilderExecution[Env, (O, T), R, Exec] =
    transformWithExec[(O, T)](source => Observable.withLatest(source, latest))

  final def scan[T](seed: T)(f: (T, O) => T): EmitterBuilderExecution[Env, T, R, Exec] =
    transformWithExec[T](source => Observable.scan(source)(seed)(f))

  final def useScan[T](seed: T)(f: T => T): EmitterBuilderExecution[Env, T, R, Exec] = scan(seed)((t,_) => f(t))

  final def scan0[T](seed: T)(f: (T, O) => T): EmitterBuilderExecution[Env, T, R, Exec] =
    transformWithExec[T](source => Observable.scan0(source)(seed)(f))

  final def useScan0[T](seed: T)(f: T => T): EmitterBuilderExecution[Env, T, R, Exec] = scan0(seed)((t,_) => f(t))

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

  final def mapSync[G[_]: RunSyncEffect, T](f: O => G[T]): EmitterBuilderExecution[Env, T, R, Exec] =
    transformWithExec[T](source => Observable.mapSync(source)(f))

  final def transformLifted[F[_] : Source : LiftSource, OO >: O, T](f: F[OO] => F[T]): REmitterBuilder[Env, T, R] =
    transformWithExec[T]((s: Observable[OO]) => Observable.lift(f(s.liftSource[F])))

  final def transformLift[F[_] : Source, T](f: Observable[O] => F[T]): REmitterBuilder[Env, T, R] =
    transformWithExec[T]((s: Observable[O]) => Observable.lift(f(s)))

  // do not expose transform with current exec but just normal Emitterbuilder. This tranform might be async
  @inline final def transform[T](f: Observable[O] => Observable[T]): REmitterBuilder[Env, T, R] = transformWithExec(f)
  @inline final def transformSink[T](f: Observer[T] => Observer[O]): REmitterBuilder[Env, T, R] = transformSinkWithExec(f)

  @inline final def mapResult[SEnv, S[-_]](f: R[Env] => S[SEnv]): EmitterBuilderExecution[SEnv, O, S, Exec] = new EmitterBuilder.MapResult[Env, SEnv, O, R, S, Exec](this, f)

  @inline final def provide(env: Env): EmitterBuilderExecution[Any, O, R, Exec] = new EmitterBuilder.Provide[Env, O, R, Exec](this, env)
}

object EmitterBuilder {

  sealed trait Execution
  sealed trait SyncExecution extends Execution

  type RSync[-Env, +O, +R[-_]] = EmitterBuilderExecution[Env, O, R, SyncExecution]
  type Sync[+O, +R[-_]] = RSync[Any, O, R]

  @inline final class MapResult[-IEnv, -REnv, +O, +I[-_], +R[-_], +Exec <: Execution](base: REmitterBuilder[IEnv, O, I], mapF: I[IEnv] => R[REnv]) extends EmitterBuilderExecution[REnv, O, R, Exec] {
    @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[O]): EmitterBuilderExecution[REnv, T, R, Exec] = new MapResult(base.transformSink(f), mapF)
    @inline private[outwatch] def transformWithExec[T](f: Observable[O] => Observable[T]): EmitterBuilderExecution[REnv, T, R, Exec] = new MapResult(base.transformWithExec(f), mapF)
    @inline def forwardTo[F[_] : Sink](sink: F[_ >: O]): R[REnv] = mapF(base.forwardTo(sink))
  }

  @inline final class Empty[+R[-_]](empty: R[Any]) extends EmitterBuilderExecution[Any, Nothing, R, Nothing] {
    @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[Nothing]): EmitterBuilderExecution[Any, T, R, Nothing] = this
    @inline private[outwatch] def transformWithExec[T](f: Observable[Nothing] => Observable[T]): EmitterBuilderExecution[Any, T, R, Nothing] = this
    @inline def forwardTo[F[_] : Sink](sink: F[_ >: Nothing]): R[Any] = empty
  }

  @inline final class Stream[S[_] : Source, -Env, +O, +R[-_]](source: S[O], result: R[Env])(implicit owner: SubscriptionOwner[R[Env]]) extends EmitterBuilderExecution[Env, O, R, Execution] {
    @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[O]): EmitterBuilderExecution[Env, T, R, Execution] = new Stream(Observable.transformSink(source)(f), result)
    @inline private[outwatch] def transformWithExec[T](f: Observable[O] => Observable[T]): EmitterBuilderExecution[Env, T, R, Execution] = new Stream(f(Observable.lift(source)), result)
    @inline def forwardTo[F[_] : Sink](sink: F[_ >: O]): R[Env] = SubscriptionOwner[R[Env]].own(result)(() => Source[S].subscribe(source)(sink))
  }

  @inline final class Custom[-Env, +O, +R[-_], +Exec <: Execution](create: Observer[O] => R[Env])(implicit owner: SubscriptionOwner[R[Env]]) extends EmitterBuilderExecution[Env, O, R, Exec] {
    @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[O]): EmitterBuilderExecution[Env, T, R, Exec] = new TransformSink(this, f)
    @inline private[outwatch] def transformWithExec[T](f: Observable[O] => Observable[T]): EmitterBuilderExecution[Env, T, R, Exec] = new Transform(this, f)
    @inline def forwardTo[F[_] : Sink](sink: F[_ >: O]): R[Env] = create(Observer.lift(sink))
  }

  @inline final class TransformSink[-Env, +I, +O, +R[-_], Exec <: Execution](base: EmitterBuilderExecution[Env, I, R, Exec], transformF: Observer[O] => Observer[I])(implicit owner: SubscriptionOwner[R[Env]]) extends EmitterBuilderExecution[Env, O, R, Exec] {
    @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[O]): EmitterBuilderExecution[Env, T, R, Exec] = new TransformSink(base, s => transformF(f(s)))
    @inline private[outwatch] def transformWithExec[T](f: Observable[O] => Observable[T]): EmitterBuilderExecution[Env, T, R, Exec] = new Transform[Env, I, T, R, Exec](base, s => f(Observable.transformSink(s)(transformF)))
    @inline def forwardTo[F[_] : Sink](sink: F[_ >: O]): R[Env] = base.forwardTo(transformF(Observer.lift(sink)))
  }

  @inline final class Transform[-Env, +I, +O, +R[-_], Exec <: Execution](base: EmitterBuilderExecution[Env, I, R, Exec], transformF: Observable[I] => Observable[O])(implicit owner: SubscriptionOwner[R[Env]]) extends EmitterBuilderExecution[Env, O, R, Exec] {
    @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[O]): EmitterBuilderExecution[Env, T, R, Exec] = new Transform[Env, I, T, R, Exec](base, s => Observable.transformSink(transformF(s))(f))
    @inline private[outwatch] def transformWithExec[T](f: Observable[O] => Observable[T]): EmitterBuilderExecution[Env, T, R, Exec] = new Transform[Env, I, T, R, Exec](base, s => f(transformF(s)))
    @inline def forwardTo[F[_] : Sink](sink: F[_ >: O]): R[Env] = ??? //forwardToInTransform(base, transformF, sink)
  }

  @inline final class Access[-Env, +O, +R[-_], Exec <: Execution](base: Env => EmitterBuilderExecution[Any, O, R, Exec]) extends EmitterBuilderExecution[Env, O, R, Exec] {
    @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[O]): EmitterBuilderExecution[Env, T, R, Exec] = new Access(env => base(env).transformSinkWithExec(f))
    @inline private[outwatch] def transformWithExec[T](f: Observable[O] => Observable[T]): EmitterBuilderExecution[Env, T, R, Exec] = new Access(env => base(env).transformWithExec(f))
    @inline def forwardTo[F[_] : Sink](sink: F[_ >: O]): R[Env] = ???
  }

  @inline final class Provide[-Env, +O, +R[-_], Exec <: Execution](base: EmitterBuilderExecution[Env, O, R, Exec], env: Env) extends EmitterBuilderExecution[Any, O, R, Exec] {
    @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[O]): EmitterBuilderExecution[Any, T, R, Exec] = new Provide(base.transformSinkWithExec(f), env)
    @inline private[outwatch] def transformWithExec[T](f: Observable[O] => Observable[T]): EmitterBuilderExecution[Any, T, R, Exec] = new Provide(base.transformWithExec(f), env)
    @inline def forwardTo[F[_] : Sink](sink: F[_ >: O]): R[Any] = ???
  }

  //TODO: we requiring Monoid here, but actually just want an empty. Would allycats be better with Empty?
  @inline def emptyOf[R[-_]: MonoidK]: EmitterBuilderExecution[Any, Nothing, R, Nothing] = new Empty[R](MonoidK[R].empty[Any])

  @inline def apply[Env, E, R[-_]](create: Observer[E] => R[Env])(implicit owner: SubscriptionOwner[R[Env]]): EmitterBuilder.RSync[Env, E, R] = new Custom[Env, E, R, SyncExecution](sink => create(sink))

  @inline def fromSourceOf[F[_] : Source, E, R[-_] : MonoidK](source: F[E])(implicit owner: SubscriptionOwner[R[Any]]): EmitterBuilder[E, R] = new Stream[F, Any, E, R](source, MonoidK[R].empty[Any])

  //TODO partially apply
  @inline def access[Env, O, R[-_], Exec <: Execution](emitter: Env => EmitterBuilderExecution[Any, O, R, Exec]): EmitterBuilderExecution[Env, O, R, Exec] = new Access[Env, O, R, Exec](emitter)

  // shortcuts for modifiers with less type ascriptions
  @inline def empty: EmitterBuilderExecution[Any, Nothing, RModifier, Nothing] = emptyOf[RModifier]
  @inline def ofModifier[E](create: Observer[E] => Modifier): EmitterBuilder.Sync[E, RModifier] = ofModifierR[E, Any](create)
  @inline def ofModifierR[E, Env](create: Observer[E] => RModifier[Env]): EmitterBuilder.RSync[Env, E, RModifier] = apply[Env, E, RModifier](create)
  @inline def ofNode[E](create: Observer[E] => VNode): EmitterBuilder.Sync[E, RVNode] = ofNodeR[E, Any](create)
  @inline def ofNodeR[E, Env](create: Observer[E] => RVNode[Env]): EmitterBuilder.RSync[Env, E, RVNode] = apply[Env, E, RVNode](create)
  @inline def fromSource[F[_] : Source, E](source: F[E]): EmitterBuilder[E, RModifier] = fromSourceOf[F, E, RModifier](source)

  def fromEvent[E <: Event](eventType: String): EmitterBuilder.Sync[E, RModifier] = apply[Any, E, RModifier] { sink =>
    Emitter(eventType, e => sink.onNext(e.asInstanceOf[E]))
  }

  @inline def combine[Env, T, R[-_] : MonoidK, Exec <: Execution](builders: EmitterBuilderExecution[Env, T, R, Exec]*)(implicit owner: SubscriptionOwner[R[Env]]): EmitterBuilderExecution[Env, T, R, Exec] = combineSeq(builders)

  def combineSeq[Env, T, R[-_] : MonoidK, Exec <: Execution](builders: Seq[EmitterBuilderExecution[Env, T, R, Exec]])(implicit owner: SubscriptionOwner[R[Env]]): EmitterBuilderExecution[Env, T, R, Exec] = new Custom[Env, T, R, Exec](sink =>
    builders.foldLeft(MonoidK[R].empty[Env])((a,b) => MonoidK[R].combineK[Env](a, b.forwardTo(sink)))
  )

  @deprecated("Use EmitterBuilder.fromEvent[E] instead", "0.11.0")
  @inline def apply[E <: Event](eventType: String): EmitterBuilder.Sync[E, RModifier] = fromEvent[E](eventType)
  @deprecated("Use EmitterBuilder[E, O] instead", "0.11.0")
  @inline def custom[Env, E, R[-_]](create: Observer[E] => R[Env])(implicit owner: SubscriptionOwner[R[Env]]): EmitterBuilder.RSync[Env, E, R] = apply[Env, E, R](create)

  implicit def monoid[Env, T, R[-_] : MonoidK, Exec <: Execution](implicit owner: SubscriptionOwner[R[Env]]): Monoid[EmitterBuilderExecution[Env, T, R, Exec]] = new Monoid[EmitterBuilderExecution[Env, T, R, Exec]] {
    def empty: EmitterBuilderExecution[Env, T, R, Exec] = EmitterBuilder.emptyOf[R]
    def combine(x: EmitterBuilderExecution[Env, T, R, Exec], y: EmitterBuilderExecution[Env, T, R, Exec]): EmitterBuilderExecution[Env, T, R, Exec] = EmitterBuilder.combine(x, y)
  }

//  implicit def functor[Env, R[-_]]: Functor[EmitterBuilder[Env, ?, R]] = new Functor[EmitterBuilder[Env, ?, R]] {
//    def map[A, B](fa: REmitterBuilder[Env, A, R])(f: A => B): REmitterBuilder[B, R] = fa.map(f)
//  }

//  //TODO: REmitterBuilder
//  implicit object bifunctor extends Bifunctor[EmitterBuilder] {
//    def bimap[A, B, C, D](fab: EmitterBuilder[A, B])(f: A => C, g: B => D): EmitterBuilder[C, D] = fab.map(f).mapResult(g)
//  }

//  @inline implicit class HandlerIntegrationMonoid[Env, O, R[-_], Exec <: Execution](builder: EmitterBuilderExecution[Env, O, R, Exec])(implicit monoid: Monoid[R[Env]]) {
//    @inline def handled(f: Observable[O] => R): SyncIO[R] = handledF[SyncIO](f)

//    @inline def handledF[F[_] : SyncCats](f: Observable[O] => R): F[R] = Functor[F].map(handler.Handler.createF[F, O]) { handler =>
//      Monoid[R[Env]].combine(builder.forwardTo(handler), f(handler))
//    }
//  }

//  @inline implicit class HandlerIntegration[Env, O, R[-_], Exec <: Execution](builder: EmitterBuilderExecution[Env, O, R, Exec]) {
//    @inline def handledWith(f: (R, Observable[O]) => R): SyncIO[R] = handledWithF[SyncIO](f)

//    @inline def handledWithF[F[_] : SyncCats](f: (R, Observable[O]) => R): F[R] = Functor[F].map(handler.Handler.createF[F, O]) { handler =>
//      f(builder.forwardTo(handler), handler)
//    }
//  }

  // @inline implicit class EmitterOperations[O, R : Monoid : SubscriptionOwner, Exec <: Execution](builder: EmitterBuilderExecution[O, R, Exec]) {

  //   @inline def withLatestEmitter[T](emitter: EmitterBuilder[T, R]): EmitterBuilderExecution[(O,T), SyncIO[R], Exec] = combineWithLatestEmitter(builder, emitter)

  //   @inline def useLatestEmitter[T](emitter: EmitterBuilder[T, R]): EmitterBuilderExecution[T, SyncIO[R], Exec] = combineWithLatestEmitter(builder, emitter).map(_._2)
  // }

  // @inline implicit class EventActions[O <: Event, R](val builder: EmitterBuilder.Sync[O, R]) extends AnyVal {
  //   def onlyOwnEvents: EmitterBuilder[O, R] = builder.filter(ev => ev.currentTarget == ev.target)
  //   def preventDefault: EmitterBuilder.Sync[O, R] = builder.map { e => e.preventDefault; e }
  //   def stopPropagation: EmitterBuilder.Sync[O, R] = builder.map { e => e.stopPropagation; e }
  // }

  // @inline implicit class TargetAsInput[O <: Event, R](builder: EmitterBuilder.Sync[O, R]) {
  //   object target {
  //     @inline def value: EmitterBuilder.Sync[String, R] = builder.map(_.target.asInstanceOf[html.Input].value)
  //     @inline def valueAsNumber: EmitterBuilder.Sync[Double, R] = builder.map(_.target.asInstanceOf[html.Input].valueAsNumber)
  //     @inline def checked: EmitterBuilder.Sync[Boolean, R] = builder.map(_.target.asInstanceOf[html.Input].checked)
  //   }
  // }

  // @inline implicit class CurrentTargetAsInput[O <: Event, R](val builder: EmitterBuilder.Sync[O, R]) extends AnyVal {
  //   def value: EmitterBuilder.Sync[String, R] = builder.map(e => e.currentTarget.asInstanceOf[html.Input].value)
  //   def valueAsNumber: EmitterBuilder.Sync[Double, R] = builder.map(e => e.currentTarget.asInstanceOf[html.Input].valueAsNumber)
  //   def checked: EmitterBuilder.Sync[Boolean, R] = builder.map(e => e.currentTarget.asInstanceOf[html.Input].checked)
  // }

  // @inline implicit class CurrentTargetAsElement[O <: Event, R](val builder: EmitterBuilder.Sync[O, R]) extends AnyVal {
  //   def asHtml: EmitterBuilder.Sync[html.Element, R] = builder.map(_.currentTarget.asInstanceOf[html.Element])
  //   def asSvg: EmitterBuilder.Sync[svg.Element, R] = builder.map(_.currentTarget.asInstanceOf[svg.Element])
  //   def asElement: EmitterBuilder.Sync[Element, R] = builder.map(_.currentTarget.asInstanceOf[Element])
  // }

  // @inline implicit class TypedElements[O <: Element, R](val builder: EmitterBuilder.Sync[O, R]) extends AnyVal {
  //   @inline def asHtml: EmitterBuilder.Sync[html.Element, R] = builder.asInstanceOf[EmitterBuilder.Sync[html.Element, R]]
  //   @inline def asSvg: EmitterBuilder.Sync[svg.Element, R] = builder.asInstanceOf[EmitterBuilder.Sync[svg.Element, R]]
  // }

  // @inline implicit class TypedElementTuples[E <: Element, R](val builder: EmitterBuilder.Sync[(E,E), R]) extends AnyVal {
  //   @inline def asHtml: EmitterBuilder.Sync[(html.Element, html.Element), R] = builder.asInstanceOf[EmitterBuilder.Sync[(html.Element, html.Element), R]]
  //   @inline def asSvg: EmitterBuilder.Sync[(svg.Element, svg.Element), R] = builder.asInstanceOf[EmitterBuilder.Sync[(svg.Element, svg.Element), R]]
  // }

  // @noinline private def combineWithLatestEmitter[O, T, R : Monoid : SubscriptionOwner, Exec <: Execution](sourceEmitter: EmitterBuilderExecution[O, R, Exec], latestEmitter: EmitterBuilder[T, R]): EmitterBuilderExecution[(O, T), SyncIO[R], Exec] =
  //   new Custom[(O, T), SyncIO[R], Exec]({ sink =>
  //     import scala.scalajs.js

  //     SyncIO {
  //       var lastValue: js.UndefOr[T] = js.undefined
  //       Monoid[R].combine(
  //         latestEmitter.forwardTo(Observer.create[T](lastValue = _, sink.onError)),
  //         sourceEmitter.forwardTo(Observer.create[O](
  //           { o =>
  //             lastValue.foreach { t =>
  //               sink.onNext((o, t))
  //             }
  //           },
  //           sink.onError
  //         ))
  //       )
  //     }
  //   })

  // @noinline private def forwardToInTransform[F[_] : Sink, I, O, R: SubscriptionOwner](base: EmitterBuilder[I, R], transformF: Observable[I] => Observable[O], sink: F[_ >: O]): R = {
  //   val connectable = Observer.redirect[F, Observable, O, I](sink)(transformF)
  //   SubscriptionOwner[R].own(base.forwardTo(connectable.sink))(() => connectable.connect())
  // }
}
