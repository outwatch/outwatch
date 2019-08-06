package outwatch

import monix.execution.Ack 
import monix.reactive.subjects.PublishSubject
import monix.reactive.{Observable, Observer}

import scala.concurrent.Future

trait ObserverOps {
  implicit class RichObserver[I](observer: Observer[I]) {
    def redirect[I2](f: Observable[I2] => Observable[I]): ConnectableObserver[I2] = {
      val subject = PublishSubject[I2]
      val transformed = f(subject)
      new ConnectableObserver[I2](subject, implicit scheduler => transformed.subscribe(observer))
    }

    def redirectMap[I2](f: I2 => I): Observer[I2] = new Observer[I2] {
      override def onNext(elem: I2): Future[Ack] = observer.onNext(f(elem))
      override def onError(ex: Throwable): Unit = observer.onError(ex)
      override def onComplete(): Unit = observer.onComplete()
    }

    def redirectMapMaybe[I2](f: I2 => Option[I]): Observer[I2] = new Observer[I2] {
      override def onNext(elem: I2): Future[Ack] = f(elem).fold[Future[Ack]](Ack.Continue)(observer.onNext(_))
      override def onError(ex: Throwable): Unit = observer.onError(ex)
      override def onComplete(): Unit = observer.onComplete()
    }

    def redirectCollect[I2](f: PartialFunction[I2, I]): Observer[I2] = redirectMapMaybe(f.lift)
    def redirectFilter(f: I => Boolean): Observer[I] = redirectMapMaybe(e => Some(e).filter(f))
  }
}

