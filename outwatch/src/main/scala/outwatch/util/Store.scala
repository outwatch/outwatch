package outwatch.util

import cats.effect.Sync
import cats.implicits._
import monix.execution.Scheduler
import monix.eval.Task
import monix.reactive.subjects.PublishSubject
import org.scalajs.dom
import outwatch._
import outwatch.dom.helpers.STRef
import outwatch.dom.{OutWatchOps, VNode, ProHandlerOps, ProHandler}

trait StoreOps[F[_]] {

  object Store extends ProHandlerOps[F] {
    def create[A, M](
      initialAction: A,
      initialState: M,
      reducer: Reducer[A, M]
    )(implicit s: Scheduler, F: Sync[F]): F[ProHandler[A, (A, M)]] = F.delay {
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
}

class GlobalStore[F[_]: Sync, A, M] extends ProHandlerOps[F] with StoreOps[F] with OutWatchOps[F] {

  /**
   * A global reference to a Store.
   * Commonly used to implement the Redux Pattern.
   */
  private val storeRef = STRef.empty[F, ProHandler[A, M]]

  /**
   * Get's a globally unique store.
   * Commonly used to implement the Redux Pattern.
   */
  def get: F[ProHandler[A, M]] = storeRef.getOrThrow(NoStoreException)

  /**
   * Renders an Application with a globally unique Store.
   * Commonly used to implement the Redux Pattern.
   */
  def renderWithStore(
    initialAction: A,
    initialState: M,
    reducer: Reducer[A, M],
    selector: String,
    root: F[VNode]
  )(implicit s: Scheduler): F[Unit] = for {
    store <- Store.create[A, M](initialAction, initialState, reducer)
    _ <- storeRef.asInstanceOf[STRef[F, ProHandler[A, M]]].put(store.mapProHandler[A, M](in => in)(out => out._2))
    vnode <- root
    _ <- OutWatch.renderInto(selector, vnode)
  } yield ()

  private object NoStoreException
    extends Exception("Application was rendered without specifying a Store, please use Outwatch.renderWithStore instead")
}
