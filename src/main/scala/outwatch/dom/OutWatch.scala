package outwatch.dom

import cats.effect.IO
import org.scalajs.dom._
import outwatch.dom.helpers.DomUtils
import outwatch.util.Store

object OutWatch {
  def render(querySelector: String, vNode: VNode): IO[Unit] =
    DomUtils.render(document.querySelector(querySelector), vNode)

  def renderWithStore[S, A](initialState: S, reducer: (S, A) => (S, Option[IO[A]]), querySelector: String, root: VNode): IO[Unit] =
    Store.renderWithStore(initialState, reducer, querySelector, root)
}
