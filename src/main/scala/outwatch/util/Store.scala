package outwatch.util

import cats.effect.IO
import outwatch.{Pipe, Sink}
import outwatch.dom._
import outwatch.dom.helpers.STRef
import rxscalajs.Observable
import rxscalajs.subscription.Subscription

import scala.language.implicitConversions


final case class Store[State, Action](initialState: State,
                                           reducer: (State, Action) => (State, Option[IO[Action]]),
                                           handler: Pipe[Action, Action]) {
  val sink: Sink[Action] = handler
  val source: Observable[State] = handler
    .scan(initialState)(fold)
    .startWith(initialState)
    .share

  private def fold(state: State, action: Action): State = {
    val (newState, next) = reducer(state, action)

    next.foreach(_.unsafeRunAsync {
      case Left(e) => sink.observer.error(e.toString)
      case Right(r) => sink.observer.next(r)
    })

    newState
  }

  def subscribe(f: State => IO[Unit]): IO[Subscription] =
    IO(source.subscribe(f andThen(_.unsafeRunSync())))
}

object Store {
  implicit def toPipe[State, Action](store: Store[State, Action]): Pipe[Action, State] =
    Pipe(store.sink, store.source)

  private val storeRef = STRef.empty

  def renderWithStore[S, A](initialState: S, reducer: (S, A) => (S, Option[IO[A]]), selector: String, root: VNode): IO[Unit] = for {
    handler <- Handler.create[A]
    store <- IO(Store(initialState, reducer, handler))
    _ <- storeRef.asInstanceOf[STRef[Store[S, A]]].put(store)
    _ <- OutWatch.render(selector, root)
  } yield ()

  def getStore[S, A]: IO[Store[S, A]] =
    storeRef.asInstanceOf[STRef[Store[S, A]]].getOrThrow(NoStoreException)

  private object NoStoreException extends
    Exception("Application was rendered without specifying a Store, please use Outwatch.renderWithStore instead")

}
