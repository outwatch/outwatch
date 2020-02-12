package outwatch

import cats.effect.IO

import outwatch.ext.monix._
import outwatch.ext.monix.handler._

import monix.reactive._
import monix.reactive.subjects.PublishSubject
import monix.execution.ExecutionModel.SynchronousExecution
import monix.execution.{Cancelable, Scheduler}
import monix.execution.Ack.Continue
import monix.execution.schedulers.TrampolineScheduler
import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AsyncFlatSpec

class MonixOpsSpec extends AsyncFlatSpec with Matchers {
  implicit val scheduler = TrampolineScheduler(Scheduler.global, SynchronousExecution)

  implicit class Subscriber[T](obs: Observable[T]) {
    def apply(next: T => Unit): Cancelable = obs.subscribe { t =>
      next(t)
      Continue
    }
  }

  "Observer" should "redirect" in {

    var currentValue = 0

    val subject = PublishSubject[Int]
    subject.foreach{currentValue = _}

    val redirected = subject.redirect[Int](_.map(_ + 1))
    redirected.connect()

    for {
      _ <- subject.onNext(5)
      _ = currentValue shouldBe 5
      _ <- redirected.onNext(5)
      _ = currentValue shouldBe 6

    } yield succeed
  }

  "PublishSubject" should "transformObservable" in {

    var currentValue = 0

    val subject = PublishSubject[Int]
    val mapped = subject.transformObservable(_.map(_ + 1))
    mapped.foreach{currentValue = _}

    for {
      _ <- subject.onNext(5)
      _ = currentValue shouldBe 6
      _ <- mapped.onNext(7)
      _ = currentValue shouldBe 8

    } yield succeed
  }

  "Subject" should "lens" in {

    var handlerValue: (String, Int) = null
    var lensedValue: Int = -100

    for {

      handler <- Handler.createF[IO, (String, Int)].unsafeToFuture()
       lensed = handler.lens[Int](("harals", 0))(_._2)((tuple, num) => (tuple._1, num))
            _ = lensed.connect()
            _ = handler(handlerValue = _)
            _ = lensed(lensedValue = _)
            _ <- lensed.onNext(15)
            _ = lensedValue shouldBe 15
            _ = handlerValue shouldBe (("harals", 15))
            _ <- handler.onNext(("peter", 12))
            _ = lensedValue shouldBe 12
            _ = handlerValue shouldBe (("peter", 12))
            _ <- lensed.onNext(-1)
            _ = lensedValue shouldBe -1
            _ = handlerValue shouldBe (("peter", -1))

    } yield succeed
  }

  it should "mapObservable" in {

    var handlerValue: Int = -100
    var lensedValue: Int = -100

    for {
      handler <- Handler.createF[IO, Int].unsafeToFuture()
       lensed = handler.mapObservable(_ - 1)
            _ = handler(handlerValue = _)
            _ = lensed(lensedValue = _)
            _ <- lensed.onNext(15)
            _ = lensedValue shouldBe 14
            _ = handlerValue shouldBe 15
            _ <- handler.onNext(12)
            _ = lensedValue shouldBe 11
            _ = handlerValue shouldBe 12

    } yield succeed
  }

  it should "transformObservable" in {

    var handlerValue: Int = -100
    var lensedValue: Int = -100

    for {
      handler <- Handler.createF[IO, Int].unsafeToFuture()
       lensed = handler.transformObservable(_.map(_ - 1))
            _ = handler(handlerValue = _)
            _ = lensed(lensedValue = _)
            _ <- lensed.onNext(15)
            _ = lensedValue shouldBe 14
            _ = handlerValue shouldBe 15
            _ <- handler.onNext(12)
            _ = lensedValue shouldBe 11
            _ = handlerValue shouldBe 12

    } yield succeed
  }

  it should "mapObserver" in {

    var handlerValue: Int = -100
    var lensedValue: Int = -100

    for {
      handler <- Handler.createF[IO, Int].unsafeToFuture()
        lensed = handler.mapObserver[Int](_ + 1)
             _ = handler(handlerValue = _)
             _ = lensed(lensedValue = _)
             _ <- lensed.onNext(15)
             _ = lensedValue shouldBe 16
             _ = handlerValue shouldBe 16
             _ <- handler.onNext(12)
             _ = lensedValue shouldBe 12
             _ = handlerValue shouldBe 12

    } yield succeed
  }

  it should "transformObserver" in {

    var handlerValue: Int = -100
    var lensedValue: Int = -100

    for {
      handler <- Handler.createF[IO, Int].unsafeToFuture()
       lensed = handler.transformObserver[Int](_.map(_ + 1))
            _ = lensed.connect()
            _ = handler(handlerValue = _)
            _ = lensed(lensedValue = _)
            _ <- lensed.onNext(15)
            _ = lensedValue shouldBe 16
            _ = handlerValue shouldBe 16
            _ <- handler.onNext(12)
            _ = lensedValue shouldBe 12
            _ = handlerValue shouldBe 12

    } yield succeed
  }

  it should "mapSubject" in {

    var handlerValue: Int = -100
    var lensedValue: Int = -100

    for {
      handler <- Handler.createF[IO, Int].unsafeToFuture()
       lensed = handler.mapHandler[Int](_ + 1)(_ - 1)
            _ = handler(handlerValue = _)
            _ = lensed(lensedValue = _)
            _ <- lensed.onNext(15)
            _ = lensedValue shouldBe 15
            _ = handlerValue shouldBe 16
            _ <- handler.onNext(12)
            _ = lensedValue shouldBe 11
            _ = handlerValue shouldBe 12

    } yield succeed
  }

  it should "transformSubject" in {

    var handlerValue: Int = -100
    var lensedValue: Int = -100

    for {
      handler <- Handler.createF[IO, Int].unsafeToFuture()
       lensed = handler.transformHandler[Int](_.map(_ + 1))(_.map(_ - 1))
            _ = lensed.connect()
            _ = handler(handlerValue = _)
            _ = lensed(lensedValue = _)
            _ <- lensed.onNext(15)
            _ = lensedValue shouldBe 15
            _ = handlerValue shouldBe 16
            _ <- handler.onNext(12)
            _ = lensedValue shouldBe 11
            _ = handlerValue shouldBe 12

    } yield succeed

  }
}
