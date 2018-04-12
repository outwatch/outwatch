package outwatch.dom

import cats.effect.{Effect, IO, Sync}
import cats.syntax.all._
import monix.execution.Scheduler
import org.scalajs.dom
import org.scalajs.dom._
import outwatch.util.StoreFactory
import snabbdom.patch

trait RenderFactory[F[+_]] extends VDomModifierFactory[F] with StoreFactory[F] {
  implicit val effectF: Effect[F]


  def renderInto(element: Element, vNode: VNodeF)(implicit s: Scheduler): F[Unit] = for {
    node <- vNode
    _ <- effectF.delay {
      val elem = dom.document.createElement("app")
      element.appendChild(elem)
      patch(elem, node.toSnabbdom)
    }
  } yield ()

  def renderReplace(element: Element, vNode: VNodeF)(implicit s: Scheduler): F[Unit] = for {
    node <- vNode
    _ <- effectF.delay(patch(element, node.toSnabbdom))
  } yield ()

  def renderInto(querySelector: String, vNode: VNodeF)(implicit s: Scheduler): F[Unit] =
    renderInto(document.querySelector(querySelector), vNode)

  def renderReplace(querySelector: String, vNode: VNodeF)(implicit s: Scheduler): F[Unit] =
    renderReplace(document.querySelector(querySelector), vNode)

  @deprecated("Use renderInto instead (or renderReplace)", "0.11.0")
  def render(querySelector: String, vNode: VNodeF)(implicit s: Scheduler): F[Unit] =
    renderInto(querySelector, vNode)

  def renderWithStore[S, A](
                             initialState: S, reducer: Store.Reducer[S, A], querySelector: String, root: VNodeF
                           )(implicit s: Scheduler): F[Unit] =
    Store.renderWithStore(initialState, reducer, querySelector, root, renderInto)
}

object OutWatch extends StoreFactory[IO] {
  override val effectF: Effect[IO] = IO.ioConcurrentEffect
}
