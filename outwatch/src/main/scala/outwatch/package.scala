import monix.reactive.{Observer, Observable}

package object outwatch extends ObserverOps {
  @deprecated("Use ObserverBuilder instead", "")
  val Sink = ObserverBuilder

  type ProHandler[-I, +O] = Observable[O] with Observer[I]
  type Handler[T] = ProHandler[T,T]

}
