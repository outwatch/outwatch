package outwatch

import org.scalatest.{FlatSpec, Matchers}
import outwatch.reactive._

class SourceStreamSpec extends FlatSpec with Matchers {

  "SourceStream" should "map" in {
    var mapped = List.empty[Int]
    var received = List.empty[Int]
    val stream = SourceStream.fromIterable(Seq(1,2,3)).map { x => mapped ::= x; x }

    mapped shouldBe List.empty

    stream.subscribe(SinkObserver.create[Int](received ::= _))

    mapped shouldBe List(3,2,1)
    received shouldBe List(3,2,1)

    stream.subscribe(SinkObserver.create[Int](received ::= _))

    mapped shouldBe List(3,2,1,3,2,1)
    received shouldBe List(3,2,1,3,2,1)
  }

  it should "dropWhile" in {
    var mapped = List.empty[Int]
    var received = List.empty[Int]
    val stream = SourceStream.fromIterable(Seq(1,2,3,4)).dropWhile { x => mapped ::= x; x < 3 }

    mapped shouldBe List.empty

    stream.subscribe(SinkObserver.create[Int](received ::= _))

    mapped shouldBe List(3,2,1)
    received shouldBe List(4,3)
  }

  it should "share" in {
    var mapped = List.empty[Int]
    var received = List.empty[Int]
    val handler = SinkSourceHandler[Int]
    val stream = SourceStream.merge(handler, SourceStream.fromIterable(Seq(1,2,3))).map { x => mapped ::= x; x }.share

    mapped shouldBe List.empty

    val sub1 = stream.subscribe(SinkObserver.create[Int](received ::= _))

    mapped shouldBe List(3,2,1)
    received shouldBe List(3,2,1)

    val sub2 = stream.subscribe(SinkObserver.create[Int](received ::= _))

    mapped shouldBe List(3,2,1)
    received shouldBe List(3,2,1)

    handler.onNext(4)

    mapped shouldBe List(4,3,2,1)
    received shouldBe List(4,4,3,2,1)

    sub1.cancel()

    handler.onNext(5)

    mapped shouldBe List(5,4,3,2,1)
    received shouldBe List(5,4,4,3,2,1)

    sub2.cancel()

    handler.onNext(6)

    mapped shouldBe List(5,4,3,2,1)
    received shouldBe List(5,4,4,3,2,1)
  }
}
