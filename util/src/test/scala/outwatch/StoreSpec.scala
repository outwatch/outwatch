package outwatch

import cats.effect.IO
import outwatch.util._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class StoreSpec extends AnyFlatSpec with Matchers {

  sealed trait CounterAction

  case object Initial extends CounterAction
  case object Plus extends CounterAction
  case object Minus extends CounterAction

  type Model = Int

  val reduce: Reducer[CounterAction, Model] = Reducer { 
    case (_, Initial) => ???
    case (state, Plus) => state + 1
    case (state, Minus) => state - 1
  }

  "A Store" should "emit its initial state to multiple subscribers" in {
    val store = Store.create[IO](Initial, 0, reduce).unsafeRunSync()

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

  "A Store" should "emit consecutive states to multiple subscribers" in {
    val store = Store.create[IO](Initial, 0, reduce).unsafeRunSync()

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

  "A Store" should "emit its current state to new subscribers" in {
    val store = Store.create[IO](Initial, 0, reduce).unsafeRunSync()

    (1 to 10).foreach(_ => store.onNext(Plus))

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
