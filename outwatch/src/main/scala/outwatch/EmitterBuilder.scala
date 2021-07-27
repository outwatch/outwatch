package outwatch

import cats.{Monoid, Functor}
import cats.effect.{Effect, Sync => SyncCats, SyncIO}
import org.scalajs.dom
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
// onClick.mapResult(emitter => Modifier(emitter, ???)): EmitterBuilder[Int, Modifier]
//
// Now you have conbined the emitter with another Modifier, so the combined modifier
// will later be rendered instead of only the emitter. Then you can describe the action
// that should be done when an event triggers:
//
// onClick.map(_ => 1).foreach(doSomething(_)): Modifier
//

trait EmitterBuilderExec[+O, +R, +Exec <: EmitterBuilderExec.Execution] {

  @inline def forwardTo[F[_] : Sink](sink: F[_ >: O]): R

  // this method keeps the current Execution but actually, the caller must decide,
  // whether this really keeps the execution type or might be async. Therefore private.
  @inline private[outwatch] def transformWithExec[T](f: Observable[O] => Observable[T]): EmitterBuilderExec[T, R, Exec]
  @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[O]): EmitterBuilderExec[T, R, Exec]

  @inline final def -->[F[_] : Sink](sink: F[_ >: O]): R = forwardTo(sink)

  @inline final def via[F[_] : Sink](sink: F[_ >: O]): EmitterBuilderExec[O, R, Exec] = transformSinkWithExec[O](_.combine(Observer.lift(sink): Observer[O]))

  @inline final def discard: R = forwardTo(Observer.empty)

  @inline final def foreach(action: O => Unit): R = forwardTo(Observer.create(action))
  @inline final def foreach(action: => Unit): R = foreach(_ => action)

  @inline final def foreachSync[G[_] : RunSyncEffect](action: O => G[Unit]): R = mapSync(action).discard
  @inline final def doSync[G[_] : RunSyncEffect](action: G[Unit]): R = foreachSync(_ => action)

  @inline final def foreachAsync[G[_] : Effect](action: O => G[Unit]): R = mapAsync(action).discard
  @inline final def doAsync[G[_] : Effect](action: G[Unit]): R = foreachAsync(_ => action)

  @inline final def foreachAsyncSingleOrDrop[G[_] : Effect](action: O => G[Unit]): R = mapAsyncSingleOrDrop(action).discard
  @inline final def doAsyncSingleOrDrop[G[_] : Effect](action: G[Unit]): R = foreachAsyncSingleOrDrop(_ => action)

  @inline final def map[T](f: O => T): EmitterBuilderExec[T, R, Exec] = transformSinkWithExec(_.contramap(f))

  @inline final def collect[T](f: PartialFunction[O, T]): EmitterBuilderExec[T, R, Exec] = transformSinkWithExec(_.contracollect(f))

  @inline final def filter(predicate: O => Boolean): EmitterBuilderExec[O, R, Exec] = transformSinkWithExec(_.contrafilter(predicate))

  @inline final def mapFilter[T](f: O => Option[T]): EmitterBuilderExec[T, R, Exec] = transformSinkWithExec(_.contramapFilter(f))

  @inline final def mapIterable[T](f: O => Iterable[T]): EmitterBuilderExec[T, R, Exec] = transformSinkWithExec(_.contramapIterable(f))

  @inline final def use[T](value: T): EmitterBuilderExec[T, R, Exec] = map(_ => value)
  @inline final def useLazy[T](value: => T): EmitterBuilderExec[T, R, Exec] = map(_ => value)

  @deprecated("Use .useLazy(value) instead", "")
  @inline final def mapTo[T](value: => T): EmitterBuilderExec[T, R, Exec] = useLazy(value)
  @deprecated("Use .use(value) instead", "")
  @inline final def apply[T](value: T): EmitterBuilderExec[T, R, Exec] = use(value)
  @deprecated("Use .mapFuture(f) instead", "")
  @inline final def concatMapFuture[T](f: O => Future[T])(implicit ec: ExecutionContext): EmitterBuilder[T, R] = mapFuture(f)
  @deprecated("Use .mapAsync(f) instead", "")
  @inline final def concatMapAsync[G[_]: Effect, T](f: O => G[T]): EmitterBuilder[T, R] = mapAsync(f)

  @inline final def useSync[G[_]: RunSyncEffect, T](value: G[T]): EmitterBuilderExec[T, R, Exec] = mapSync(_ => value)

  @inline final def useAsync[G[_]: Effect, T](value: G[T]): EmitterBuilder[T, R] = mapAsync(_ => value)

  @inline final def useFuture[T](value: => Future[T])(implicit ec: ExecutionContext): EmitterBuilder[T, R] = mapFuture(_ => value)

  @inline final def useAsyncSingleOrDrop[G[_]: Effect, T](value: G[T]): EmitterBuilder[T, R] = mapAsyncSingleOrDrop(_ => value)

  @inline final def useFutureSingleOrDrop[T](value: => Future[T])(implicit ec: ExecutionContext): EmitterBuilder[T, R] = mapFutureSingleOrDrop(_ => value)

  @inline final def apply[G[_] : Source, T](source: G[T]): EmitterBuilderExec[T, R, Exec] = useLatest(source)

  @inline final def useLatest[F[_] : Source, T](latest: F[T]): EmitterBuilderExec[T, R, Exec] =
    transformWithExec[T](source => Observable.withLatestMap(source, latest)((_, u) => u))

  @inline final def withLatest[F[_] : Source, T](latest: F[T]): EmitterBuilderExec[(O, T), R, Exec] =
    transformWithExec[(O, T)](source => Observable.withLatest(source, latest))

  @inline final def scan[T](seed: T)(f: (T, O) => T): EmitterBuilderExec[T, R, Exec] =
    transformWithExec[T](source => Observable.scan(source)(seed)(f))

  @inline final def useScan[T](seed: T)(f: T => T): EmitterBuilderExec[T, R, Exec] = scan(seed)((t,_) => f(t))

  @inline final def scan0[T](seed: T)(f: (T, O) => T): EmitterBuilderExec[T, R, Exec] =
    transformWithExec[T](source => Observable.scan0(source)(seed)(f))

  @inline final def useScan0[T](seed: T)(f: T => T): EmitterBuilderExec[T, R, Exec] = scan0(seed)((t,_) => f(t))

  @inline final def debounce(duration: FiniteDuration): EmitterBuilder[O, R] =
    transformWithExec[O](source => Observable.debounce(source)(duration))

  @inline final def debounceMillis(millis: Int): EmitterBuilder[O, R] =
    transformWithExec[O](source => Observable.debounceMillis(source)(millis))

  @inline final def async: EmitterBuilder[O, R] =
    transformWithExec[O](source => Observable.async(source))

  @inline final def delay(duration: FiniteDuration): EmitterBuilder[O, R] =
    transformWithExec[O](source => Observable.delay(source)(duration))

  @inline final def delayMillis(millis: Int): EmitterBuilder[O, R] =
    transformWithExec[O](source => Observable.delayMillis(source)(millis))

  @inline final def mapFuture[T](f: O => Future[T])(implicit ec: ExecutionContext): EmitterBuilder[T, R] =
    transformWithExec[T](source => Observable.mapFuture(source)(f))

  @inline final def mapAsync[G[_]: Effect, T](f: O => G[T]): EmitterBuilder[T, R] =
    transformWithExec[T](source => Observable.mapAsync(source)(f))

  @inline final def mapFutureSingleOrDrop[T](f: O => Future[T])(implicit ec: ExecutionContext): EmitterBuilder[T, R] =
    transformWithExec[T](source => Observable.mapFutureSingleOrDrop(source)(f))

  @inline final def mapAsyncSingleOrDrop[G[_]: Effect, T](f: O => G[T]): EmitterBuilder[T, R] =
    transformWithExec[T](source => Observable.mapAsyncSingleOrDrop(source)(f))

  @inline final def mapSync[G[_]: RunSyncEffect, T](f: O => G[T]): EmitterBuilderExec[T, R, Exec] =
    transformWithExec[T](source => Observable.mapSync(source)(f))

  @deprecated("Use transform instead", "1.0.0")
  @inline final def transformLifted[F[_] : Source : LiftSource, OO >: O, T](f: F[OO] => F[T]): EmitterBuilder[T, R] =
    transformWithExec[T]((s: Observable[OO]) => Observable.lift(f(s.liftSource[F])))

  @deprecated("Use transform instead", "1.0.0")
  @inline final def transformLift[F[_] : Source, T](f: Observable[O] => F[T]): EmitterBuilder[T, R] =
    transformWithExec[T]((s: Observable[O]) => Observable.lift(f(s)))

  // do not expose transform with current exec but just normal Emitterbuilder. This tranform might be async
  @inline final def transform[T](f: Observable[O] => Observable[T]): EmitterBuilder[T, R] = transformWithExec(f)
  @inline final def transformSink[T](f: Observer[T] => Observer[O]): EmitterBuilder[T, R] = transformSinkWithExec(f)

  @inline final def mapResult[S](f: R => S): EmitterBuilderExec[O, S, Exec] = new EmitterBuilderExec.MapResult[O, R, S, Exec](this, f)

  @inline final def dispatchWith(dispatcher: EventDispatcher[O]): R = transform[Any](dispatcher.dispatch).discard
}

object EmitterBuilderExec {

  sealed trait Execution
  sealed trait SyncExecution extends Execution

  @inline final object Empty extends EmitterBuilderExec[Nothing, Modifier, Nothing] {
    @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[Nothing]): EmitterBuilderExec[T, Modifier, Nothing] = this
    @inline private[outwatch] def transformWithExec[T](f: Observable[Nothing] => Observable[T]): EmitterBuilderExec[T, Modifier, Nothing] = this
    @inline def forwardTo[F[_] : Sink](sink: F[_ >: Nothing]): Modifier = Modifier.empty
  }

  @inline final class Stream[S[_] : Source, +O, +Exec <: Execution](source: S[O]) extends EmitterBuilderExec[O, Modifier, Exec] {
    @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[O]): EmitterBuilderExec[T, Modifier, Exec] = new Stream(Observable.transformSink(source)(f))
    @inline private[outwatch] def transformWithExec[T](f: Observable[O] => Observable[T]): EmitterBuilderExec[T, Modifier, Exec] = new Stream(f(Observable.lift(source)))
    @inline def forwardTo[F[_] : Sink](sink: F[_ >: O]): Modifier = Modifier.managedFunction(() => Source[S].subscribe(source)(sink))
  }

  @inline final class MapResult[+O, +I, +R, +Exec <: Execution](base: EmitterBuilderExec[O, I, Exec], mapF: I => R) extends EmitterBuilderExec[O, R, Exec] {
    @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[O]): EmitterBuilderExec[T, R, Exec] = new MapResult(base.transformSinkWithExec(f), mapF)
    @inline private[outwatch] def transformWithExec[T](f: Observable[O] => Observable[T]): EmitterBuilderExec[T, R, Exec] = new MapResult(base.transformWithExec(f), mapF)
    @inline def forwardTo[F[_] : Sink](sink: F[_ >: O]): R = mapF(base.forwardTo(sink))
  }

  @inline final class Custom[+O, +R : SubscriptionOwner, +Exec <: Execution](create: Observer[O] => R) extends EmitterBuilderExec[O, R, Exec] {
    @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[O]): EmitterBuilderExec[T, R, Exec] = new TransformSink(this, f)
    @inline private[outwatch] def transformWithExec[T](f: Observable[O] => Observable[T]): EmitterBuilderExec[T, R, Exec] = new Transform(this, f)
    @inline def forwardTo[F[_] : Sink](sink: F[_ >: O]): R = create(Observer.lift(sink))
  }

  @inline final class TransformSink[+I, +O, +R : SubscriptionOwner, Exec <: Execution](base: EmitterBuilderExec[I, R, Exec], transformF: Observer[O] => Observer[I]) extends EmitterBuilderExec[O, R, Exec] {
    @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[O]): EmitterBuilderExec[T, R, Exec] = new TransformSink(base, f andThen transformF)
    @inline private[outwatch] def transformWithExec[T](f: Observable[O] => Observable[T]): EmitterBuilderExec[T, R, Exec] = new Transform[I, T, R, Exec](base, s => f(Observable.transformSink(s)(transformF)))
    @inline def forwardTo[F[_] : Sink](sink: F[_ >: O]): R = base.forwardTo(transformF(Observer.lift(sink)))
  }

  @inline final class Transform[+I, +O, +R : SubscriptionOwner, Exec <: Execution](base: EmitterBuilderExec[I, R, Exec], transformF: Observable[I] => Observable[O]) extends EmitterBuilderExec[O, R, Exec] {
    @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[O]): EmitterBuilderExec[T, R, Exec] = new Transform[I, T, R, Exec](base, s => Observable.transformSink(transformF(s))(f))
    @inline private[outwatch] def transformWithExec[T](f: Observable[O] => Observable[T]): EmitterBuilderExec[T, R, Exec] = new Transform[I, T, R, Exec](base, transformF andThen f)
    @inline def forwardTo[F[_] : Sink](sink: F[_ >: O]): R = forwardToInTransform(base, transformF, sink)
  }

  @inline final class Access[-Env, +O, R[-_], Exec <: Execution](base: Env => EmitterBuilderExec[O, R[Any], Exec])(implicit acc: AccessEnvironment[R]) extends EmitterBuilderExec[O, R[Env], Exec] {
    @inline private[outwatch] def transformSinkWithExec[T](f: Observer[T] => Observer[O]): EmitterBuilderExec[T, R[Env], Exec] = new Access(env => base(env).transformSinkWithExec(f))
    @inline private[outwatch] def transformWithExec[T](f: Observable[O] => Observable[T]): EmitterBuilderExec[T, R[Env], Exec] = new Access(env => base(env).transformWithExec(f))
    @inline def forwardTo[F[_] : Sink](sink: F[_ >: O]): R[Env] = AccessEnvironment[R].access(env => base(env).forwardTo(sink))
  }

  @inline implicit def monoid[T, Exec <: Execution]: Monoid[EmitterBuilderExec[T, Modifier, Exec]] = new EmitterBuilderMonoid[T, Exec]
  @inline final class EmitterBuilderMonoid[T, Exec <: Execution] extends Monoid[EmitterBuilderExec[T, Modifier, Exec]] {
    @inline def empty: EmitterBuilderExec[T, Modifier, Exec] = EmitterBuilder.empty
    @inline def combine(x: EmitterBuilderExec[T, Modifier, Exec], y: EmitterBuilderExec[T, Modifier, Exec]): EmitterBuilderExec[T, Modifier, Exec] = EmitterBuilder.combine(x, y)
    // @inline override def combineAll(x: Iterable[EmitterBuilderExec[T, Modifier, Exec]]): EmitterBuilderExec[T, Modifier, Exec] = EmitterBuilder.combineAll(x)
  }

  @inline implicit def functor[R, Exec <: Execution]: Functor[EmitterBuilderExec[*, R, Exec]] = new EmitterBuilderFunctor[R, Exec]
  @inline final class EmitterBuilderFunctor[R, Exec <: Execution] extends Functor[EmitterBuilderExec[*, R, Exec]] {
    @inline def map[A, B](fa: EmitterBuilderExec[A, R, Exec])(f: A => B): EmitterBuilderExec[B, R, Exec] = fa.map(f)
  }

  @inline implicit final class ModifierOperations[Env, O, Exec <: Execution](val builder: EmitterBuilderExec[O, ModifierM[Env], Exec]) extends AnyVal {
    @inline def handled[R](f: Observable[O] => ModifierM[R]): SyncIO[ModifierM[Env with R]] = handledF[SyncIO, R](f)
    @inline def handledF[F[_] : SyncCats, R](f: Observable[O] => ModifierM[R]): F[ModifierM[Env with R]] = handledWithF[F, Env with R]((r, o) => ModifierM(r, f(o)))
    @inline def handledWith[R](f: (ModifierM[Env], Observable[O]) => ModifierM[R]): SyncIO[ModifierM[R]] = handledWithF[SyncIO, R](f)
    @inline def handledWithF[F[_] : SyncCats, R](f: (ModifierM[Env], Observable[O]) => ModifierM[R]): F[ModifierM[R]] = Functor[F].map(handler.Handler.createF[F, O]) { handler =>
      f(builder.forwardTo(handler), handler)
    }

    @inline def withLatestEmitter[T](emitter: EmitterBuilder[T, ModifierM[Env]]): EmitterBuilderExec[(O,T), ModifierM[Env], Exec] = combineWithLatestEmitter(builder, emitter)
    @inline def useLatestEmitter[T](emitter: EmitterBuilder[T, ModifierM[Env]]): EmitterBuilderExec[T, ModifierM[Env], Exec] = withLatestEmitter(emitter).map(_._2)
  }

  @inline implicit final class AccessEnvironmentOperations[Env, O, R[-_], Exec <: Execution](val builder: EmitterBuilderExec[O, R[Env], Exec])(implicit acc: AccessEnvironment[R]) {
    @inline def provide(env: Env): EmitterBuilderExec[O, R[Any], Exec] = builder.mapResult(r => AccessEnvironment[R].provide(r)(env))
    @inline def provideSome[REnv](map: REnv => Env): EmitterBuilderExec[O, R[REnv], Exec] = builder.mapResult(r => AccessEnvironment[R].provideSome(r)(map))

    @inline def useAccess[REnv]: EmitterBuilderExec[REnv, R[Env with REnv], Exec] = EmitterBuilder.accessM[REnv](builder.use)
    @inline def withAccess[REnv]: EmitterBuilderExec[(O, REnv), R[Env with REnv], Exec] = EmitterBuilder.accessM[REnv](env => builder.map(_ -> env))
  }

  @inline implicit final class EventActions[O <: dom.Event, R](val builder: EmitterBuilder.Sync[O, R]) extends AnyVal {
    @inline def asElement: EmitterBuilder.Sync[dom.Element, R] = builder.map(_.currentTarget.asInstanceOf[dom.Element])
    @inline def asHtml: EmitterBuilder.Sync[dom.html.Element, R] = builder.map(_.currentTarget.asInstanceOf[dom.html.Element])
    @inline def asSvg: EmitterBuilder.Sync[dom.svg.Element, R] = builder.map(_.currentTarget.asInstanceOf[dom.svg.Element])

    @inline def onlyOwnEvents: EmitterBuilder.Sync[O, R] = builder.filter(ev => ev.currentTarget == ev.target)
    @inline def preventDefault: EmitterBuilder.Sync[O, R] = builder.map { e => e.preventDefault(); e }
    @inline def stopPropagation: EmitterBuilder.Sync[O, R] = builder.map { e => e.stopPropagation(); e }

    @inline def value: EmitterBuilder.Sync[String, R] = builder.map(e => e.currentTarget.asInstanceOf[dom.html.Input].value)
    @inline def valueAsNumber: EmitterBuilder.Sync[Double, R] = builder.map(e => e.currentTarget.asInstanceOf[dom.html.Input].valueAsNumber)
    @inline def checked: EmitterBuilder.Sync[Boolean, R] = builder.map(e => e.currentTarget.asInstanceOf[dom.html.Input].checked)

    @inline def target: EventActionsTargetOps[O, R] = new EventActionsTargetOps(builder)
  }

  @inline final class EventActionsTargetOps[O <: dom.Event, R](val builder: EmitterBuilder.Sync[O, R]) extends AnyVal {
    @inline def value: EmitterBuilder.Sync[String, R] = builder.map(_.target.asInstanceOf[dom.html.Input].value)
    @inline def valueAsNumber: EmitterBuilder.Sync[Double, R] = builder.map(_.target.asInstanceOf[dom.html.Input].valueAsNumber)
    @inline def checked: EmitterBuilder.Sync[Boolean, R] = builder.map(_.target.asInstanceOf[dom.html.Input].checked)
  }

  @inline implicit final class TypedElements[O <: dom.Element, R, Exec <: Execution](val builder: EmitterBuilderExec[O, R, Exec]) extends AnyVal {
    @inline def asHtml: EmitterBuilderExec[dom.html.Element, R, Exec] = builder.asInstanceOf[EmitterBuilderExec[dom.html.Element, R, Exec]]
    @inline def asSvg: EmitterBuilderExec[dom.svg.Element, R, Exec] = builder.asInstanceOf[EmitterBuilderExec[dom.svg.Element, R, Exec]]
  }

  @inline implicit final class TypedElementTuples[E <: dom.Element, R, Exec <: Execution](val builder: EmitterBuilderExec[(E,E), R, Exec]) extends AnyVal {
    @inline def asHtml: EmitterBuilderExec[(dom.html.Element, dom.html.Element), R, Exec] = builder.asInstanceOf[EmitterBuilderExec[(dom.html.Element, dom.html.Element), R, Exec]]
    @inline def asSvg: EmitterBuilderExec[(dom.svg.Element, dom.svg.Element), R, Exec] = builder.asInstanceOf[EmitterBuilderExec[(dom.svg.Element, dom.svg.Element), R, Exec]]
  }

  @noinline private def combineWithLatestEmitter[Env, O, T, Exec <: Execution](sourceEmitter: EmitterBuilderExec[O, ModifierM[Env], Exec], latestEmitter: EmitterBuilder[T, ModifierM[Env]]): EmitterBuilderExec[(O, T), ModifierM[Env], Exec] =
    new Custom[(O, T), ModifierM[Env], Exec]({ sink =>
      ModifierM.delay {
        var lastValue: Option[T] = None
        ModifierM(
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


  @noinline private def forwardToInTransform[F[_] : Sink, I, O, R : SubscriptionOwner](base: EmitterBuilder[I, R], transformF: Observable[I] => Observable[O], sink: F[_ >: O]): R = {
    val connectable = Observer.redirect[F, Observable, O, I](sink)(transformF)
    SubscriptionOwner[R].own(base.forwardTo(connectable.sink))(connectable.connect)
  }
}

object EmitterBuilder {
  import EmitterBuilderExec._

  type Sync[+O, +R] = EmitterBuilderExec[O, R, SyncExecution]

  @inline final def empty: EmitterBuilderExec[Nothing, Modifier, Nothing] = Empty
  @inline final def fromSource[F[_] : Source, E](source: F[E]): EmitterBuilder[E, Modifier] = new Stream(source)

  final def fromEvent[E <: dom.Event](eventType: String): EmitterBuilder.Sync[E, Modifier] = EmitterBuilder[E, Modifier] { sink =>
    Emitter(eventType, e => sink.onNext(e.asInstanceOf[E]))
  }

  @inline def apply[E, R : SubscriptionOwner](create: Observer[E] => R): EmitterBuilder.Sync[E, R] = new Custom[E, R, SyncExecution](sink => create(sink))
  @inline def ofModifier[E](create: Observer[E] => Modifier): EmitterBuilder.Sync[E, Modifier] = ofModifierM[Any, E](create)
  @inline def ofVNode[E](create: Observer[E] => VNode): EmitterBuilder.Sync[E, VNode] = ofVNodeM[Any, E](create)
  @inline def ofModifierM[Env, E](create: Observer[E] => ModifierM[Env]): EmitterBuilder.Sync[E, ModifierM[Env]] = apply[E, ModifierM[Env]](create)
  @inline def ofVNodeM[Env, E](create: Observer[E] => VNodeM[Env]): EmitterBuilder.Sync[E, VNodeM[Env]] = apply[E, VNodeM[Env]](create)

  @inline def combine[T, R : Monoid : SubscriptionOwner, Exec <: Execution](builders: EmitterBuilderExec[T, R, Exec]*): EmitterBuilderExec[T, R, Exec] = combineAll(builders)
  def combineAll[T, R : Monoid : SubscriptionOwner, Exec <: Execution](builders: Iterable[EmitterBuilderExec[T, R, Exec]]): EmitterBuilderExec[T, R, Exec] = new Custom[T, R, Exec](sink =>
    Monoid[R].combineAll(builders.iterator.map(_ --> sink))
  )

  @deprecated("Use EmitterBuilder.fromEvent[E] instead", "0.11.0")
  @inline def apply[E <: dom.Event](eventType: String): EmitterBuilder.Sync[E, Modifier] = fromEvent[E](eventType)
  @deprecated("Use EmitterBuilder[E, O] instead", "0.11.0")
  @inline def custom[E, R : SubscriptionOwner](create: Observer[E] => R): EmitterBuilder.Sync[E, R] = apply(create)
  @deprecated("Use EmitterBuilder.ofVNode[E] instead", "1.0.0")
  @inline def ofNode[E](create: Observer[E] => VNode): EmitterBuilder.Sync[E, VNode] = ofVNode[E](create)

  @inline def access[Env] = new PartiallyAppliedAccess[Env]
  @inline class PartiallyAppliedAccess[Env] {
    @inline def apply[O, T[-_], Exec <: Execution](emitter: Env => EmitterBuilderExec[O, T[Any], Exec])(implicit acc: AccessEnvironment[T]): EmitterBuilderExec[O, T[Env], Exec] = new Access(env => emitter(env))
  }
  @inline def accessM[Env] = new PartiallyAppliedAccessM[Env]
  @inline class PartiallyAppliedAccessM[Env] {
    @inline def apply[R, O, T[-_], Exec <: Execution](emitter: Env => EmitterBuilderExec[O, T[R], Exec])(implicit acc: AccessEnvironment[T]): EmitterBuilderExec[O, T[Env with R], Exec] = access[Env with R][O, T, Exec](env => emitter(env).provide(env))
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
