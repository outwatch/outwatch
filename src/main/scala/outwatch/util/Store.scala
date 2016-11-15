package outwatch.util

import outwatch.Sink
import outwatch.dom.createHandler
import rxscalajs.Observable

case class Store[T, U](initialState: T, reducer: (T,U) => T) {
  val sink = createHandler[U]
  val source = sink
    .scan(initialState)(reducer)
    .startWith(initialState)

  def subscribe(f: T => Unit) = source.subscribe(f)
}

object Store {
  implicit def toSink[U](store: Store[_,U]): Sink[U] = store.sink
  implicit def toSource[T](store: Store[T,_]): Observable[T] = store.source

}