package outwatch.dom

import cats.Functor
import monix.reactive.Observable

trait ValueObservable[+T] { self =>
  def observable: Observable[T]
  def value: Option[T]
  def map[B](f: T => B): ValueObservable[B] = new ValueObservable[B] {
    def observable: Observable[B] = self.observable.map(f)
    def value: Option[B] = self.value.map(f)
  }
}

object ValueObservable {
  implicit def functor: Functor[ValueObservable] = new Functor[ValueObservable] {
    override def map[A, B](fa: ValueObservable[A])(f: A => B): ValueObservable[B] = fa.map(f)
  }

  def apply[T](stream: Observable[T]): ValueObservable[T] = new ValueObservable[T] {
    override def observable: Observable[T] = stream
    override def value: Option[T] = None
  }

  def apply[T](stream: Observable[T], initialValue: T): ValueObservable[T] = new ValueObservable[T] {
    override def observable: Observable[T] = stream
    override def value: Option[T] = Some(initialValue)
  }

  @inline def apply[F[_], T](stream: F[T])(implicit asValueObservable: AsValueObservable[F]): ValueObservable[T] = asValueObservable.as(stream)
}
