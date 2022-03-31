package outwatch.helpers

import scala.scalajs.js
import scala.scalajs.js.|

private[outwatch] class MutableNestedArray[T] {
  private val array = new js.Array[T | MutableNestedArray[T]]

  // not safe if T = MutableNestedArray.
  @annotation.nowarn("msg=exhaustive")
  def foreach(f: T => Unit): Unit = array.foreach(a => (a: Any) match {
    case nested: MutableNestedArray[T@unchecked] => nested.foreach(f)
    case t: T@unchecked => f(t)
  })

  @inline def push(value: T | MutableNestedArray[T]): Unit = { array.push(value); () }
  @inline def clear(): Unit = array.clear()
  @inline def isEmpty: Boolean = array.isEmpty
  @inline def calculateLength(): Int = {
    var counter = 0
    foreach(_ => counter += 1)
    counter
  }
}
