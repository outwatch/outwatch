package outwatch

import cats.effect.Sync
import cats.implicits._
import org.scalajs.dom
import org.scalajs.dom._
import outwatch.interpreter.SnabbdomOps

case class RenderConfig(
  errorModifier: Throwable => VMod,
)
object RenderConfig {
  import dsl._

  private lazy val isLocalhost =
    dom.window.location.host.startsWith("localhost:") || dom.window.location.host == "localhost"

  def default = if (isLocalhost) showError else ignoreError

  def showError = RenderConfig(error =>
    div(backgroundColor := "#FDF2F5", color := "#D70022", padding := "5px", display.inlineBlock, s"ERROR: $error"),
  )

  def ignoreError = RenderConfig(_ => VMod.empty)
}

object Outwatch {

  def toSnabbdom[F[_]: Sync](vNode: VNode, config: RenderConfig = RenderConfig.default): F[snabbdom.VNodeProxy] =
    Sync[F].delay {
      SnabbdomOps.toSnabbdom(vNode, config)
    }

  def renderInto[F[_]: Sync](element: dom.Element, vNode: VNode, config: RenderConfig = RenderConfig.default): F[Unit] =
    for {
      node <- toSnabbdom(vNode, config)
      elem <- Sync[F].delay(dom.document.createElement("div"))
      _    <- Sync[F].delay(element.appendChild(elem))
      _    <- Sync[F].delay(snabbdom.patch(elem, node))
    } yield ()

  def renderReplace[F[_]: Sync](
    element: dom.Element,
    vNode: VNode,
    config: RenderConfig = RenderConfig.default,
  ): F[Unit] = for {
    node        <- toSnabbdom(vNode, config)
    elementNode <- Sync[F].delay(snabbdom.tovnode(element))
    _           <- Sync[F].delay(snabbdom.patch(elementNode, node))
  } yield ()

  def renderInto[F[_]: Sync](querySelector: String, vNode: VNode, config: RenderConfig): F[Unit] = for {
    elem <- Sync[F].delay(document.querySelector(querySelector))
    _    <- renderInto(elem, vNode, config)
  } yield ()

  def renderReplace[F[_]: Sync](querySelector: String, vNode: VNode, config: RenderConfig): F[Unit] = for {
    elem <- Sync[F].delay(document.querySelector(querySelector))
    _    <- renderReplace(elem, vNode, config)
  } yield ()

  def renderInto[F[_]: Sync](querySelector: String, vNode: VNode): F[Unit] =
    renderInto(querySelector, vNode, RenderConfig.default)

  def renderReplace[F[_]: Sync](querySelector: String, vNode: VNode): F[Unit] =
    renderReplace(querySelector, vNode, RenderConfig.default)
}
