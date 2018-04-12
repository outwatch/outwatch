package outwatch.dom

import cats.effect.{Effect, IO, Sync}
import cats.syntax.all._
import monix.execution.Scheduler
import org.scalajs.dom
import org.scalajs.dom._
import outwatch.util.StoreFactory
import snabbdom.patch

override object OutWatch extends StoreFactory[IO] {
  override val effectF: Effect[IO] = IO.ioConcurrentEffect

  def renderInto[F[+ _] : Effect](element: Element, vNode: VNodeF[F])(implicit s: Scheduler): F[Unit] = for {
    node <- vNode
    _ <- Sync[F].delay {
      val elem = dom.document.createElement("app")
      element.appendChild(elem)
      patch(elem, node.toSnabbdom)
    }
  } yield ()

  def renderReplace[F[+ _] : Effect](element: Element, vNode: VNodeF[F])(implicit s: Scheduler): F[Unit] = for {
    node <- vNode
    _ <- Sync[F].delay(patch(element, node.toSnabbdom))
  } yield ()

  def renderInto[F[+ _] : Effect](querySelector: String, vNode: VNodeF[F])(implicit s: Scheduler): F[Unit] =
    renderInto[F](document.querySelector(querySelector), vNode)

  def renderReplace[F[+ _] : Effect](querySelector: String, vNode: VNodeF[F])(implicit s: Scheduler): F[Unit] =
    renderReplace[F](document.querySelector(querySelector), vNode)

  @deprecated("Use renderInto instead (or renderReplace)", "0.11.0")
  def render(querySelector: String, vNode: VNodeF[IO])(implicit s: Scheduler): IO[Unit] =
    renderInto(querySelector, vNode)

  def renderWithStore[S, A](
                             initialState: S, reducer: Store.Reducer[S, A], querySelector: String, root: VNodeF[IO]
                           )(implicit s: Scheduler): IO[Unit] =
    Store.renderWithStore(initialState, reducer, querySelector, root)
}
