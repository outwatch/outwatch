package outwatch.util

import cats.effect.Effect
import cats.syntax.all._
import monix.execution.Scheduler
import org.scalajs.dom
import outwatch.OutwatchOps
import outwatch.dom.VDomModifierFactory
import outwatch.dom.helpers.STRef

import scala.util.Try
import scala.util.control.NonFatal

trait StoreFactory[F[+_]] extends VDomModifierFactory[F] with OutwatchOps[F] {
  implicit val effectF:Effect[F]

  object Store {

    case class Reducer[S, A](reducer: (S, A) => (S, Observable[A]))

    object Reducer {

      implicit def stateAndEffects[S, A](f: (S, A) => (S, Observable[A])): Reducer[S, A] = Reducer(f)

      implicit def justState[S, A](f: (S, A) => S): Reducer[S, A] = Reducer { (s: S, a: A) => (f(s, a), Observable.empty) }

      implicit def stateAndOptionEffect[S, A](f: (S, A) => (S, Option[F[A]])): Reducer[S, A] = Reducer { (s: S, a: A) =>
        val (mewState, effect) = f(s, a)
        (mewState, effect.fold[Observable[A]](Observable.empty)(Observable.fromEffect))
      }
    }

    private val storeRef = STRef.empty

    def create[State, Action](
                                                initialState: State,
                                                reducer: Reducer[State, Action]
                                              )(implicit s: Scheduler): F[Pipe[Action, State]] = {

      Handler.create[Action].map { handler =>

        val fold: (State, Action) => State = (state, action) => Try { // guard against reducer throwing an exception
          val (newState, effects) = reducer.reducer(state, action)

          effects.subscribe(
            e => handler.observer.feed(e :: Nil),
            e => dom.console.error(e.getMessage) // just log the error, don't push it into the handler's observable, because it would stop the scan "loop"
          )
          newState
        }.recover { case NonFatal(e) =>
          dom.console.error(e.getMessage)
          state
        }.get

        handler.transformSource(source =>
          source
            .scan(initialState)(fold)
            .share
            .startWith(Seq(initialState))
        )
      }
    }


    def get[S, A]: F[Pipe[A, S]] = storeRef.asInstanceOf[STRef[F, Pipe[A, S]]].getOrThrow(NoStoreException)

    def renderWithStore[S, A]( initialState: S, reducer: Reducer[S, A], selector: String, root: VNodeF, renderInto:(String, VNodeF) => F[Unit] )
                             (implicit s: Scheduler): F[Unit] = for {
      store <- Store.create(initialState, reducer)
      _ <- storeRef.asInstanceOf[STRef[F, Pipe[A, S]]].put(store)
      _ <- renderInto(selector, root)
    } yield ()

    private object NoStoreException
      extends Exception("Application was rendered without specifying a Store, please use Outwatch.renderWithStore instead")

  }

}
