package outwatch.util

import cats.effect.{IO, Sync}
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.{ObservableLike, Observable}
import monix.reactive.subjects.PublishSubject
import org.scalajs.dom
import outwatch._
import outwatch.dom.helpers.STRef
import outwatch.dom.{OutWatch, VNode}

object Store {

  /**
   * A Function that applies an Action onto the Stores current state.
   * @tparam A The Action Type
   * @tparam M The Model Type
   */
  type Reducer[A, M] = (M, A) => (M, Observable[A])

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
    def withEffects[A, M](f: (M, A) => (M, Observable[A])): Reducer[A, M] = f

    /**
     * Creates a reducer which just transforms the state, without additional effects.
     */
    def apply[A, M](f: (M, A) => M): Reducer[A, M] = (s: M, a: A) => f(s, a) -> Observable.empty

    /**
     * Creates a Reducer with an optional IO effect.
     */
    def withOptionalEffects[F[_]: ObservableLike, A, M](f: (M, A) => (M, Option[F[A]])): Reducer[A, M] = { (s: M, a: A) =>
      val (newState, effect) = f(s, a)
      (newState, effect.fold[Observable[A]](Observable.empty)(Observable.from))
    }
  }

  /**
   * Creates an IO[Store]
   * A Store capules a scan operation on an Observable in an opinionated manner.
   * An internal state is tansformed by a series of actions.
   * The state will be the same for every subscriber.
   * @param initialAction The Stores initial action. Useful for re-creating a store from memory.
   * @param initialState The stores initial state. Similar to the initial accumulator on a fold / scan.
   * @param reducer The Reducing funcion. Creates a new State from the previous state and an Action.
   * @return An Observable emitting a tuple of the current state and the action that caused that state.
   */
  def create[F[_]: Sync, A, M](
    initialAction: A,
    initialState: M,
    reducer: Reducer[A, M]
  )(implicit s: Scheduler): F[ProHandler[A, (A, M)]] = Sync[F].delay {
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

  /**
   * A global reference to a Store.
   * Commonly used to implement the Redux Pattern.
   */
  private val storeRef = STRef.empty

  /**
   * Get's a globally unique store.
   * Commonly used to implement the Redux Pattern.
   */
  def get[A, M]: IO[ProHandler[A, M]] = storeRef.asInstanceOf[STRef[ProHandler[A, M]]].getOrThrow(NoStoreException)

  /**
   * Renders an Application with a globally unique Store.
   * Commonly used to implement the Redux Pattern.
   */
  def renderWithStore[A, M](
    initialAction: A,
    initialState: M,
    reducer: Reducer[A, M],
    selector: String,
    root: IO[VNode]
  )(implicit s: Scheduler): IO[Unit] = for {
    store <- Store.create[IO, A, M](initialAction, initialState, reducer)
    _ <- storeRef.asInstanceOf[STRef[ProHandler[A, M]]].put(store.mapProHandler[A, M](in => in)(out => out._2))
    vnode <- root
    _ <- OutWatch.renderInto(selector, vnode)
  } yield ()

  private object NoStoreException
    extends Exception("Application was rendered without specifying a Store, please use Outwatch.renderWithStore instead")

}
