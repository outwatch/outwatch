package outwatch.helpers

import colibri.Observer

private[outwatch] class StatefulObserver[T] extends Observer[T] {
  private var observer: Observer[T] = Observer.empty
  def set(observer: Observer[T]) = this.observer = observer
  def unsafeOnNext(value: T) = this.observer.unsafeOnNext(value)
  def unsafeOnError(error: Throwable) = this.observer.unsafeOnError(error)
}
