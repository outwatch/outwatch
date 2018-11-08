package outwatch.dom

import cats.Monad
import monix.eval.Task
import monix.execution.{Ack, Cancelable, CancelableFuture, Scheduler}
import monix.reactive.observers.Subscriber
import monix.reactive.subjects.Var
import monix.reactive.{Observable, ObservableLike, Observer, OverflowStrategy}

import scala.concurrent.Future

trait ValueObservable[+T] { self =>

  def value(): ObservableWithInitialValue[T]

  final val observable: Observable[T] = (subscriber: Subscriber[T]) => self.value().observable.unsafeSubscribeFn(subscriber)

  @inline final def transformHead[B >: T](f: Option[T] => Option[B]): ValueObservable[B] = transform[B](f, identity)
  @inline final def transformTail[B >: T](f: Observable[T] => Observable[B]): ValueObservable[B] = transform[B](identity, f)
  @inline final def transformWith[B](f: ObservableWithInitialValue[T] => ObservableWithInitialValue[B]): ValueObservable[B] = () => f(self.value())
  @inline final def transform[B](transformHead: Option[T] => Option[B], transformTail: Observable[T] => Observable[B]): ValueObservable[B] = transformWith { v =>
    ObservableWithInitialValue(transformHead(v.head), transformTail(v.tail))
  }

  final def memoizeValue: ValueObservable[T] = new ValueObservable[T] {
    private var cached: Observable[T] = null
    override def value(): ObservableWithInitialValue[T] = {
      if (cached == null) {
        val v = self.value()
        cached = v.tail
        v
      } else ObservableWithInitialValue(None, cached)
    }
  }

  // the following methods are convenience methods that mimick that API of Observable on a ValueObservable

  final def map[B](f: T => B): ValueObservable[B] = transformWith { v =>
    ObservableWithInitialValue(v.head.map(f), v.tail.map(f))
  }

  final def scan[S](seed: => S)(op: (S, T) => S): ValueObservable[S] = transformWith { v =>
    val mappedHeadValue = v.head.map(op(seed, _))
    ObservableWithInitialValue(mappedHeadValue, v.tail.scan[S](mappedHeadValue.getOrElse(seed))(op))
  }

  final def collect[B](f: PartialFunction[T, B]): ValueObservable[B] = transform[B](_.collect(f), _.collect(f))
  final def filter(f: T => Boolean): ValueObservable[T] = transform[T](_.filter(f), _.filter(f))

  @inline final def +:[B >: T](elem: B): ValueObservable[B] = prepend(elem)
  final def prepend[B >: T](elem: B): ValueObservable[B] = ValueObservable.from(observable, elem)

  @inline final def :+[B >: T](elem: B): ValueObservable[B] = append(elem)
  final def append[B >: T](elem: B): ValueObservable[B] = transformTail[B](_.append(elem))

  final def startWith[B >: T](elems: Seq[B]): ValueObservable[B] = {
    if (elems.isEmpty) this
    else if (elems.size == 1) ValueObservable.from(observable, elems.head)
    else ValueObservable.from(observable.startWith(elems.tail), elems.head)
  }

  final def distinctUntilChanged[TT >: T](implicit e: cats.Eq[TT]): ValueObservable[TT] = transformWith { v =>
    v.copy(tail = v.observable.distinctUntilChanged[TT].tail)
  }

  final def distinctUntilChangedByKey[K](key: T => K)(implicit e: cats.Eq[K]): ValueObservable[T] = transformWith { v =>
    v.copy(tail = v.observable.distinctUntilChangedByKey[K](key).tail)
  }

  @inline final def merge[B](implicit ev: T <:< ValueObservable[B], os: OverflowStrategy[B] = OverflowStrategy.Default): ValueObservable[B] = mergeMap[B](x => x)
  final def mergeMap[B](f: T => ValueObservable[B])(implicit os: OverflowStrategy[B] = OverflowStrategy.Default): ValueObservable[B] = transformWith { v =>
    v.head.fold(ObservableWithInitialValue(None, v.tail.mergeMap(v => f(v).observable))) { value =>
      val headV = f(value).value()
      headV.copy(tail = v.tail.map(v => f(v).observable).prepend(headV.tail).merge)
    }
  }

  @inline final def flatten[B](implicit ev: T <:< ValueObservable[B]): ValueObservable[B] = concat
  @inline final def concat[B](implicit ev: T <:< ValueObservable[B]): ValueObservable[B] = concatMap[B](x => x)
  @inline final def flatMap[B](f: T => ValueObservable[B]): ValueObservable[B] = concatMap(f)
  final def concatMap[B](f: T => ValueObservable[B]): ValueObservable[B] = transformWith { v =>
    v.head.fold(ObservableWithInitialValue(None, v.tail.concatMap(v => f(v).observable))) { value =>
      val headV = f(value).value()
      headV.copy(tail = v.tail.map(v => f(v).observable).prepend(headV.tail).concat)
    }
  }

  @inline final def switch[B](implicit ev: T <:< ValueObservable[B]): ValueObservable[B] = switchMap[B](x => x)
  final def switchMap[B](f: T => ValueObservable[B]): ValueObservable[B] = transformWith { v =>
    v.head.fold(ObservableWithInitialValue(None, v.tail.switchMap(v => f(v).observable))) { value =>
      val headV = f(value).value()
      headV.copy(tail = v.tail.map(v => f(v).observable).prepend(headV.tail).switch)
    }
  }

  final def switchIfEmpty[B >: T](backup: Observable[B]): ValueObservable[B] = transformWith { v =>
    val newTail = if (v.head.isEmpty) v.tail.switchIfEmpty[B](backup) else v.tail
    ObservableWithInitialValue(v.head, newTail)
  }

  @inline def combineLatest[B](other: ValueObservable[B]): ValueObservable[(T, B)] = combineLatestMap(other)(_ -> _)
  final def combineLatestMap[B, R](other: ValueObservable[B])(f: (T, B) => R): ValueObservable[R] = transformWith { v0 =>
    val v1 = other.value()
    val head = for {v0 <- v0.head; v1 <- v1.head } yield f(v0, v1)
    val tail = v0.observable.combineLatest(v1.observable)
    val tailWithoutHead = if (head.isEmpty) tail else tail.tail
    ObservableWithInitialValue(head, tailWithoutHead.map[R](f.tupled))
  }

  final def withLatestFrom[B, R](o1: ValueObservable[B])(f: (T, B) => R): ValueObservable[R] = transformWith { v0 =>
    val v1 = o1.value()
    val head = for {v0 <- v0.head; v1 <- v1.head } yield f(v0, v1)
    val tail = v0.tail.withLatestFrom(v1.observable)(f)
    ObservableWithInitialValue(head, tail)
  }
  final def withLatestFrom2[B1, B2, R](o1: ValueObservable[B1], o2: ValueObservable[B2])(f: (T, B1, B2) => R): ValueObservable[R] = transformWith { v0 =>
    val v1 = o1.value()
    val v2 = o2.value()
    val head = for {v0 <- v0.head; v1 <- v1.head; v2 <- v2.head } yield f(v0, v1, v2)
    val tail = v0.tail.withLatestFrom2(v1.observable, v2.observable)(f)
    ObservableWithInitialValue(head, tail)
  }
  final def withLatestFrom3[B1, B2, B3, R](o1: ValueObservable[B1], o2: ValueObservable[B2], o3: ValueObservable[B3])(f: (T, B1, B2, B3) => R): ValueObservable[R] = transformWith { v0 =>
    val v1 = o1.value()
    val v2 = o2.value()
    val v3 = o3.value()
    val head = for {v0 <- v0.head; v1 <- v1.head; v2 <- v2.head; v3 <- v3.head } yield f(v0, v1, v2, v3)
    val tail = v0.tail.withLatestFrom3(v1.observable, v2.observable, v3.observable)(f)
    ObservableWithInitialValue(head, tail)
  }
  final def withLatestFrom4[B1, B2, B3, B4, R](o1: ValueObservable[B1], o2: ValueObservable[B2], o3: ValueObservable[B3], o4: ValueObservable[B4])(f: (T, B1, B2, B3, B4) => R): ValueObservable[R] = transformWith { v0 =>
    val v1 = o1.value()
    val v2 = o2.value()
    val v3 = o3.value()
    val v4 = o4.value()
    val head = for {v0 <- v0.head; v1 <- v1.head; v2 <- v2.head; v3 <- v3.head; v4 <- v4.head } yield f(v0, v1, v2, v3, v4)
    val tail = v0.tail.withLatestFrom4(v1.observable, v2.observable, v3.observable, v4.observable)(f)
    ObservableWithInitialValue(head, tail)
  }
  final def withLatestFrom5[B1, B2, B3, B4, B5, R](o1: ValueObservable[B1], o2: ValueObservable[B2], o3: ValueObservable[B3], o4: ValueObservable[B4], o5: ValueObservable[B5])(f: (T, B1, B2, B3, B4, B5) => R): ValueObservable[R] = transformWith { v0 =>
    val v1 = o1.value()
    val v2 = o2.value()
    val v3 = o3.value()
    val v4 = o4.value()
    val v5 = o5.value()
    val head = for {v0 <- v0.head; v1 <- v1.head; v2 <- v2.head; v3 <- v3.head; v4 <- v4.head; v5 <- v5.head } yield f(v0, v1, v2, v3, v4, v5)
    val tail = v0.tail.withLatestFrom5(v1.observable, v2.observable, v3.observable, v4.observable, v5.observable)(f)
    ObservableWithInitialValue(head, tail)
  }
  final def withLatestFrom6[B1, B2, B3, B4, B5, B6, R](o1: ValueObservable[B1], o2: ValueObservable[B2], o3: ValueObservable[B3], o4: ValueObservable[B4], o5: ValueObservable[B5], o6: ValueObservable[B6])(f: (T, B1, B2, B3, B4, B5, B6) => R): ValueObservable[R] = transformWith { v0 =>
    val v1 = o1.value()
    val v2 = o2.value()
    val v3 = o3.value()
    val v4 = o4.value()
    val v5 = o5.value()
    val v6 = o6.value()
    val head = for {v0 <- v0.head; v1 <- v1.head; v2 <- v2.head; v3 <- v3.head; v4 <- v4.head; v5 <- v5.head; v6 <- v6.head } yield f(v0, v1, v2, v3, v4, v5, v6)
    val tail = v0.tail.withLatestFrom6(v1.observable, v2.observable, v3.observable, v4.observable, v5.observable, v6.observable)(f)
    ObservableWithInitialValue(head, tail)
  }

  @inline final def foreach(cb: T => Unit)(implicit s: Scheduler): CancelableFuture[Unit] = observable.foreach(cb)
  @inline final def foreachL(cb: T => Unit): Task[Unit] = observable.foreachL(cb)
  @inline final def subscribe(observer: Observer[T])(implicit s: Scheduler): Cancelable = observable.subscribe(observer)
  @inline final def subscribe(subscriber: Subscriber[T]): Cancelable = observable.subscribe(subscriber)
  @inline final def subscribe()(implicit s: Scheduler): Cancelable = observable.subscribe()
  @inline final def subscribe(nextFn: T => Future[Ack])(implicit s: Scheduler): Cancelable = observable.subscribe(nextFn)
  @inline final def subscribe(nextFn: T => Future[Ack], errorFn: Throwable => Unit)(implicit s: Scheduler): Cancelable = observable.subscribe(nextFn, errorFn)
  @inline final def subscribe(nextFn: T => Future[Ack], errorFn: Throwable => Unit, completedFn: () => Unit)(implicit s: Scheduler): Cancelable = observable.subscribe(nextFn, errorFn, completedFn)
  @inline final def subscribeOn(scheduler: Scheduler): Observable[T] = observable.subscribeOn(scheduler)

  final def takeUntil(trigger: Observable[Any]): ValueObservable[T] = transformTail(_.takeUntil(trigger))
  final def takeWhile(p: T => Boolean): ValueObservable[T] = transformWith { v =>
    v.head.fold(ObservableWithInitialValue(None, v.tail.takeWhile(p))) { value =>
      if (p(value)) ObservableWithInitialValue(Some(value), v.tail.takeWhile(p))
      else ObservableWithInitialValue(None, Observable.empty)
    }
  }

  final def take(n: Long): ValueObservable[T] = if (n <= 0) ValueObservable.empty else transformWith { v =>
    val tailN = if (v.head.isEmpty) n else n - 1
    ObservableWithInitialValue(v.head, v.tail.take(tailN))
  }

  final def doOnNext(cb: T => Task[Unit]): ValueObservable[T] = transformTail(_.doOnNext(cb))
  final def doOnError(cb: Throwable => Task[Unit]): ValueObservable[T] = transformTail(_.doOnError(cb))
  final def doOnComplete(cb: Task[Unit]): ValueObservable[T] = transformTail(_.doOnComplete(cb))
  final def doOnSubscribe(cb: Task[Unit]): ValueObservable[T] = transformTail(_.doOnSubscribe(cb))
  final def doOnEarlyStop(cb: Task[Unit]): ValueObservable[T] = transformTail(_.doOnEarlyStop(cb))
  final def doOnStart(cb: T => Task[Unit]): ValueObservable[T] = transformTail(_.doOnStart(cb))
  final def doNextAck(cb: (T, Ack) => Task[Unit]): ValueObservable[T] = transformTail(_.doOnNextAck(cb))
  final def doOnSubscriptionCancel(cb: Task[Unit]): ValueObservable[T] = transformTail(_.doOnSubscriptionCancel(cb))

  final def share(implicit s: Scheduler): ValueObservable[T] = transformTail(_.share).memoizeValue
}

object ValueObservable {
  implicit object monad extends Monad[ValueObservable] {
    @inline override def map[A, B](fa: ValueObservable[A])(f: A => B): ValueObservable[B] = fa.map(f)
    @inline override def pure[A](x: A): ValueObservable[A] = ValueObservable[A](x)
    @inline override def flatMap[A, B](fa: ValueObservable[A])(f: A => ValueObservable[B]): ValueObservable[B] = fa.flatMap(f)
    override def tailRecM[A, B](a: A)(f: A => ValueObservable[Either[A, B]]): ValueObservable[B] = f(a).flatMap {
      case Left(a) => tailRecM(a)(f)
      case Right(b) => ValueObservable(b)
    }
  }

  implicit object observableLike extends ObservableLike[ValueObservable] {
    @inline override def toObservable[A](fa: ValueObservable[A]): Observable[A] = fa.observable
  }

  @inline def empty: ValueObservable[Nothing] = apply()
  @inline def apply[T](): ValueObservable[T] = from[T](Observable.empty)
  @inline def apply[T](initialValue: T): ValueObservable[T] = from[T](Observable.empty, initialValue)
  @inline def apply[T](initialValue: T, value: T, values: T*): ValueObservable[T] = from[T](Observable.fromIterable(value :: values.toList), initialValue)
  @inline def from[T](stream: Observable[T]): ValueObservable[T] = fromOption(stream, None)
  @inline def from[T](stream: Observable[T], initialValue: T): ValueObservable[T] = fromOption(stream, Some(initialValue))
  def fromOption[T](stream: Observable[T], initialValue: Option[T]): ValueObservable[T] = () => ObservableWithInitialValue(initialValue, stream)
  def from[T](stream: Var[T]): ValueObservable[T] = () => ObservableWithInitialValue(Some(stream.apply()), stream.drop(1))

  @inline def from[F[_], T](stream: F[T])(implicit asValueObservable: AsValueObservable[F]): ValueObservable[T] = asValueObservable.as(stream)
}

final case class ObservableWithInitialValue[+T](head: Option[T], tail: Observable[T]) {
  def observable: Observable[T] = head.fold(tail)(tail.prepend)
}
