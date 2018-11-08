package outwatch.util

import cats.effect.IO
import monix.execution.Scheduler
import monix.reactive.Observable
import org.scalajs.dom
import outwatch._
import outwatch.dom.{OutWatch, VNode}
import outwatch.dom.helpers.STRef

import scala.util.Try
import scala.util.control.NonFatal


object Store {

  case class Reducer[S, A](reducer: (S, A) => (S, Observable[A]))

  object Reducer {

    implicit def stateAndEffects[S, A](f: (S, A) => (S, Observable[A])): Reducer[S, A] = Reducer(f)

    implicit def justState[S, A](f: (S, A) => S): Reducer[S, A] = Reducer { (s: S, a: A) => (f(s, a), Observable.empty) }

    implicit def stateAndOptionIO[S, A](f: (S, A) => (S, Option[IO[A]])): Reducer[S, A] =  Reducer { (s: S, a: A) =>
      val (mewState, effect) = f(s, a)
      (mewState, effect.fold[Observable[A]](Observable.empty)(Observable.fromIO))
    }
  }

  private val storeRef = STRef.empty

  def create[State, Action](
    initialState: State,
    reducer: Reducer[State, Action]
  )(implicit s: Scheduler): IO[ProHandler[Action, State]] = Handler.create[Action].map { subject =>

      val fold: (State, Action) => State = (state, action) => Try { // guard against reducer throwing an exception
        val (newState, effects) = reducer.reducer(state, action)

        effects.subscribe(
          e => subject.feed(e :: Nil),
          e => dom.console.error(e.getMessage) // just log the error, don't push it into the subject's observable, because it would stop the scan "loop"
        )
        newState
      }.recover { case NonFatal(e) =>
        dom.console.error(e.getMessage)
        state
      }.get

      subject.transformObservable(source =>
        source
          .scan(initialState)(fold)
          .share
          .startWith(Seq(initialState))
      )
  }

  def get[S, A]: IO[ProHandler[A, S]] = storeRef.asInstanceOf[STRef[ProHandler[A, S]]].getOrThrow(NoStoreException)

  def renderWithStore[S, A](
    initialState: S, reducer: Reducer[S, A], selector: String, root: VNode
  )(implicit s: Scheduler): IO[Unit] = for {
    store <- Store.create(initialState, reducer)
    _ <- storeRef.asInstanceOf[STRef[ProHandler[A, S]]].put(store)
    _ <- OutWatch.renderInto(selector, root)
  } yield ()

  private object NoStoreException
    extends Exception("Application was rendered without specifying a Store, please use Outwatch.renderWithStore instead")
}
