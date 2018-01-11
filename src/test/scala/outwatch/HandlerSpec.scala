package outwatch

class HandlerSpec extends JSDomSpec {
  "Sink" should "have unsafeOnNext" in {
    val handler = Handler.create[Int].unsafeRunSync()
    var handlerValue: Int = 0
    handler(handlerValue = _)
    handler.unsafeOnNext(5)
    handlerValue shouldBe 5
  }

  "Handler" should "lens" in {
    val handler = Handler.create[(String, Int)].unsafeRunSync()
    val lensed = handler.lens[Int](("harals", 0))(_._2)((tuple, num) => (tuple._1, num))

    var handlerValue: (String, Int) = null
    var lensedValue: Int = -100

    handler(handlerValue = _)
    lensed(lensedValue = _)

    lensed.observer.onNext(15)
    lensedValue shouldBe 15
    handlerValue shouldBe (("harals", 15))

    handler.observer.onNext(("peter", 12))
    lensedValue shouldBe 12
    handlerValue shouldBe (("peter", 12))

    lensed.observer.onNext(-1)
    lensedValue shouldBe -1
    handlerValue shouldBe (("peter", -1))
  }

  it should "mapSource" in {
    val handler = Handler.create[Int].unsafeRunSync()
    val lensed = handler.mapSource(_ - 1)

    var handlerValue: Int = -100
    var lensedValue: Int = -100

    handler(handlerValue = _)
    lensed(lensedValue = _)

    lensed.observer.onNext(15)
    lensedValue shouldBe 14
    handlerValue shouldBe 15

    handler.observer.onNext(12)
    lensedValue shouldBe 11
    handlerValue shouldBe 12
  }

  it should "transformSource" in {
    val handler = Handler.create[Int].unsafeRunSync()
    val lensed = handler.transformSource(_.map(_ - 1))

    var handlerValue: Int = -100
    var lensedValue: Int = -100

    handler(handlerValue = _)
    lensed(lensedValue = _)

    lensed.observer.onNext(15)
    lensedValue shouldBe 14
    handlerValue shouldBe 15

    handler.observer.onNext(12)
    lensedValue shouldBe 11
    handlerValue shouldBe 12
  }

  it should "mapSink" in {
    val handler = Handler.create[Int].unsafeRunSync()
    val lensed = handler.mapSink[Int](_ + 1)

    var handlerValue: Int = -100
    var lensedValue: Int = -100

    handler(handlerValue = _)
    lensed(lensedValue = _)

    lensed.observer.onNext(15)
    lensedValue shouldBe 16
    handlerValue shouldBe 16

    handler.observer.onNext(12)
    lensedValue shouldBe 12
    handlerValue shouldBe 12
  }

  it should "transformSink" in {
    val handler = Handler.create[Int].unsafeRunSync()
    val lensed = handler.transformSink[Int](_.map(_ + 1))

    var handlerValue: Int = -100
    var lensedValue: Int = -100

    handler(handlerValue = _)
    lensed(lensedValue = _)

    lensed.observer.onNext(15)
    lensedValue shouldBe 16
    handlerValue shouldBe 16

    handler.observer.onNext(12)
    lensedValue shouldBe 12
    handlerValue shouldBe 12
  }

  it should "imap" in {
    val handler = Handler.create[Int].unsafeRunSync()
    val lensed = handler.imap[Int](_ - 1)(_ + 1)

    var handlerValue: Int = -100
    var lensedValue: Int = -100

    handler(handlerValue = _)
    lensed(lensedValue = _)

    lensed.observer.onNext(15)
    lensedValue shouldBe 15
    handlerValue shouldBe 16

    handler.observer.onNext(12)
    lensedValue shouldBe 11
    handlerValue shouldBe 12
  }

  it should "transformHandler" in {
    val handler = Handler.create[Int].unsafeRunSync()
    val lensed = handler.transformHandler[Int](_.map(_ - 1))(_.map(_ + 1))

    var handlerValue: Int = -100
    var lensedValue: Int = -100

    handler(handlerValue = _)
    lensed(lensedValue = _)

    lensed.observer.onNext(15)
    lensedValue shouldBe 15
    handlerValue shouldBe 16

    handler.observer.onNext(12)
    lensedValue shouldBe 11
    handlerValue shouldBe 12
  }
}
