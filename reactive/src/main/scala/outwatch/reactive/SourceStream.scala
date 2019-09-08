package outwatch.reactive

import outwatch.effect._

import cats.{ MonoidK, Functor, FunctorFilter, Eq }
import cats.effect.{ Effect, IO }

import scala.scalajs.js
import scala.util.{ Success, Failure, Try }
import scala.util.control.NonFatal
import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.concurrent.duration.FiniteDuration

trait SourceStream[+A] {
  def subscribe[G[_]: Sink](sink: G[_ >: A]): Subscription

  @inline final def subscribe(): Subscription = subscribe(SinkObserver.empty)

  @inline final def foreach(f: A => Unit): Subscription = subscribe(SinkObserver.create(f))
}
object SourceStream {

  // Only one execution context in javascript that is a queued execution
  // context using the javascript event loop. We skip the implicit execution
  // context and just fire on the global one. As it is most likely what you
  // want to do in this API.
  import ExecutionContext.Implicits.global

  object Empty extends SourceStream[Nothing] {
    @inline def subscribe[G[_]: Sink](sink: G[_ >: Nothing]): Subscription = Subscription.empty
  }

  @inline def empty = Empty

  def apply[T](value: T): SourceStream[T] = new SourceStream[T] {
    def subscribe[G[_]: Sink](sink: G[_ >: T]): Subscription = {
      Sink[G].onNext(sink)(value)
      Subscription.empty
    }
  }

  def fromIterable[T](values: Iterable[T]): SourceStream[T] = new SourceStream[T] {
    def subscribe[G[_]: Sink](sink: G[_ >: T]): Subscription = {
      values.foreach(Sink[G].onNext(sink))
      Subscription.empty
    }
  }

  @inline def lift[F[_]: Source, A](source: F[A]): SourceStream[A] = create(Source[F].subscribe(source))

  @inline def create[A](produce: SinkObserver[A] => Subscription): SourceStream[A] = createLift[SinkObserver, A](produce)

  def createLift[F[_]: Sink: LiftSink, A](produce: F[_ >: A] => Subscription): SourceStream[A] = new SourceStream[A] {
    def subscribe[G[_]: Sink](sink: G[_ >: A]): Subscription = produce(LiftSink[F].lift(sink))
  }

  def fromEffect[F[_]: Effect, A](effect: F[A]): SourceStream[A] = new SourceStream[A] {
    def subscribe[G[_]: Sink](sink: G[_ >: A]): Subscription = {
      //TODO: proper cancel effects?
      var isCancel = false

      Effect[F].runAsync(effect)(either => IO {
        if (!isCancel) either match {
          case Right(value) => Sink[G].onNext(sink)(value)
          case Left(error)  => Sink[G].onError(sink)(error)
        }
      }).unsafeRunSync()

      Subscription(() => isCancel = true)
    }
  }

  def fromFuture[A](future: Future[A]): SourceStream[A] = new SourceStream[A] {
    def subscribe[G[_]: Sink](sink: G[_ >: A]): Subscription = {
      var isCancel = false

      future.onComplete { either =>
        if (!isCancel) either match {
          case Success(value) => Sink[G].onNext(sink)(value)
          case Failure(error) => Sink[G].onError(sink)(error)
        }
      }

      Subscription(() => isCancel = true)
    }
  }

  def interval(delay: FiniteDuration): SourceStream[Long] = intervalMillis(delay.toMillis.toInt)

  def intervalMillis(delay: Int): SourceStream[Long] = new SourceStream[Long] {
    def subscribe[G[_]: Sink](sink: G[_ >: Long]): Subscription = {
      import org.scalajs.dom
      var isCancel = false
      var counter: Long = 0

      def send(): Unit = {
        val current = counter
        counter += 1
        Sink[G].onNext(sink)(current)
      }

      send()

      val intervalId = dom.window.setInterval(() => if (!isCancel) send(), delay.toDouble)

      Subscription { () =>
        isCancel = true
        dom.window.clearInterval(intervalId)
      }
    }
  }

  def merge[S[_]: Source, A](sources: S[A]*): SourceStream[A] = mergeSeq(sources)

  def mergeSeq[S[_]: Source, A](sources: Seq[S[A]]): SourceStream[A] = new SourceStream[A] {
    def subscribe[G[_]: Sink](sink: G[_ >: A]): Subscription = {
      Subscription.compositeFromIterable(sources.map(Source[S].subscribe(_)(sink)))
    }
  }

  def mergeVaried[SA[_]: Source, SB[_]: Source, A](sourceA: SA[A], sourceB: SB[A]): SourceStream[A] = new SourceStream[A] {
    def subscribe[G[_]: Sink](sink: G[_ >: A]): Subscription = {
      Subscription.composite(
        Source[SA].subscribe(sourceA)(sink),
        Source[SB].subscribe(sourceB)(sink)
      )
    }
  }

  def map[F[_]: Source, A, B](source: F[A])(f: A => B): SourceStream[B] = new SourceStream[B] {
    def subscribe[G[_]: Sink](sink: G[_ >: B]): Subscription = Source[F].subscribe(source)(SinkObserver.contramap(sink)(f))
  }

  def mapFilter[F[_]: Source, A, B](source: F[A])(f: A => Option[B]): SourceStream[B] = new SourceStream[B] {
    def subscribe[G[_]: Sink](sink: G[_ >: B]): Subscription = Source[F].subscribe(source)(SinkObserver.contramapFilter(sink)(f))
  }

  def collect[F[_]: Source, A, B](source: F[A])(f: PartialFunction[A, B]): SourceStream[B] = new SourceStream[B] {
    def subscribe[G[_]: Sink](sink: G[_ >: B]): Subscription = Source[F].subscribe(source)(SinkObserver.contracollect(sink)(f))
  }

  def filter[F[_]: Source, A](source: F[A])(f: A => Boolean): SourceStream[A] = new SourceStream[A] {
    def subscribe[G[_]: Sink](sink: G[_ >: A]): Subscription = Source[F].subscribe(source)(SinkObserver.filter[G, A](sink)(f))
  }

  def mapTry[F[_]: Source, A, B](source: F[A])(f: A => Try[B]): SourceStream[B] = new SourceStream[B] {
    def subscribe[G[_]: Sink](sink: G[_ >: B]): Subscription = Source[F].subscribe(source)(SinkObserver.create[A](
      value => f(value) match {
        case Success(b) => Sink[G].onNext(sink)(b)
        case Failure(error) => Sink[G].onError(sink)(error)
      }
    ))
  }

  def recover[F[_]: Source, A](source: F[A])(f: PartialFunction[Throwable, A]): SourceStream[A] = new SourceStream[A] {
    def subscribe[G[_]: Sink](sink: G[_ >: A]): Subscription = {
      Source[F].subscribe(source)(SinkObserver.create[A](
        Sink[G].onNext(sink),
        { error =>
          f.lift(error) match {
            case Some(v) => Sink[G].onNext(sink)(v)
            case None => Sink[G].onError(sink)(error)
          }
        }
      ))
    }
  }

  def scan[F[_]: Source, A, B](source: F[A])(seed: B)(f: (B, A) => B): SourceStream[B] = new SourceStream[B] {
    def subscribe[G[_]: Sink](sink: G[_ >: B]): Subscription = {
      var state = seed

      Sink[G].onNext(sink)(seed)

      Source[F].subscribe(source)(SinkObserver.create[A](
        { value =>
          val result = f(state, value)
          state = result
          Sink[G].onNext(sink)(result)
        },
        Sink[G].onError(sink)
      ))
    }
  }

  def mergeMap[SA[_]: Source, SB[_]: Source, A, B](sourceA: SA[A])(f: A => SB[B]): SourceStream[B] = new SourceStream[B] {
    def subscribe[G[_]: Sink](sink: G[_ >: B]): Subscription = {
      val subscriptions = Subscription.builder()

      subscriptions += Source[SA].subscribe(sourceA)(SinkObserver.create[A](
        { value =>
          val sourceB = f(value)
          subscriptions += Source[SB].subscribe(sourceB)(sink)
        },
        Sink[G].onError(sink)
      ))

      subscriptions
    }
  }

  def switchMap[SA[_]: Source, SB[_]: Source, A, B](sourceA: SA[A])(f: A => SB[B]): SourceStream[B] = new SourceStream[B] {
    def subscribe[G[_]: Sink](sink: G[_ >: B]): Subscription = {
      val current = Subscription.variable()

      val subscription = Source[SA].subscribe(sourceA)(SinkObserver.create[A](
        { value =>
          val sourceB = f(value)
          current() = Source[SB].subscribe(sourceB)(sink)
        },
        Sink[G].onError(sink)
      ))

      Subscription.composite(current, subscription)
    }
  }

  @inline def combineLatest[SA[_]: Source, SB[_]: Source, A, B](sourceA: SA[A])(sourceB: SB[B]): SourceStream[(A,B)] = combineLatestMap(sourceA)(sourceB)(_ -> _)

  def combineLatestMap[SA[_]: Source, SB[_]: Source, A, B, R](sourceA: SA[A])(sourceB: SB[B])(f: (A, B) => R): SourceStream[R] = new SourceStream[R] {
    def subscribe[G[_]: Sink](sink: G[_ >: R]): Subscription = {
      var latestA = Option.empty[A]
      var latestB = Option.empty[B]

      def send(): Unit = for {
        a <- latestA
        b <- latestB
      } Sink[G].onNext(sink)(f(a,b))

      Subscription.composite(
        Source[SA].subscribe(sourceA)(SinkObserver.create[A](
          { value =>
            latestA = Some(value)
            send()
          },
          Sink[G].onError(sink)
        )),
        Source[SB].subscribe(sourceB)(SinkObserver.create[B](
          { value =>
            latestB = Some(value)
            send()
          },
          Sink[G].onError(sink)
        ))
      )
    }
  }

  def withLatestFrom[SA[_]: Source, SB[_]: Source, A, B, R](source: SA[A])(latest: SB[B])(f: (A, B) => R): SourceStream[R] = new SourceStream[R] {
    def subscribe[G[_]: Sink](sink: G[_ >: R]): Subscription = {
      var latestValue = Option.empty[B]

      Subscription.composite(
        Source[SA].subscribe(source)(SinkObserver.create[A](
          value => latestValue.foreach(latestValue => Sink[G].onNext(sink)(f(value, latestValue))),
          Sink[G].onError(sink)
        )),
        Source[SB].subscribe(latest)(SinkObserver.create[B](
          value => latestValue = Some(value),
          Sink[G].onError(sink)
        ))
      )
    }
  }

  def zipWithIndex[S[_]: Source, A, R](source: S[A]): SourceStream[(A, Int)] = new SourceStream[(A, Int)] {
    def subscribe[G[_]: Sink](sink: G[_ >: (A, Int)]): Subscription = {
      var counter = 0

      Source[S].subscribe(source)(SinkObserver.create[A](
        { value =>
          val index = counter
          counter += 1
          Sink[G].onNext(sink)((value, index))
        },
        Sink[G].onError(sink)
      ))
    }
  }

  def debounce[S[_]: Source, A](source: S[A])(duration: FiniteDuration): SourceStream[A] = debounceMillis(source)(duration.toMillis.toInt)

  def debounceMillis[S[_]: Source, A](source: S[A])(duration: Int): SourceStream[A] = new SourceStream[A] {
    def subscribe[G[_]: Sink](sink: G[_ >: A]): Subscription = {
      import org.scalajs.dom
      var lastTimeout = Option.empty[Int]

      Source[S].subscribe(source)(SinkObserver.create[A](
        { value =>
          lastTimeout.foreach(dom.window.clearTimeout)
          lastTimeout = Some(dom.window.setTimeout(() => Sink[G].onNext(sink)(value), duration.toDouble))
        },
        Sink[G].onError(sink)
      ))
    }
  }

  //TODO setImmediate?
  def async[S[_]: Source, A](source: S[A]): SourceStream[A] = delayMillis(source)(0)

  def delay[S[_]: Source, A](source: S[A])(duration: FiniteDuration): SourceStream[A] = delayMillis(source)(duration.toMillis.toInt)

  def delayMillis[S[_]: Source, A](source: S[A])(duration: Int): SourceStream[A] = new SourceStream[A] {
    def subscribe[G[_]: Sink](sink: G[_ >: A]): Subscription = {
      import org.scalajs.dom
      var lastTimeout = Option.empty[Int]
      var isCancel = false

      // TODO: we onyl actually cancel the last timeout. The check isCancel
      // makes sure that cancelled subscription is really respected.
      Subscription.composite(
        Subscription { () =>
          isCancel = true
          lastTimeout.foreach(dom.window.clearTimeout)
        },
        Source[S].subscribe(source)(SinkObserver.create[A](
          { value =>
            lastTimeout = Some(dom.window.setTimeout(() => if (!isCancel) Sink[G].onNext(sink)(value), duration.toDouble))
          },
          Sink[G].onError(sink)
        ))
      )
    }
  }

  def distinct[S[_]: Source, A : Eq](source: S[A]): SourceStream[A] = new SourceStream[A] {
    def subscribe[G[_]: Sink](sink: G[_ >: A]): Subscription = {
      var lastValue = Option.empty[A]

      Source[S].subscribe(source)(SinkObserver.create[A](
        { value =>
            val shouldSend = lastValue.forall(lastValue => !Eq[A].eqv(lastValue, value))
            if (shouldSend) {
              lastValue = Some(value)
              Sink[G].onNext(sink)(value)
            }
        },
        Sink[G].onError(sink)
      ))
    }
  }

  @inline def distinctOnEquals[S[_]: Source, A](source: S[A]): SourceStream[A] = distinct(source)(Source[S], Eq.fromUniversalEquals)

  def concatMapFuture[S[_]: Source, A, B](source: S[A])(f: A => Future[B]): SourceStream[B] = new SourceStream[B] {
    def subscribe[G[_]: Sink](sink: G[_ >: B]): Subscription = {
      var lastFuture = Future.successful(())

      Source[S].subscribe(source)(SinkObserver.create[A](
        { value =>
          lastFuture = lastFuture
            .flatMap(_ => f(value))
            .map(value => Sink[G].onNext(sink)(value))
            .recover { case NonFatal(error) => Sink[G].onError(sink)(error) }
        },
        Sink[G].onError(sink)
      ))
    }
  }

  def concatMapAsync[S[_]: Source, G[_]: Effect, A, B](source: S[A])(f: A => G[B]): SourceStream[B] = concatMapFuture(source) { value =>
    val promise = Promise[B]()

    Effect[G].runAsync(f(value))(either => IO {
      either match {
        case Right(value) => promise.success(value)
        case Left(error)  => promise.failure(error)
      }
    }).unsafeRunSync()

    promise.future
  }

  @inline def mapSync[S[_]: Source, G[_]: RunSyncEffect, A, B](source: S[A])(f: A => G[B]): SourceStream[B] = mapTry(source)(v => Try(RunSyncEffect[G].unsafeRun(f(v))))

  def withDefaultSubscription[S[_]: Source, F[_]: Sink, A](source: S[A])(sink: F[A]): SourceStream[A] = new SourceStream[A] {
    private var defaultSubscription = Source[S].subscribe(source)(sink)

    def subscribe[G[_]: Sink](sink: G[_ >: A]): Subscription = {
      // stop the default subscription.
      if (defaultSubscription != null) {
        defaultSubscription.cancel()
        defaultSubscription = null
      }

      Source[S].subscribe(source)(sink)
    }
  }

  def share[F[_]: Source, A](source: F[A]): SourceStream[A] = new SourceStream[A] {
    private var currentSubscription: Subscription = null
    private val subscribers = new js.Array[SinkObserver[A]]

    def subscribe[G[_]: Sink](sink: G[_ >: A]): Subscription = {
      val observer = SinkObserver.lift(sink)
      subscribers.push(observer)

      if (currentSubscription == null) {
        currentSubscription = Source[F].subscribe(source)(SinkObserver.create[A](
          value => subscribers.foreach(_.onNext(value)),
          err => subscribers.foreach(_.onError(err)),
        ))
      }

      Subscription { () =>
        JSArrayHelper.removeElement(subscribers)(observer)
        if (subscribers.isEmpty) {
          currentSubscription.cancel()
          currentSubscription = null
        }
      }
    }
  }

  def prepend[F[_]: Source, A](source: F[A])(value: A): SourceStream[A] = new SourceStream[A] {
    def subscribe[G[_]: Sink](sink: G[_ >: A]): Subscription = {
      Sink[G].onNext(sink)(value)
      Source[F].subscribe(source)(sink)
    }
  }

  def startWith[F[_]: Source, A](source: F[A])(values: Iterable[A]): SourceStream[A] = new SourceStream[A] {
    def subscribe[G[_]: Sink](sink: G[_ >: A]): Subscription = {
      values.foreach(Sink[G].onNext(sink))
      Source[F].subscribe(source)(sink)
    }
  }

  //TODO write as explicit SinkObserver instead of filter, more readable.
  def take[F[_]: Source, A](source: F[A])(num: Int): SourceStream[A] = {
    if (num <= 0) SourceStream.empty
    else new SourceStream[A] {
      def subscribe[G[_]: Sink](sink: G[_ >: A]): Subscription = {
        var counter = 0
        val subscription = Subscription.variable()
        subscription() = Source[F].subscribe(source)(SinkObserver.filter(sink) { _ =>
          if (num > counter) {
            counter += 1
            true
          } else {
            subscription.cancel()
            false
          }
        })

        subscription
      }
    }
  }

  //TODO write as explicit SinkObserver instead of filter, more readable.
  def drop[F[_]: Source, A](source: F[A])(num: Int): SourceStream[A] = {
    if (num <= 0) SourceStream.lift(source)
    else new SourceStream[A] {
      def subscribe[G[_]: Sink](sink: G[_ >: A]): Subscription = {
        var counter = 0
        Source[F].subscribe(source)(SinkObserver.filter(sink) { _ =>
          if (num > counter) {
            counter += 1
            false
          } else true
        })
      }
    }
  }

  implicit object source extends Source[SourceStream] {
    @inline def subscribe[G[_]: Sink, A](source: SourceStream[A])(sink: G[_ >: A]): Subscription = source.subscribe(sink)
  }

  implicit object liftSource extends LiftSource[SourceStream] {
    @inline def lift[G[_]: Source, A](source: G[A]): SourceStream[A] = SourceStream.lift[G, A](source)
  }

  implicit object monoidK extends MonoidK[SourceStream] {
    @inline def empty[T] = SourceStream.empty
    @inline def combineK[T](a: SourceStream[T], b: SourceStream[T]) = SourceStream.mergeVaried(a, b)
  }

  implicit object functor extends Functor[SourceStream] {
    @inline def map[A, B](fa: SourceStream[A])(f: A => B): SourceStream[B] = SourceStream.map(fa)(f)
  }

  implicit object functorFilter extends FunctorFilter[SourceStream] {
    @inline def functor = SourceStream.functor
    @inline def mapFilter[A, B](fa: SourceStream[A])(f: A => Option[B]): SourceStream[B] = SourceStream.mapFilter(fa)(f)
  }

  @inline implicit class Operations[A](val source: SourceStream[A]) extends AnyVal {
    @inline def lift[G[_]: LiftSource]: G[A] = LiftSource[G].lift(source)
    @inline def mergeMap[S[_]: Source, B](f: A => S[B]): SourceStream[B] = SourceStream.mergeMap(source)(f)
    @inline def switchMap[S[_]: Source, B](f: A => S[B]): SourceStream[B] = SourceStream.switchMap(source)(f)
    @inline def combineLatest[S[_]: Source, B, R](combined: S[B]): SourceStream[(A,B)] = SourceStream.combineLatest(source)(combined)
    @inline def combineLatestMap[S[_]: Source, B, R](combined: S[B])(f: (A, B) => R): SourceStream[R] = SourceStream.combineLatestMap(source)(combined)(f)
    @inline def withLatestFrom[S[_]: Source, B, R](latest: S[B])(f: (A, B) => R): SourceStream[R] = SourceStream.withLatestFrom(source)(latest)(f)
    @inline def zipWithIndex: SourceStream[(A, Int)] = SourceStream.zipWithIndex(source)
    @inline def debounce(duration: FiniteDuration): SourceStream[A] = SourceStream.debounce(source)(duration)
    @inline def async: SourceStream[A] = SourceStream.async(source)
    @inline def delay(duration: FiniteDuration): SourceStream[A] = SourceStream.delay(source)(duration)
    @inline def delayMillis(millis: Int): SourceStream[A] = SourceStream.delayMillis(source)(millis)
    @inline def distinctOnEquals: SourceStream[A] = SourceStream.distinctOnEquals(source)
    @inline def distinct(implicit eq: Eq[A]): SourceStream[A] = SourceStream.distinct(source)
    @inline def concatMapFuture[B](f: A => Future[B]): SourceStream[B] = SourceStream.concatMapFuture(source)(f)
    @inline def concatMapAsync[G[_]: Effect, B](f: A => G[B]): SourceStream[B] = SourceStream.concatMapAsync(source)(f)
    @inline def mapSync[G[_]: RunSyncEffect, B](f: A => G[B]): SourceStream[B] = SourceStream.mapSync(source)(f)
    @inline def map[B](f: A => B): SourceStream[B] = SourceStream.map(source)(f)
    @inline def mapTry[B](f: A => Try[B]): SourceStream[B] = SourceStream.mapTry(source)(f)
    @inline def mapFilter[B](f: A => Option[B]): SourceStream[B] = SourceStream.mapFilter(source)(f)
    @inline def collect[B](f: PartialFunction[A, B]): SourceStream[B] = SourceStream.collect(source)(f)
    @inline def filter(f: A => Boolean): SourceStream[A] = SourceStream.filter(source)(f)
    @inline def scan[B](seed: B)(f: (B, A) => B): SourceStream[B] = SourceStream.scan(source)(seed)(f)
    @inline def recover(f: PartialFunction[Throwable, A]): SourceStream[A] = SourceStream.recover(source)(f)
    @inline def share: SourceStream[A] = SourceStream.share(source)
    @inline def prepend(value: A): SourceStream[A] = SourceStream.prepend(source)(value)
    @inline def startWith(values: Iterable[A]): SourceStream[A] = SourceStream.startWith(source)(values)
    @inline def take(num: Int): SourceStream[A] = SourceStream.take(source)(num)
    @inline def drop(num: Int): SourceStream[A] = SourceStream.drop(source)(num)
    @inline def withDefaultSubscription[G[_] : Sink](sink: G[A]): SourceStream[A] = SourceStream.withDefaultSubscription(source)(sink)
  }
}
