package outwatch.reactive

import scala.scalajs.js

private[reactive] object JSArrayHelper {
  def removeElement[T](array: js.Array[T])(element: T): Unit = {
    val index = array.indexOf(element)
    if (index != -1) array.splice(index, deleteCount = 1)
    ()
  }
}
