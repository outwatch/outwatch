package outwatch.dom

import monix.execution.Ack
import monix.reactive.Observer
import monix.reactive.subjects.Var

trait AsObserver[-F[_]] {
  def as[T](stream: F[_ >: T]): Observer[T]
}

object AsObserver {
  implicit object variable extends AsObserver[Var] {
    def as[T](stream: Var[_ >: T]): Observer[T] = new Observer.Sync[T] {
      override def onNext(elem: T): Ack = stream := elem
      override def onError(ex: Throwable): Unit = throw ex
      override def onComplete(): Unit = ()
    }
  }
}
