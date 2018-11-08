package outwatch

import monix.eval.Task
import monix.execution.Ack
import monix.reactive.Observable
import monix.reactive.subjects.{BehaviorSubject, PublishSubject, ReplaySubject}
import outwatch.dom.ValueObservable

class ValueObservableSpec extends JSDomSpec {

  "ValueObservable" should "map" in {

    var mappedValue = List.empty[Int]
    var currentValue = List.empty[Int]

    val obs = ValueObservable(1, 2, 3)
    val m = obs.map{ x => mappedValue :+= x; x }
    val v = m.value()
    v.head shouldBe Some(1)
    v.tail.foreach { x => currentValue :+= x }

    mappedValue shouldBe List(1, 2, 3)
    currentValue shouldBe List(2, 3)
  }

  it should "flatMap" in {

    var mappedValue = List.empty[Int]
    var currentValue = List.empty[Int]

    val obs = ValueObservable(1, 2, 3)
    val m = obs.flatMap{ x => mappedValue :+= x; ValueObservable(x, x * 2) }
    val v = m.value()
    v.head shouldBe Some(1)
    v.tail.foreach { x => currentValue :+= x }

    mappedValue shouldBe List(1, 2, 3)
    currentValue shouldBe List(2, 2, 4, 3, 6)
  }

  it should "scan" in {

    var mappedValue = List.empty[Int]
    var currentValue = List.empty[String]

    val obs = ValueObservable(1, 2, 3)
    val m = obs.scan(""){ (x,y) => mappedValue :+= y; x + "+" + y.toString  }
    val v = m.value()
    v.head shouldBe Some("+1")
    v.tail.foreach { x => currentValue :+= x }

    mappedValue shouldBe List(1, 2, 3)
    currentValue shouldBe List("+1+2", "+1+2+3")
  }

  it should "distinctUntilChanged" in {

    var mappedValue = List.empty[Int]
    var currentValue = List.empty[Int]

    val obs = ValueObservable(0, 0, 1, 2, 2, 1, 2, 3, 3, 3)
    val m = obs.distinctUntilChanged(cats.Eq.fromUniversalEquals).map { x => mappedValue :+= x; x }
    val v = m.value()
    v.head shouldBe Some(0)
    v.tail.foreach { x => currentValue :+= x }

    mappedValue shouldBe List(0, 1, 2, 1, 2, 3)
    currentValue shouldBe List(1, 2, 1, 2, 3)
  }

  it should "map empty" in {

    var mappedValue = List.empty[Int]
    var currentValue = List.empty[Int]

    val obs = ValueObservable.empty
    val m = obs.map{ x => mappedValue :+= x; x }
    val v = m.value()
    v.head shouldBe None
    v.tail.foreach { x => currentValue :+= x }

    mappedValue shouldBe List.empty
    currentValue shouldBe List.empty
  }

  it should "map single" in {

    var mappedValue = List.empty[Int]
    var currentValue = List.empty[Int]

    val obs = ValueObservable(1)
    val m = obs.map{ x => mappedValue :+= x; x }
    val v = m.value()
    v.head shouldBe Some(1)
    v.tail.foreach { x => currentValue :+= x }

    mappedValue shouldBe List(1)
    currentValue shouldBe List.empty
  }

  it should "map dual subscriptions" in {

    var mappedValue = List.empty[Int]
    var currentValue = List.empty[Int]
    var currentValue2 = List.empty[Int]

    val obs = ValueObservable(1,2,3)
    val m = obs.map{ x => mappedValue :+= x; x }
    val v = m.value()
    v.head shouldBe Some(1)
    v.tail.foreach { x => currentValue :+= x }
    v.tail.foreach { x => currentValue2 :+= x }

    mappedValue shouldBe List(1,2,3,2,3)
    currentValue shouldBe List(2,3)
    currentValue2 shouldBe List(2,3)
  }

  it should "foreach" in {

    var mappedValue = List.empty[Int]
    var currentValue = List.empty[Int]
    var currentValue2 = List.empty[Int]

    val obs = ValueObservable(1,2,3)
    val m = obs.map{ x => mappedValue :+= x; x }
    m.foreach { x => currentValue :+= x }
    m.foreach { x => currentValue2 :+= x }

    mappedValue shouldBe List(1,2,3,1,2,3)
    currentValue shouldBe List(1,2,3)
    currentValue2 shouldBe List(1,2,3)
  }

  "BehaviorProHandler" should "map" in {
    var mappedValue = List.empty[Int]
    var currentValue = List.empty[Int]
    var currentValue2 = List.empty[Int]

    val obs = Handler.unsafe[Int](1)
    val m = obs.map{ x => mappedValue :+= x; x }
    m.foreach { x => currentValue :+= x }

    for {
      Ack.Continue <- obs.onNext(2)
      _ = m.foreach { x => currentValue2 :+= x }
      Ack.Continue <- obs.onNext(3)
      Ack.Continue <- obs.onNext(4)
    } yield {
      mappedValue shouldBe List(1,2,2,3,3,4,4)
      currentValue shouldBe List(1,2,3,4)
      currentValue2 shouldBe List(2,3,4)
    }
  }

  it should "map dual subscriptions" in {

    var mappedValue = List.empty[Int]
    var currentValue = List.empty[Int]
    var currentValue2 = List.empty[Int]

    val obs = Handler.unsafe[Int](1)
    val m = obs.map{ x => mappedValue :+= x; x }
    m.foreach { x => currentValue :+= x }

    for {
      Ack.Continue <- obs.onNext(2)
      _ = m.foreach { x => currentValue2 :+= x }
      Ack.Continue <- obs.onNext(3)
      Ack.Continue <- obs.onNext(4)
    } yield {
      mappedValue shouldBe List(1,2,2,3,3,4,4)
      currentValue shouldBe List(1,2,3,4)
      currentValue2 shouldBe List(2,3,4)
    }
  }

  it should "scan dual subscriptions" in {
    var mappedValue = List.empty[Int]
    var currentValue = List.empty[String]
    var currentValue2 = List.empty[String]

    val obs = Handler.unsafe[Int](1)
    val m = obs.scan(""){ (x,y) => mappedValue :+= y; x + "+" + y.toString  }
    m.foreach { x => currentValue :+= x }

    for {
      Ack.Continue <- obs.onNext(2)
      _ = m.foreach { x => currentValue2 :+= x }
      Ack.Continue <- obs.onNext(3)
      Ack.Continue <- obs.onNext(4)
    } yield {
      mappedValue shouldBe List(1, 2, 2, 3, 3, 4, 4)
      currentValue shouldBe List("+1", "+1+2", "+1+2+3", "+1+2+3+4")
      currentValue2 shouldBe List("+2", "+2+3", "+2+3+4")
    }
  }

  it should "map dual subscriptions shared" in {
    var mappedValue = List.empty[Int]
    var currentValue = List.empty[Int]
    var currentValue2 = List.empty[Int]

    val obs = Handler.unsafe[Int](1)
    val m = obs.map{ x => mappedValue :+= x; x }.share
    m.foreach { x => currentValue :+= x }

    for {
      Ack.Continue <- obs.onNext(2)
      _ = m.foreach { x => currentValue2 :+= x }
      Ack.Continue <- obs.onNext(3)
      Ack.Continue <- obs.onNext(4)
    } yield {
      mappedValue shouldBe List(1,2,3,4)
      currentValue shouldBe List(1,2,3,4)
      currentValue2 shouldBe List(3,4)
    }
  }

  it should "scan dual subscriptions shared" in {
    var mappedValue = List.empty[Int]
    var currentValue = List.empty[String]
    var currentValue2 = List.empty[String]

    val obs = Handler.unsafe[Int](1)
    val m = obs.scan(""){ (x,y) => mappedValue :+= y; x + "+" + y.toString  }.share
    m.foreach { x => currentValue :+= x }

    for {
      Ack.Continue <- obs.onNext(2)
      _ = m.foreach { x => currentValue2 :+= x }
      Ack.Continue <- obs.onNext(3)
      Ack.Continue <- obs.onNext(4)
    } yield {
      mappedValue shouldBe List(1, 2, 3, 4)
      currentValue shouldBe List("+1", "+1+2", "+1+2+3", "+1+2+3+4")
      currentValue2 shouldBe List("+1+2+3", "+1+2+3+4")
    }
  }

  it should "map dual direct subscriptions shared" in {
    var mappedValue = List.empty[Int]
    var currentValue = List.empty[Int]
    var currentValue2 = List.empty[Int]

    val obs = Handler.unsafe[Int](1)
    val m = obs.map{ x => mappedValue :+= x; x }.share
    m.foreach { x => currentValue :+= x }
    m.foreach { x => currentValue2 :+= x }

    for {
      Ack.Continue <- obs.onNext(2)
      Ack.Continue <- obs.onNext(3)
      Ack.Continue <- obs.onNext(4)
    } yield {
      mappedValue shouldBe List(1,2,3,4)
      currentValue shouldBe List(1,2,3,4)
      currentValue2 shouldBe List(2,3,4)
    }
  }

  it should "scan dual direct subscriptions shared" in {
    var mappedValue = List.empty[Int]
    var currentValue = List.empty[String]
    var currentValue2 = List.empty[String]

    val obs = Handler.unsafe[Int](1)
    val m = obs.scan(""){ (x,y) => mappedValue :+= y; x + "+" + y.toString  }.share
    m.foreach { x => currentValue :+= x }
    m.foreach { x => currentValue2 :+= x }

    for {
      Ack.Continue <- obs.onNext(2)
      Ack.Continue <- obs.onNext(3)
      Ack.Continue <- obs.onNext(4)
    } yield {
      mappedValue shouldBe List(1, 2, 3, 4)
      currentValue shouldBe List("+1", "+1+2", "+1+2+3", "+1+2+3+4")
      currentValue2 shouldBe List("+1+2", "+1+2+3", "+1+2+3+4")
    }
  }

  it should "withLatestFrom" in {
    var mappedValue = List.empty[Int]
    var currentValue = List.empty[(Int,Int)]

    val obs = Handler.unsafe[Int](1)
    val obs2 = Handler.unsafe[Int]
    val m = obs.withLatestFrom(obs2){ (x,y) => mappedValue :+= x; (x,y) }
    m.foreach { x => currentValue :+= x }

    for {
      Ack.Continue <- obs.onNext(2)
      Ack.Continue <- obs2.onNext(-1)
      Ack.Continue <- obs2.onNext(-2)
      Ack.Continue <- obs.onNext(3)
      Ack.Continue <- obs2.onNext(-4)
    } yield {
      mappedValue shouldBe List(3)
      currentValue shouldBe List((3,-2))
    }
  }

  it should "withLatestFrom with seed" in {
    var mappedValue = List.empty[Int]
    var currentValue = List.empty[(Int,Int)]

    val obs = Handler.unsafe[Int](1)
    val obs2 = Handler.unsafe[Int](0)
    val m = obs.withLatestFrom(obs2){ (x,y) => mappedValue :+= x; (x,y) }
    m.foreach { x => currentValue :+= x }

    for {
      Ack.Continue <- obs.onNext(2)
      Ack.Continue <- obs2.onNext(-1)
      Ack.Continue <- obs2.onNext(-2)
      Ack.Continue <- obs.onNext(3)
    } yield {
      mappedValue shouldBe List(1,2,3)
      currentValue shouldBe List((1,0),(2,0),(3,-2))
    }
  }

  it should "combineLatest" in {
    var mappedValue = List.empty[Int]
    var currentValue = List.empty[(Int,Int)]

    val obs = Handler.unsafe[Int](1)
    val obs2 = Handler.unsafe[Int]
    val m = obs.combineLatestMap(obs2){ (x,y) => mappedValue :+= x; (x,y) }
    m.foreach { x => currentValue :+= x }

    for {
      Ack.Continue <- obs.onNext(2)
      Ack.Continue <- obs2.onNext(-1)
      Ack.Continue <- obs2.onNext(-2)
      Ack.Continue <- obs.onNext(3)
    } yield {
      mappedValue shouldBe List(2,2,3)
      currentValue shouldBe List((2,-1),(2,-2),(3,-2))
    }
  }

  it should "combineLatest with seed" in {
    var mappedValue = List.empty[Int]
    var currentValue = List.empty[(Int,Int)]

    val obs = Handler.unsafe[Int](1)
    val obs2 = Handler.unsafe[Int](0)
    val m = obs.combineLatestMap(obs2){ (x,y) => mappedValue :+= x; (x,y) }
    m.foreach { x => currentValue :+= x }

    for {
      Ack.Continue <- obs.onNext(2)
      Ack.Continue <- obs2.onNext(-1)
      Ack.Continue <- obs2.onNext(-2)
      Ack.Continue <- obs.onNext(3)
    } yield {
      mappedValue shouldBe List(1,2,2,2,3)
      currentValue shouldBe List((1,0),(2,0),(2,-1),(2,-2),(3,-2))
    }
  }

  it should "concat" in {
    var currentValue = List.empty[Int]

    val innerObs = Handler.unsafe[Int]
    val obs = Handler.unsafe[ValueObservable[Int]](innerObs)
    val m = obs.concat
    m.foreach { x => currentValue :+= x }

    for {
      Ack.Continue <- innerObs.onNext(13)
      Ack.Continue <- innerObs.onNext(14)
      f0 = obs.onNext(ValueObservable(0))
      Ack.Continue <- innerObs.onNext(15)
      f1 = obs.onNext(ValueObservable(1))
      _ = innerObs.onComplete()
      Ack.Continue <- f0
      Ack.Continue <- f1
      Ack.Continue <- obs.onNext(ValueObservable(2))
    } yield {
      currentValue shouldBe List(13, 14, 15, 0, 1, 2)
    }
  }

  it should "merge" in {
    var currentValue = List.empty[Int]

    val innerObs = Handler.unsafe[Int]
    val obs = Handler.unsafe[ValueObservable[Int]](innerObs)
    val m = obs.merge
    m.foreach { x => currentValue :+= x }

    for {
      Ack.Continue <- innerObs.onNext(13)
      Ack.Continue <- innerObs.onNext(14)
      f0 = obs.onNext(ValueObservable(0))
      Ack.Continue <- innerObs.onNext(15)
      f1 = obs.onNext(ValueObservable(1))
      _ = innerObs.onComplete()
      Ack.Continue <- f0
      Ack.Continue <- f1
      Ack.Continue <- obs.onNext(ValueObservable(2))
    } yield {
      currentValue shouldBe List(13, 14, 0, 15, 1, 2)
    }
  }

  it should "switch" in {
    var currentValue = List.empty[Int]

    val innerObs = Handler.unsafe[Int]
    val obs = Handler.unsafe[ValueObservable[Int]](innerObs)
    val m = obs.switch
    m.foreach { x => currentValue :+= x }

    for {
      Ack.Continue <- innerObs.onNext(13)
      Ack.Continue <- innerObs.onNext(14)
      Ack.Continue <- obs.onNext(ValueObservable(0))
      Ack.Continue <- innerObs.onNext(15)
      Ack.Continue <- obs.onNext(ValueObservable(1))
      _ = innerObs.onComplete()
      Ack.Continue <- obs.onNext(ValueObservable(2))
    } yield {
      currentValue shouldBe List(13, 14, 0, 1, 2)
    }
  }

  it should "takeWhile with false seed" in {
    var currentValue = List.empty[Int]

    val obs = Handler.unsafe[Int](2)
    val m = obs.takeWhile(_ < 0)
    m.foreach { x => currentValue :+= x }

    for {
      Ack.Continue <- obs.onNext(-3)
      Ack.Continue <- obs.onNext(4)
      Ack.Continue <- obs.onNext(1)
    } yield {
      currentValue shouldBe List.empty
    }
  }

  it should "takeWhile with true seed" in {
    var currentValue = List.empty[Int]

    val obs = Handler.unsafe[Int](-1)
    val m = obs.takeWhile(_ < 0)
    m.foreach { x => currentValue :+= x }

    for {
      Ack.Continue <- obs.onNext(-3)
      Ack.Continue <- obs.onNext(4)
      Ack.Continue <- obs.onNext(1)
    } yield {
      currentValue shouldBe List(-1, -3)
    }
  }

  it should "takeWhile without seed" in {
    var currentValue = List.empty[Int]

    val obs = Handler.unsafe[Int]
    val m = obs.takeWhile(_ < 0)
    m.foreach { x => currentValue :+= x }

    for {
      Ack.Continue <- obs.onNext(-3)
      Ack.Continue <- obs.onNext(4)
      Ack.Continue <- obs.onNext(1)
    } yield {
      currentValue shouldBe List(-3)
    }
  }

  it should "doOnNext" in {
    var doValue = List.empty[Int]
    var currentValue = List.empty[Int]

    val obs = Handler.unsafe[Int](1)
    val m = obs.doOnNext(x => Task { doValue :+= x })
    m.foreach { x => currentValue :+= x }

    for {
      Ack.Continue <- obs.onNext(2)
      Ack.Continue <- obs.onNext(3)
    } yield {
      currentValue shouldBe List(1,2,3)
      doValue shouldBe List(1,2,3)
    }
  }

  it should "doOnError" in {
    var doCounter = 0
    var currentValue = List.empty[Int]

    val obs = Handler.unsafe[Int](1)
    val m = obs.doOnError(_ => Task { doCounter += 1 })
    m.foreach { x => currentValue :+= x }

    for {
      Ack.Continue <- obs.onNext(2)
      Ack.Continue <- obs.onNext(3)
      _ = obs.onError(new Exception)
    } yield {
      currentValue shouldBe List(1,2,3)
      doCounter shouldBe 0
      obs.onError(new Exception)
      doCounter shouldBe 1
    }
  }

  it should "doOnComplete" in {
    var doCounter = 0
    var currentValue = List.empty[Int]

    val obs = Handler.unsafe[Int](1)
    val m = obs.doOnComplete(Task { doCounter += 1 })
    m.foreach { x => currentValue :+= x }

    for {
      Ack.Continue <- obs.onNext(2)
      Ack.Continue <- obs.onNext(3)
    } yield {
      currentValue shouldBe List(1,2,3)
      doCounter shouldBe 0
      obs.onComplete()
      doCounter shouldBe 1
    }
  }
}
