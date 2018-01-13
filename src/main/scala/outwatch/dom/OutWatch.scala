package outwatch.dom

import cats.effect.Effect
import monix.execution.Scheduler
import org.scalajs.dom
import org.scalajs.dom._
import outwatch.util.Store
import snabbdom.patch

object OutWatch {

  def renderInto(element: dom.Element, vNode: VNode)(implicit s: Scheduler): F[Unit] = for {
    node <- vNode
    _ <- Effect[F].delay {
      val elem = dom.document.createElement("app")
      element.appendChild(elem)
      patch(elem, node.asProxy)
    }
  } yield ()

  def renderReplace(element: dom.Element, vNode: VNode)(implicit s: Scheduler): F[Unit] = for {
    node <- vNode
    _ <- IO(patch(element, node.asProxy))
  } yield ()

  def renderInto(querySelector: String, vNode: VNode)(implicit s: Scheduler): F[Unit] =
    renderInto(document.querySelector(querySelector), vNode)

  def renderReplace(querySelector: String, vNode: VNode)(implicit s: Scheduler): F[Unit] =
    renderReplace(document.querySelector(querySelector), vNode)

  @deprecated("Use renderInto instead (or renderReplace)", "0.11.0")
  def render(querySelector: String, vNode: VNode)(implicit s: Scheduler): F[Unit] = renderInto(querySelector, vNode)

  def renderWithStore[S, A](initialState: S, reducer: (S, A) => (S, Option[F[A]]), querySelector: String, root: VNode
  )(implicit s: Scheduler): F[Unit] =
    Store.renderWithStore(initialState, reducer, querySelector, root)
}
