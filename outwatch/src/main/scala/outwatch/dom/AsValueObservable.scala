package outwatch.dom

import monix.execution.Scheduler
import monix.reactive.Observable
import monix.reactive.subjects.Var

trait AsValueObservable[-F[_]] {
  def as[T](stream: F[T]): ValueObservable[T]
}

trait AsValueObservableInstances0 {
  implicit object observable extends AsValueObservable[Observable] {
    @inline def as[T](stream: Observable[T]): ValueObservable[T] = ValueObservable.from(stream)
  }
}

object AsValueObservable extends AsValueObservableInstances0  {
  implicit object valueObservable extends AsValueObservable[ValueObservable] {
    @inline def as[T](stream: ValueObservable[T]): ValueObservable[T] = stream
  }

  implicit object variable extends AsValueObservable[Var] {
    @inline def as[T](stream: Var[T]): ValueObservable[T] = ValueObservable.from(stream)
  }
}

