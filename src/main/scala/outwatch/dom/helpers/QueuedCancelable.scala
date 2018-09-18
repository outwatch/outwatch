package outwatch.dom.helpers

import monix.execution.Cancelable
import scala.scalajs.js

class QueuedCancelable extends Cancelable {
  private val queue: js.Array[Cancelable] = new js.Array()

  def enqueue(cancelable: Cancelable): Unit = queue.push(cancelable)
  def dequeue(): Cancelable = {
    val head = queue.shift()
    if (head == null) Cancelable.empty
    else head
  }

  def cancel() = {
    queue.foreach(_.cancel())
    queue.clear()
  }
}
