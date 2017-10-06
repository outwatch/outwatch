package outwatch.util

import cats.effect.IO
import outwatch.Sink
import outwatch.dom.createHandler
import rxscalajs.Observable
import rxscalajs.subscription.Subscription

import scala.language.implicitConversions

final case class Store[State, Action](initialState: State, reducer: (State, Action) => State) {
  private val handler: Observable[Action] with Sink[Action] = createHandler[Action]().unsafeRunSync() //TODO why unsafe?
  val sink: Sink[Action] = handler
  val source: Observable[State] = handler
    .scan(initialState)(reducer)
    .startWith(initialState)

  def subscribe(f: State => IO[Unit]): Subscription = source.subscribe(s =>f(s).unsafeRunSync())
}

object Store {
  implicit def toSink[Action](store: Store[_, Action]): Sink[Action] = store.sink
  implicit def toSource[State](store: Store[State, _]): Observable[State] = store.source
}
