package outwatch.dom.helpers

import monix.execution.Cancelable

class QueuedCancelable extends Cancelable {
  private var queue: List[Cancelable] = Nil

  def enqueue(cancelable: Cancelable) = queue :+= cancelable
  def dequeue(): Cancelable = {
    if (queue.isEmpty) Cancelable.empty
    else {
      val res = queue.head
      queue = queue.tail
      res
    }
  }

  def cancel() = {
    queue.foreach(_.cancel())
    queue = Nil
  }
}
