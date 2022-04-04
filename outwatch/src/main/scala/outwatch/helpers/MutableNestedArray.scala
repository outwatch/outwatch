package outwatch.helpers

import scala.scalajs.js
import scala.scalajs.js.|

private[outwatch] final class MutableNestedArray[T] {
  private val array = new js.Array[T | MutableNestedArray[T]]

  // not safe if T = MutableNestedArray.
  @annotation.nowarn("msg=exhaustive")
  def foreach(f: T => Unit): Unit = array.foreach(a => (a: Any) match {
    case nested: MutableNestedArray[T@unchecked] => nested.foreach(f)
    case t: T@unchecked => f(t)
  })

  def forall(condition: T => Boolean): Boolean = !exists(t => !condition(t))
  def exists(condition: T => Boolean): Boolean = {
    foreach { t => if (condition(t)) return true }
    return false
  }

  @inline def push(value: T | MutableNestedArray[T]): Unit = { array.push(value); () }
  @inline def clear(): Unit = array.clear()
  @inline def isEmpty: Boolean = array.isEmpty

  def toFlatArray: js.Array[T] = {
    val flatArray = new js.Array[T]
    foreach { t =>
      flatArray.push(t)
      ()
    }
    flatArray
  }
}
