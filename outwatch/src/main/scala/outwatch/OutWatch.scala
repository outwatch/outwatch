package outwatch

import cats.effect.Sync
import cats.implicits._
import org.scalajs.dom
import org.scalajs.dom._
import outwatch.interpreter.SnabbdomOps
import snabbdom.{VNodeProxy, patch}

object OutWatch {
  @inline def toSnabbdom[F[_] : Sync](vNode: VNode): F[VNodeProxy] = toSnabbdom[F, Any](vNode, ())
  def toSnabbdom[F[_] : Sync, Env](vNode: RVNode[Env], env: Env): F[VNodeProxy] = Sync[F].delay {
    SnabbdomOps.toSnabbdom(vNode, env)
  }

  @inline def renderInto[F[_] : Sync](element: dom.Element, vNode: VNode): F[Unit] = renderInto[F, Any](element, vNode, ())
  def renderInto[F[_], Env](element: dom.Element, vNode: RVNode[Env], env: Env)(implicit F: Sync[F]): F[Unit] =
    toSnabbdom(vNode, env).map { node =>
      val elem = dom.document.createElement("div")
      element.appendChild(elem)
      patch(elem, node)
    }.void

  @inline def renderReplace[F[_]: Sync](element: dom.Element, vNode: VNode): F[Unit] = renderReplace[F, Any](element, vNode, ())
  def renderReplace[F[_]: Sync, Env](element: dom.Element, vNode: RVNode[Env], env: Env): F[Unit] =
    toSnabbdom(vNode, env).map { node =>
      val elementNode = snabbdom.tovnode(element)
      patch(elementNode, node)
    }.void

  @inline def renderInto[F[_]: Sync](querySelector: String, vNode: VNode): F[Unit] = renderInto[F, Any](querySelector, vNode, ())
  def renderInto[F[_]: Sync, Env](querySelector: String, vNode: RVNode[Env], env: Env): F[Unit] =
    renderInto(document.querySelector(querySelector), vNode, env)

  @inline def renderReplace[F[_]: Sync](querySelector: String, vNode: VNode): F[Unit] = renderReplace[F, Any](querySelector, vNode, ())
  def renderReplace[F[_]: Sync, Env](querySelector: String, vNode: RVNode[Env], env: Env): F[Unit] =
    renderReplace(document.querySelector(querySelector), vNode, env)
}
