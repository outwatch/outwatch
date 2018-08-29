package outwatch.dom

import cats.Functor
import monix.reactive.Observable
import monix.reactive.subjects.Var

trait ValueObservable[+T] {
  def observable: Observable[T]
  def value: Option[T]
}

object ValueObservable {
  implicit def functor: Functor[ValueObservable] = new Functor[ValueObservable] {
    override def map[A, B](fa: ValueObservable[A])(f: A => B): ValueObservable[B] = new ValueObservable[B] {
      def observable: Observable[B] = fa.observable.map(f)
      def value: Option[B] = fa.value.map(f)
    }
  }

  def apply[T](stream: Observable[T], initialValue: T): ValueObservable[T] = new ValueObservable[T] {
    override def observable: Observable[T] = stream
    override def value: Option[T] = Some(initialValue)
  }
  def apply[F[_], T](stream: F[T])(implicit asValueObservable: AsValueObservable[F]): ValueObservable[T] = asValueObservable.as(stream)
}

trait AsValueObservable[-F[_]] {
  def as[T](stream: F[T]): ValueObservable[T]
}

object AsValueObservable {
  implicit object valueObservable extends AsValueObservable[ValueObservable] {
    def as[T](stream: ValueObservable[T]): ValueObservable[T] = stream
  }

  implicit object observable extends AsValueObservable[Observable] {
    def as[T](stream: Observable[T]): ValueObservable[T] = new ValueObservable[T] {
      def observable: Observable[T] = stream
      def value: Option[T] = None
    }
  }

  implicit object variable extends AsValueObservable[Var] {
    def as[T](stream: Var[T]): ValueObservable[T] = new ValueObservable[T] {
      def observable: Observable[T] = stream.drop(1)
      def value: Option[T] = Some(stream.apply())
    }
  }
}
