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

  case class Reducer[M, A](reducer: (M, A) => (M, Observable[A]))

  object Reducer {
    implicit def stateAndEffects[M, A](f: (M, A) => (M, Observable[A])): Reducer[M, A] = Reducer(f)

    implicit def justState[M, A](f: (M, A) => M): Reducer[M, A] = Reducer { (s: M, a: A) => (f(s, a), Observable.empty) }

    implicit def stateAndOptionIO[M, A](f: (M, A) => (M, Option[IO[A]])): Reducer[M, A] =  Reducer { (s: M, a: A) =>
      val (mewState, effect) = f(s, a)
      (mewState, effect.fold[Observable[A]](Observable.empty)(Observable.fromIO))
    }
  }

  private val storeRef = STRef.empty

  def create[M, A](
    initialState: M,
    reducer: Reducer[M, A]
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

  def get[M, A]: IO[ProHandler[A, M]] = storeRef.asInstanceOf[STRef[ProHandler[A, M]]].getOrThrow(NoStoreException)

  def renderWithStore[M, A](
    initialState: M, reducer: Reducer[M, A], selector: String, root: VNode
  )(implicit s: Scheduler): IO[Unit] = for {
    store <- Store.create(initialState, reducer)
    _ <- storeRef.asInstanceOf[STRef[ProHandler[A, M]]].put(store)
    _ <- OutWatch.renderInto(selector, root)
  } yield ()

  private object NoStoreException
    extends Exception("Application was rendered without specifying a Store, please use Outwatch.renderWithStore instead")
}
