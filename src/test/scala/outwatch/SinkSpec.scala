package outwatch

import org.scalatest.BeforeAndAfterEach
import org.scalatest.prop.PropertyChecks
import outwatch.dom._

class SinkSpec extends UnitSpec with BeforeAndAfterEach with PropertyChecks {
  "Sink" should "lens" in {
    val handler = createHandler[(String, Int)]().unsafeRunSync()
    val lensed = handler.lens[Int](("harals", 0))(_._2)((tuple, num) => (tuple._1, num))

    var handlerValue: (String, Int) = null
    var lensedValue: Int = -100

    handler(handlerValue = _)
    lensed(lensedValue = _)

    lensed.observer.next(15)
    lensedValue shouldBe 15
    handlerValue shouldBe (("harals", 15))

    handler.observer.next(("peter", 12))
    lensedValue shouldBe 12
    handlerValue shouldBe (("peter", 12))

    lensed.observer.next(-1)
    lensedValue shouldBe -1
    handlerValue shouldBe (("peter", -1))
  }

  it should "comapMap" in {
    val handler = createHandler[Int]().unsafeRunSync()
    val lensed = handler.comapMap(_ - 1)

    var handlerValue: Int = -100
    var lensedValue: Int = -100

    handler(handlerValue = _)
    lensed(lensedValue = _)

    lensed.observer.next(15)
    lensedValue shouldBe 14
    handlerValue shouldBe 15

    handler.observer.next(12)
    lensedValue shouldBe 11
    handlerValue shouldBe 12
  }

  it should "comap" in {
    val handler = createHandler[Int]().unsafeRunSync()
    val lensed = handler.comap(_.map(_ - 1))

    var handlerValue: Int = -100
    var lensedValue: Int = -100

    handler(handlerValue = _)
    lensed(lensedValue = _)

    lensed.observer.next(15)
    lensedValue shouldBe 14
    handlerValue shouldBe 15

    handler.observer.next(12)
    lensedValue shouldBe 11
    handlerValue shouldBe 12
  }

  it should "contramapMap" in {
    val handler = createHandler[Int]().unsafeRunSync()
    val lensed = handler.contramapMap(_ + 1)

    var handlerValue: Int = -100
    var lensedValue: Int = -100

    handler(handlerValue = _)
    lensed(lensedValue = _)

    lensed.observer.next(15)
    lensedValue shouldBe 16
    handlerValue shouldBe 16

    handler.observer.next(12)
    lensedValue shouldBe 12
    handlerValue shouldBe 12
  }

  it should "contramap" in {
    val handler = createHandler[Int]().unsafeRunSync()
    val lensed = handler.contramap(_.map(_ + 1))

    var handlerValue: Int = -100
    var lensedValue: Int = -100

    handler(handlerValue = _)
    lensed(lensedValue = _)

    lensed.observer.next(15)
    lensedValue shouldBe 16
    handlerValue shouldBe 16

    handler.observer.next(12)
    lensedValue shouldBe 12
    handlerValue shouldBe 12
  }

  it should "imapMap" in {
    val handler = createHandler[Int]().unsafeRunSync()
    val lensed = handler.imapMap[Int](_ - 1)(_ + 1)

    var handlerValue: Int = -100
    var lensedValue: Int = -100

    handler(handlerValue = _)
    lensed(lensedValue = _)

    lensed.observer.next(15)
    lensedValue shouldBe 15
    handlerValue shouldBe 16

    handler.observer.next(12)
    lensedValue shouldBe 11
    handlerValue shouldBe 12
  }

  it should "imap" in {
    val handler = createHandler[Int]().unsafeRunSync()
    val lensed = handler.imap[Int](_.map(_ - 1))(_.map(_ + 1))

    var handlerValue: Int = -100
    var lensedValue: Int = -100

    handler(handlerValue = _)
    lensed(lensedValue = _)

    lensed.observer.next(15)
    lensedValue shouldBe 15
    handlerValue shouldBe 16

    handler.observer.next(12)
    lensedValue shouldBe 11
    handlerValue shouldBe 12
  }
}
