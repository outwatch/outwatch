package outwatch.dom.helpers

import scala.scalajs.js
import scala.scalajs.js.|

private[outwatch] class MutableNestedArray[T] {
  private val array = new js.Array[T | MutableNestedArray[T]]

  // not safe if T = MutableNestedArray.
  @inline def foreach(f: T => Unit): Unit = array.foreach(a => (a: Any) match {
    case nested: MutableNestedArray[T] => nested.foreach(f)
    case t: T@unchecked => f(t)
  })

  @inline def push(value: T | MutableNestedArray[T]): Unit = { array.push(value); () }
  @inline def clear(): Unit = array.clear()
  @inline def isEmpty(): Boolean = array.isEmpty
}
