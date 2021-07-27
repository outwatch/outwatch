package outwatch

import cats.effect.Sync
import cats.implicits._
import org.scalajs.dom
import org.scalajs.dom._
import outwatch.interpreter.SnabbdomOps

object Outwatch {
  def toSnabbdom[F[_] : Sync](vNode: VNode): F[snabbdom.VNodeProxy] = Sync[F].delay {
    SnabbdomOps.toSnabbdom(vNode)
  }

  def renderInto[F[_]](element: dom.Element, vNode: VNode)(implicit F: Sync[F]): F[Unit] = for {
    node <- toSnabbdom(vNode)
    elem <- Sync[F].delay(dom.document.createElement("div"))
    _ <- Sync[F].delay(element.appendChild(elem))
    _ <- Sync[F].delay(snabbdom.patch(elem, node))
  } yield ()

  def renderReplace[F[_]: Sync](element: dom.Element, vNode: VNode): F[Unit] = for {
    node <- toSnabbdom(vNode)
    elementNode <- Sync[F].delay(snabbdom.tovnode(element))
    _ <- Sync[F].delay(snabbdom.patch(elementNode, node))
  } yield ()

  def renderInto[F[_]: Sync](querySelector: String, vNode: VNode): F[Unit] = for {
    elem <- Sync[F].delay(document.querySelector(querySelector))
    _ <- renderInto(elem, vNode)
  } yield ()

  def renderReplace[F[_]: Sync](querySelector: String, vNode: VNode): F[Unit] = for {
    elem <- Sync[F].delay(document.querySelector(querySelector))
    _ <- renderReplace(elem, vNode)
  } yield ()

  def renderIntoBody[F[_]](vNode: VNode)(implicit F: Sync[F]): F[Unit] =
    renderInto(dom.document.body, vNode)
}
