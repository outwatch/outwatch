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
    initialState: M,
    reducer: Reducer[A, M]
  )(implicit s: Scheduler): IO[ProHandler[A, M]] = IO {

      val subject = PublishSubject[A]

      val fold: (M, A) => M = (state, action) => Try { // guard against reducer throwing an exception
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

  private val storeRef = STRef.empty

  def get[A, M]: IO[ProHandler[A, M]] = storeRef.asInstanceOf[STRef[ProHandler[A, M]]].getOrThrow(NoStoreException)

  def renderWithStore[A, M](
    initialState: M, reducer: Reducer[A, M], selector: String, root: VNode
  )(implicit s: Scheduler): IO[Unit] = for {
    store <- Store.create(initialState, reducer)
    _ <- storeRef.asInstanceOf[STRef[ProHandler[A, M]]].put(store)
    _ <- OutWatch.renderInto(selector, root)
  } yield ()

  private object NoStoreException
    extends Exception("Application was rendered without specifying a Store, please use Outwatch.renderWithStore instead")
}
