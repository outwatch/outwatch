package outwatch.util

import monix.reactive.{Observable, ObservableLike}

/**
 * A Function that applies an Action onto the Stores current state.
 * @param reducer The reducing function
 * @tparam A The Action Type
 * @tparam M The Model Type
 */
case class Reducer[A, M](reducer: (M, A) => (M, Observable[A]))

object Reducer {
  /**
   * Creates a Reducer which yields a new State, as-well as an Observable of Effects
   * Effects are Actions which will be executed after the Action that caused them to occur.
   * This is accomplished by subscribing to the Effects Observable within the stores scan loop.
   *
   * CAUTION: There is currently a bug which causes the Effect-States to emit,
   * before the State of the action that caused the effects is emitted.
   * However, this only effects immediate emissions of the Effects Observable, delayed emissions should be fine.
   * @param f The Reducing Function returning the (Model, Effects) tuple.
   */
  implicit def stateAndEffects[A, M](f: (M, A) => (M, Observable[A])): Reducer[A, M] = Reducer(f)

  /**
   * Creates a reducer which just transforms the state, without additional effects.
   */
  implicit def justState[A, M](f: (M, A) => M): Reducer[A, M] = Reducer { (s: M, a: A) => (f(s, a), Observable.empty) }

  /**
   * Creates a Reducer with an optional IO effect.
   */
  implicit def stateAndOptionF[F[_]: ObservableLike, A, M](f: (M, A) => (M, Option[F[A]])): Reducer[A, M] = Reducer { (s: M, a: A) =>
    val (newState, effect) = f(s, a)
    (newState, effect.fold[Observable[A]](Observable.empty)(Observable.from))
  }
}
