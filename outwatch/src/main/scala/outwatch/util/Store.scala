package outwatch.util

import cats.effect.IO
import monix.execution.Scheduler
import monix.reactive.Observable
import monix.reactive.subjects.PublishSubject
import org.scalajs.dom
import outwatch._
import outwatch.dom.helpers.STRef
import outwatch.dom.{OutWatch, VNode}

import scala.util.Try
import scala.util.control.NonFatal

object Store {

  case class Reducer[A, M](reducer: (M, A) => (M, Observable[A]))

  object Reducer {
    implicit def stateAndEffects[A, M](f: (M, A) => (M, Observable[A])): Reducer[A, M] = Reducer(f)

    implicit def justState[A, M](f: (M, A) => M): Reducer[A, M] = Reducer { (s: M, a: A) => (f(s, a), Observable.empty) }

    implicit def stateAndOptionIO[A, M](f: (M, A) => (M, Option[IO[A]])): Reducer[A, M] = Reducer { (s: M, a: A) =>
      val (newState, effect) = f(s, a)
      (newState, effect.fold[Observable[A]](Observable.empty)(Observable.fromIO))
    }
  }

  def create[A, M](
    initialAction: A,
    initialState: M,
    reducer: Reducer[A, M],
    recoverError: PartialFunction[Throwable, M => M] = PartialFunction.empty
  )(implicit s: Scheduler): IO[ProHandler[A, (A, M)]] = IO {
    val subject = PublishSubject[A]

    val fold: ((A, M), A) => (A, M) = {
      case ((_, state), action) => Try { // guard against reducer throwing an exception
        val (newState, effects) = reducer.reducer(state, action)

        effects.subscribe(
          next => subject.feed(next :: Nil),
          // just log the error, don't push it into the subject's observable, because it would stop the scan "loop"
          error => dom.console.error(error.getMessage)
        )

        action -> newState
      }.recover(recoverError.andThen(f => action -> f(state)) orElse { case NonFatal(e) =>
        dom.console.error(e.getMessage)
        action -> state
      }).get
    }

    subject.transformObservable(source =>
      source
        .scan0[(A, M)](initialAction -> initialState)(fold)
        .replay(1).refCount
    )
  }

  private val storeRef = STRef.empty

  def get[A, M]: IO[ProHandler[A, M]] = storeRef.asInstanceOf[STRef[ProHandler[A, M]]].getOrThrow(NoStoreException)

  def renderWithStore[A, M](
    initialAction: A,
    initialState: M,
    reducer: Reducer[A, M],
    selector: String,
    root: VNode
  )(implicit s: Scheduler): IO[Unit] = for {
    store <- Store.create[A, M](initialAction, initialState, reducer)
    _ <- storeRef.asInstanceOf[STRef[ProHandler[A, M]]].put(store.mapProHandler[A, M](in => in)(out => out._2))
    _ <- OutWatch.renderInto(selector, root)
  } yield ()

  private object NoStoreException
    extends Exception("Application was rendered without specifying a Store, please use Outwatch.renderWithStore instead")

}
