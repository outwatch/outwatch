package outwatch.helpers

import colibri.Observer

private[outwatch] class StatefulObserver[T] extends Observer[T] {
  private var observer: Observer[T] = Observer.empty
  def set(observer: Observer[T]) = this.observer = observer
  def onNext(value: T) = this.observer.onNext(value)
  def onError(error: Throwable) = this.observer.onError(error)
}
