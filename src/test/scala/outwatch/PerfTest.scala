package outwatch

import cats.effect.IO
import monix.execution.Ack.Continue
import monix.reactive.subjects.{BehaviorSubject, PublishSubject, Var}
import monix.reactive.Observable
import org.scalajs.dom.{document, html}
import outwatch.dom.helpers._
import outwatch.dom._
import outwatch.dom.dsl._
import outwatch.Deprecated.IgnoreWarnings.initEvent
import snabbdom.{DataObject, hFunction}
import org.scalajs.dom.window.localStorage

import scala.collection.immutable.Seq
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.collection.mutable

class PerfTest extends JSDomSpec {

  "Perf" should "be" in {
    val elemId = "msg"

    val handler = Handler.create[Int](0).unsafeRunSync

    val vtree = div(
      id := elemId,
      handler.map(div(_))
    )

    val node = document.createElement("div")
    document.body.appendChild(node)

    val t = System.currentTimeMillis

    OutWatch.renderInto(node, vtree).unsafeRunSync()

    (0 to 10000).foreach { i =>
      handler.onNext(i)
    }

    val t2 = System.currentTimeMillis

    (t2 - t) shouldBe 1
  }
}
