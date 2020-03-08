package outwatch

import cats.effect.Sync
import cats.implicits._
import org.scalajs.dom
import org.scalajs.dom._
import outwatch.interpreter.SnabbdomOps
import snabbdom.{VNodeProxy, patch}

object OutWatch {
  def toSnabbdom[F[_]](vNode: VNode)(implicit F: Sync[F]): F[VNodeProxy] = F.delay {
    SnabbdomOps.toSnabbdom(vNode)
  }

  def renderInto[F[_]](element: dom.Element, vNode: VNode)(implicit F: Sync[F]): F[Unit] =
    toSnabbdom(vNode).map { node =>
      val elem = dom.document.createElement("div")
      element.appendChild(elem)
      patch(elem, node)
    }.void

  def renderReplace[F[_]: Sync](element: dom.Element, vNode: VNode): F[Unit] =
    toSnabbdom(vNode).map { node =>
      val elementNode = snabbdom.tovnode(element)
      patch(elementNode, node)
    }.void

  def renderInto[F[_]: Sync](querySelector: String, vNode: VNode): F[Unit] =
    renderInto(document.querySelector(querySelector), vNode)

  def renderReplace[F[_]: Sync](querySelector: String, vNode: VNode): F[Unit] =
    renderReplace(document.querySelector(querySelector), vNode)
}
