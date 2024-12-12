package outwatch.helpers

import scala.scalajs.js
import scala.scalajs.js.|

private[outwatch] final class MutableNestedArray[T] {
  private val array = new js.Array[T | MutableNestedArray[T]]

  // not safe if T = MutableNestedArray.
  @annotation.nowarn("msg=exhaustive")
  def foreach(f: T => Unit): Unit = {
    var i = 0
    while (i < array.length) {
      val a = array(i)
      (a: Any) match {
        case nested: MutableNestedArray[T @unchecked] => nested.foreach(f)
        case t: T @unchecked                          => f(t)
      }

      i += 1
    }
  }

  def forall(condition: T => Boolean): Boolean = !exists(t => !condition(t))

  @annotation.nowarn("msg=exhaustive")
  def exists(condition: T => Boolean): Boolean = {
    var i = 0
    while (i < array.length) {
      val t = array(i)

      val result = (t: Any) match {
        case nested: MutableNestedArray[T @unchecked] => nested.exists(condition)
        case t: T @unchecked                          => condition(t)
      }

      if (result) return true

      i += 1
    }

    false
  }

  @inline def push(value: T | MutableNestedArray[T]): Unit = array.push(value): Unit
  @inline def clear(): Unit                                = array.clear()
  @inline def isEmpty: Boolean                             = array.isEmpty

  def toFlatArray: js.Array[T] = {
    val flatArray = new js.Array[T]
    foreach { t =>
      flatArray.push(t): Unit
    }
    flatArray
  }
}
