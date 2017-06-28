package outwatch.util

import outwatch.Sink
import outwatch.dom.{Handler, createHandler}
import rxscalajs.Observable
import rxscalajs.subscription.Subscription

import scala.language.implicitConversions

final case class Store[State, Action](initialState: State, reducer: (State, Action) => State) {
  private val handler: Handler[Action] = createHandler[Action]()
  val sink: Sink[Action] = handler
  val source: Observable[State] = handler
    .scan(initialState)(reducer)
    .startWith(initialState)

  def subscribe(f: State => Unit): Subscription = source.subscribe(f)
}

object Store {
  implicit def toSink[Action](store: Store[_, Action]): Sink[Action] = store.sink
  implicit def toSource[State](store: Store[State, _]): Observable[State] = store.source
}
