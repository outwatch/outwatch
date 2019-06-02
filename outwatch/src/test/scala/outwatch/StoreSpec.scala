package outwatch

import monix.execution.ExecutionModel.SynchronousExecution
import monix.execution.Scheduler
import monix.execution.schedulers.TrampolineScheduler
import monix.reactive.Observable
import org.scalatest.{AsyncFlatSpec, FlatSpec, Matchers}
import outwatch.util.Store.Reducer
import outwatch.util._

class StoreSpec extends FlatSpec with Matchers {
  implicit val scheduler = TrampolineScheduler(Scheduler.global, SynchronousExecution)

  sealed trait CounterAction

  case object Initial extends CounterAction
  case object Plus extends CounterAction
  case object Minus extends CounterAction

  type Model = Int

  def reduce(state: Model, action: CounterAction): Model = action match {
    case Initial => ???
    case Plus => state + 1
    case Minus => state - 1
  }

  "A Store" should "emit its initial state to multiple subscribers" in {
    val store = Store.create(Initial, 0, reduce _).unsafeRunSync()

    var a: Option[Model] = None
    var b: Option[Model] = None

    store.foreach { case (action@_, state) =>
      a = Some(state)
    }

    store.foreach { case (action@_, state) =>
      b = Some(state)
    }

    a shouldBe Some(0)
    b shouldBe Some(0)

  }

  it should "emit consecutive states to multiple subscribers" in {
    val store = Store.create(Initial, 0, reduce _).unsafeRunSync()

    var a: Option[Model] = None
    var b: Option[Model] = None

    store.foreach { case (action@_, state) =>
      a = Some(state)
    }

    store.foreach { case (action@_, state) =>
      b = Some(state)
    }

    store.onNext(Plus)

    a shouldBe Some(1)
    b shouldBe Some(1)

    for (i <- 2 to 10) {
      store.onNext(Plus)

      a shouldBe Some(i)
      b shouldBe Some(i)
    }
  }

  it should "emit its current state to new subscribers" in {
    val store = Store.create(Initial, 0, reduce _).unsafeRunSync()

    for (i <- 1 to 10)
      store.onNext(Plus)

    var a: Option[Model] = None
    var b: Option[Model] = None

    store.foreach { case (action@_, state) =>
      a = Some(state)
    }

    store.foreach { case (action@_, state) =>
      b = Some(state)
    }

    a shouldBe Some(10)
    b shouldBe Some(10)
  }
}

class AsyncStoreSpec extends AsyncFlatSpec with Matchers {
  implicit val scheduler = TrampolineScheduler(Scheduler.global, SynchronousExecution)

  "A Store" should "emit action effects in order " in {
    val reduce: Reducer[Int, Int] = new Reducer[Int, Int]({
      case (state, 5) => (100, Observable(1, 2, 3))
      case (state, action) => (state + action, Observable.empty)
    })

    val store = Store.create[Int, Int](0, 0, reduce).unsafeRunSync()

    val storeList = store.toListL.runToFuture
    for {
      _ <- store.onNext(5)
      _ <- store.onNext(13)
      _ <- store.onNext(5)
      _ = store.onComplete()
      storeList <- storeList
    } yield storeList shouldBe List(
      (0,0),
      (5,100),
      (1,101),
      (2,103),
      (3,106),
      (13,119),
      (5,100),
      (1,101),
      (2,103),
      (3,106)
    )
  }
}
