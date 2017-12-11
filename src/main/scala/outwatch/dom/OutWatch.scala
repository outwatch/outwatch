package outwatch.dom

import cats.effect.IO
import org.scalajs.dom
import org.scalajs.dom._
import outwatch.util.Store
import snabbdom.patch

object OutWatch {

  def renderInto(element: dom.Element, vNode: VNode): IO[Unit] = for {
    node <- vNode
    _ <- IO {
      val elem = dom.document.createElement("app")
      element.appendChild(elem)
      patch(elem, node.asProxy)
    }
  } yield ()

  def render(element: dom.Element, vNode: VNode): IO[Unit] = for {
    node <- vNode
    _ <- IO(patch(element, node.asProxy))
  } yield ()

  def render(querySelector: String, vNode: VNode): IO[Unit] =
    renderInto(document.querySelector(querySelector), vNode)

  def renderWithStore[S, A](initialState: S, reducer: (S, A) => (S, Option[IO[A]]), querySelector: String, root: VNode): IO[Unit] =
    Store.renderWithStore(initialState, reducer, querySelector, root)
}
