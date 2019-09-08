package outwatch.dom

import cats.effect.Sync
import cats.implicits._
import monix.execution.Scheduler
import org.scalajs.dom
import org.scalajs.dom._
import outwatch.dom.helpers.SnabbdomOps
import snabbdom.{VNodeProxy, patch}

object OutWatch {
  def toSnabbdom[F[_]](vNode: VNode)(implicit s: Scheduler, F: Sync[F]): F[VNodeProxy] = F.delay {
    SnabbdomOps.toSnabbdom(vNode)
  }

  def renderInto[F[_]](element: dom.Element, vNode: VNode)(implicit s: Scheduler, F: Sync[F]): F[Unit] =
    toSnabbdom(vNode).map { node =>
      val elem = dom.document.createElement("div")
      element.appendChild(elem)
      patch(elem, node)
    }.void

  def renderReplace[F[_]: Sync](element: dom.Element, vNode: VNode)(implicit s: Scheduler): F[Unit] =
    toSnabbdom(vNode).map { node =>
      val elementNode = snabbdom.tovnode(element)
      patch(elementNode, node)
    }.void

  def renderInto[F[_]: Sync](querySelector: String, vNode: VNode)(implicit s: Scheduler): F[Unit] =
    renderInto(document.querySelector(querySelector), vNode)

  def renderReplace[F[_]: Sync](querySelector: String, vNode: VNode)(implicit s: Scheduler): F[Unit] =
    renderReplace(document.querySelector(querySelector), vNode)

  @deprecated("Use renderInto instead (or renderReplace)", "0.11.0")
  def render[F[_]: Sync](querySelector: String, vNode: VNode)(implicit s: Scheduler): F[Unit] = renderInto(querySelector, vNode)
}
