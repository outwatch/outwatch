package outwatch.ext.monix.util

import cats.effect.Sync
import monix.execution.Scheduler
import monix.eval.Task
import monix.reactive.subjects.PublishSubject
import org.scalajs.dom
import outwatch.ext.monix._

object Store {

  /* Partial application trick, "kinda-curried type parameters"
   * https://typelevel.org/cats/guidelines.html
   *
   * In scala-js, @inline assures that this trick has no overhead and generates the same code as calling create[F, T](seed)
   * AnyVal still generates code in this code for creating an CreatePartiallyApplied instance.
   */
  @inline final class CreatePartiallyApplied[F[_]] {
    @inline def apply[A, M](initialAction: A, initialState: M, reducer: Reducer[A, M])(implicit s: Scheduler, F: Sync[F]) = create[F, A, M](initialAction, initialState, reducer)
  }

  def create[F[_]] = new CreatePartiallyApplied[F]

  def create[F[_], A, M](
    initialAction: A,
    initialState: M,
    reducer: Reducer[A, M]
  )(implicit s: Scheduler, F: Sync[F]): F[MonixProHandler[A, (A, M)]] = F.delay {
    val subject = PublishSubject[A]

    val fold: ((A, M), A) => (A, M) = {
      case ((_, state), action) => {
        val (newState, effects) = reducer(state, action)

        effects.subscribe(
          next => subject.feed(next :: Nil),
          // just log the error, don't push it into the subject's observable, because it would stop the scan "loop"
          error => dom.console.error(error.getMessage)
        )

        action -> newState
      }
    }

    val out = subject.transformObservable(source =>
      source
        .scan0[(A, M)](initialAction -> initialState)(fold)
        .replay(1).refCount
    )

    val sub = out.subscribe()
    out.doOnSubscribeF(Task(sub.cancel))

    out
  }
}
