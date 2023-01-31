package outwatch

import colibri._
import outwatch.implicits._
import outwatch.dsl._

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ImplicitsSpec extends AnyFlatSpec with Matchers {

  "Functor" should "apply on VNode" in {
    val observable = Observable(div("hallo"))

    val observableApplied = observable.apply(cls := "nee")

    val node = observableApplied.syncLatestSyncIO.unsafeRunSync()

    node.toList.flatMap(_.asInstanceOf[BasicVNode].modifiers.toList) shouldBe List(VMod("hallo"), cls := "nee")
  }

  it should "apply on VNode nested" in {
    val observable = Observable(Option(div("hallo")))

    val observableApplied = observable.apply(cls := "nee")

    val node = observableApplied.syncLatestSyncIO.unsafeRunSync().flatten

    node.toList.flatMap(_.asInstanceOf[BasicVNode].modifiers.toList) shouldBe List(VMod("hallo"), cls := "nee")
  }
}
