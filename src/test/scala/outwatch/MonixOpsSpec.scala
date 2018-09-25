package outwatch

import monix.reactive.subjects.PublishSubject

class MonixOpsSpec extends JSDomSpec {

  "Observer" should "redirect" in {
    val subject = PublishSubject[Int]
    var currentValue = 0
    subject.foreach{currentValue = _}

    val redirected = subject.redirect[Int](_.map(_ + 1))
    redirected.connect()

    subject.onNext(5)
    assert(currentValue == 5)

    redirected.onNext(5)
    assert(currentValue == 6)
  }

  it should "redirectMap" in {
    val subject = PublishSubject[Int]
    var currentValue = 0
    subject.foreach{currentValue = _}

    val redirected = subject.redirectMap[Int](_ + 1)
    redirected.connect()

    subject.onNext(5)
    assert(currentValue == 5)

    redirected.onNext(5)
    assert(currentValue == 6)
  }


  "PublishSubject" should "transformObservable" in {
    val subject = PublishSubject[Int]
    val mapped = subject.transformObservable(_.map(_ + 1))
    var currentValue = 0
    mapped.foreach{currentValue = _}


    subject.onNext(5)
    assert(currentValue == 6)

    mapped.onNext(7)
    assert(currentValue == 8)
  }

  "Subject" should "lens" in {
    val handler = Handler.create[(String, Int)].unsafeRunSync()
    val lensed = handler.lens[Int](("harals", 0))(_._2)((tuple, num) => (tuple._1, num))
    lensed.connect()

    var handlerValue: (String, Int) = null
    var lensedValue: Int = -100

    handler(handlerValue = _)
    lensed(lensedValue = _)

    lensed.onNext(15)
    lensedValue shouldBe 15
    handlerValue shouldBe (("harals", 15))

    handler.onNext(("peter", 12))
    lensedValue shouldBe 12
    handlerValue shouldBe (("peter", 12))

    lensed.onNext(-1)
    lensedValue shouldBe -1
    handlerValue shouldBe (("peter", -1))
  }

  it should "mapObservable" in {
    val handler = Handler.create[Int].unsafeRunSync()
    val lensed = handler.mapObservable(_ - 1)

    var handlerValue: Int = -100
    var lensedValue: Int = -100

    handler(handlerValue = _)
    lensed(lensedValue = _)

    lensed.onNext(15)
    lensedValue shouldBe 14
    handlerValue shouldBe 15

    handler.onNext(12)
    lensedValue shouldBe 11
    handlerValue shouldBe 12
  }

  it should "transformObservable" in {
    val handler = Handler.create[Int].unsafeRunSync()
    val lensed = handler.transformObservable(_.map(_ - 1))

    var handlerValue: Int = -100
    var lensedValue: Int = -100

    handler(handlerValue = _)
    lensed(lensedValue = _)

    lensed.onNext(15)
    lensedValue shouldBe 14
    handlerValue shouldBe 15

    handler.onNext(12)
    lensedValue shouldBe 11
    handlerValue shouldBe 12
  }

  it should "mapObserver" in {
    val handler = Handler.create[Int].unsafeRunSync()
    val lensed = handler.mapObserver[Int](_ + 1)
    lensed.connect()

    var handlerValue: Int = -100
    var lensedValue: Int = -100

    handler(handlerValue = _)
    lensed(lensedValue = _)

    lensed.onNext(15)
    lensedValue shouldBe 16
    handlerValue shouldBe 16

    handler.onNext(12)
    lensedValue shouldBe 12
    handlerValue shouldBe 12
  }

  it should "transformObserver" in {
    val handler = Handler.create[Int].unsafeRunSync()
    val lensed = handler.transformObserver[Int](_.map(_ + 1))
    lensed.connect()

    var handlerValue: Int = -100
    var lensedValue: Int = -100

    handler(handlerValue = _)
    lensed(lensedValue = _)

    lensed.onNext(15)
    lensedValue shouldBe 16
    handlerValue shouldBe 16

    handler.onNext(12)
    lensedValue shouldBe 12
    handlerValue shouldBe 12
  }

  it should "mapSubject" in {
    val handler = Handler.create[Int].unsafeRunSync()
    val lensed = handler.mapHandler[Int](_ + 1)(_ - 1)
    lensed.connect()

    var handlerValue: Int = -100
    var lensedValue: Int = -100

    handler(handlerValue = _)
    lensed(lensedValue = _)

    lensed.onNext(15)
    lensedValue shouldBe 15
    handlerValue shouldBe 16

    handler.onNext(12)
    lensedValue shouldBe 11
    handlerValue shouldBe 12
  }

  it should "transformSubject" in {
    val handler = Handler.create[Int].unsafeRunSync()
    val lensed = handler.transformHandler[Int](_.map(_ + 1))(_.map(_ - 1))
    lensed.connect()

    var handlerValue: Int = -100
    var lensedValue: Int = -100

    handler(handlerValue = _)
    lensed(lensedValue = _)

    lensed.onNext(15)
    lensedValue shouldBe 15
    handlerValue shouldBe 16

    handler.onNext(12)
    lensedValue shouldBe 11
    handlerValue shouldBe 12
  }
}
