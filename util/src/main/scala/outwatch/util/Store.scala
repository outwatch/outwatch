package outwatch.util

import cats.effect.Sync

import colibri._

object Store {

  /* Partial application trick, "kinda-curried type parameters"
   * https://typelevel.org/cats/guidelines.html
   *
   * In scala-js, @inline assures that this trick has no overhead and generates the same code as calling create[F, T](seed)
   * AnyVal still generates code in this code for creating an CreatePartiallyApplied instance.
   */
  @inline final class CreatePartiallyApplied[F[_]] {
    @inline def apply[A, M](initialAction: A, initialState: M, reducer: Reducer[A, M])(implicit F: Sync[F]) = create[F, A, M](initialAction, initialState, reducer)
  }

  def create[F[_]] = new CreatePartiallyApplied[F]

  def create[F[_], A, M](
    initialAction: A,
    initialState: M,
    reducer: Reducer[A, M]
  )(implicit F: Sync[F]): F[ProSubject[A, (A, M)]] = F.delay {
    val subject = Subject.publish[A]()

    val fold: ((A, M), A) => (A, M) = {
      case ((_, state), action) => {
        val (newState, effects) = reducer(state, action)

        effects.unsafeSubscribe(Observer.create(subject.unsafeOnNext))

        action -> newState
      }
    }

    subject.transformSubjectSource(source =>
      source
        .scan[(A, M)](initialAction -> initialState)(fold)
        .behavior(initialAction -> initialState).refCount
        .withDefaultSubscription(Observer.empty)
    )
  }
}
