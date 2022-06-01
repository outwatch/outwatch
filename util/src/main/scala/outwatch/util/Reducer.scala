package outwatch.util

import cats.implicits._
import colibri._

@deprecated("Use colibri.Subject and pattern matching to build your own Store", "")
object Reducer {

  /** Creates a Reducer which yields a new State, as-well as an Observable of Effects Effects are Actions which will be
    * executed after the Action that caused them to occur. This is accomplished by subscribing to the Effects Observable
    * within the stores scan loop.
    *
    * CAUTION: There is currently a bug which causes the Effect-States to emit, before the State of the action that
    * caused the effects is emitted. However, this only effects immediate emissions of the Effects Observable, delayed
    * emissions should be fine.
    * @param f
    *   The Reducing Function returning the (Model, Effects) tuple.
    */
  def withEffects[F[_]: ObservableLike, A, M](f: (M, A) => (M, F[A])): Reducer[A, M] = (s: M, a: A) =>
    f(s, a).map(ObservableLike[F].toObservable)

  /** Creates a reducer which just transforms the state, without additional effects.
    */
  def apply[A, M](f: (M, A) => M): Reducer[A, M] = (s: M, a: A) => f(s, a) -> Observable.empty

  /** Creates a Reducer with an optional effect.
    */
  def withOptionalEffects[F[_]: ObservableLike, A, M](f: (M, A) => (M, Option[F[A]])): Reducer[A, M] = (s: M, a: A) =>
    f(s, a).map((x: Option[F[A]]) => x.fold[Observable[A]](Observable.empty)(ObservableLike[F].toObservable))
}
