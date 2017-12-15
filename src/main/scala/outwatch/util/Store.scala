package outwatch.util

import cats.effect.IO
import monix.execution.Scheduler
import monix.execution.{Ack, Cancelable}
import outwatch.dom.{Observable, OutWatch, VNode}
import outwatch.{Handler, Pipe, Sink}
import outwatch.dom.helpers.STRef

import scala.concurrent.Future



final case class Store[State, Action](initialState: State,
                                           reducer: (State, Action) => (State, Option[IO[Action]]),
                                           handler: Pipe[Action, Action])(implicit s: Scheduler) {
  val sink: Sink[Action] = handler
  val source: Observable[State] = handler
    .scan(initialState)(fold)
    .startWith(Seq(initialState))
    .share

  private def fold(state: State, action: Action): State = {
    val (newState, next) = reducer(state, action)

    next.foreach(_.unsafeRunAsync {
      case Left(e) => sink.observer.onError(e)
      case Right(r) => sink.observer.onNext(r).asInstanceOf[Unit]
    })

    newState
  }

  def subscribe(f: State => IO[Future[Ack]]): IO[Cancelable] =
    IO(source.subscribe(f andThen(_.unsafeRunSync())))
}

object Store {
  implicit def toPipe[State, Action](store: Store[State, Action]): Pipe[Action, State] =
    Pipe(store.sink, store.source)

  private val storeRef = STRef.empty

  def renderWithStore[S, A](initialState: S, reducer: (S, A) => (S, Option[IO[A]]), selector: String, root: VNode)(implicit s: Scheduler): IO[Unit] = for {
    handler <- Handler.create[A]
    store <- IO(Store(initialState, reducer, handler))
    _ <- storeRef.asInstanceOf[STRef[Store[S, A]]].put(store)
    _ <- OutWatch.renderInto(selector, root)
  } yield ()

  def getStore[S, A]: IO[Store[S, A]] =
    storeRef.asInstanceOf[STRef[Store[S, A]]].getOrThrow(NoStoreException)

  private object NoStoreException extends
    Exception("Application was rendered without specifying a Store, please use Outwatch.renderWithStore instead")

}
