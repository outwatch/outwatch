package outwatch.util

import outwatch.Sink
import outwatch.dom.createHandler
import rxscalajs.Observable
import rxscalajs.subscription.Subscription

import scala.language.implicitConversions

final case class Store[State, Action](initialState: State, reducer: (State, Action) => State) {
  val sink: Observable[Action] with Sink[Action] = createHandler[Action]()
  val source: Observable[State] = sink
    .scan(initialState)(reducer)
    .startWith(initialState)

  def subscribe(f: State => Unit): Subscription = source.subscribe(f)
}

object Store {
  implicit def toSink[Action](store: Store[_, Action]): Sink[Action] = store.sink
  implicit def toSource[State](store: Store[State, _]): Observable[State] = store.source
}
