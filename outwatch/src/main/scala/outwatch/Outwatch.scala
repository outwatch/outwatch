package outwatch

import cats.effect.Sync
import cats.implicits._
import org.scalajs.dom
import outwatch.interpreter.SnabbdomOps

case class RenderConfig(
  errorModifier: Throwable => VMod,
  accumAttrHook: (AccumAttr, Attr.Value) => Attr.Value = (aa, attr) => aa.accum(attr, aa.value),
)
object RenderConfig {
  import dsl._

  private lazy val isLocalhost =
    dom.window.location.host.startsWith("localhost:") || dom.window.location.host == "localhost"

  def default: RenderConfig = if (isLocalhost) showError else ignoreError

  def showError: RenderConfig = RenderConfig(error =>
    div(backgroundColor := "#FDF2F5", color := "#D70022", padding := "5px", display.inlineBlock, s"ERROR: $error"),
  )

  def ignoreError: RenderConfig = RenderConfig(_ => VMod.empty)
}

object Outwatch {

  def toSnabbdom[F[_]: Sync](vNode: VNode, config: RenderConfig = RenderConfig.default): F[snabbdom.VNodeProxy] =
    Sync[F].delay {
      SnabbdomOps.toSnabbdom(vNode, config)
    }

  // Scala 3 does not allow default arguments here, that's why we use overloading instead.
  // It's a workaround for https://github.com/lampepfl/dotty/issues/14096

  def renderInto[F[_]: Sync](element: dom.Element, vNode: VNode): F[Unit] =
    renderInto(element, vNode, RenderConfig.default)
  def renderInto[F[_]: Sync](element: dom.Element, vNode: VNode, config: RenderConfig): F[Unit] = for {
    node <- toSnabbdom(vNode, config)
    elem <- Sync[F].delay(dom.document.createElement("div"))
    _    <- Sync[F].delay(element.appendChild(elem))
    _    <- Sync[F].delay(snabbdom.patch(elem, node))
  } yield ()

  def renderReplace[F[_]: Sync](element: dom.Element, vNode: VNode): F[Unit] =
    renderReplace(element, vNode, RenderConfig.default)
  def renderReplace[F[_]: Sync](element: dom.Element, vNode: VNode, config: RenderConfig): F[Unit] = for {
    node        <- toSnabbdom(vNode, config)
    elementNode <- Sync[F].delay(snabbdom.tovnode(element))
    _           <- Sync[F].delay(snabbdom.patch(elementNode, node))
  } yield ()

  def renderInto[F[_]: Sync](querySelector: String, vNode: VNode): F[Unit] =
    renderInto(querySelector, vNode, RenderConfig.default)
  def renderInto[F[_]: Sync](querySelector: String, vNode: VNode, config: RenderConfig): F[Unit] = for {
    elem <- Sync[F].delay(dom.document.querySelector(querySelector))
    _    <- renderInto(elem, vNode, config)
  } yield ()

  def renderReplace[F[_]: Sync](querySelector: String, vNode: VNode): F[Unit] =
    renderReplace(querySelector, vNode, RenderConfig.default)
  def renderReplace[F[_]: Sync](querySelector: String, vNode: VNode, config: RenderConfig): F[Unit] = for {
    elem <- Sync[F].delay(dom.document.querySelector(querySelector))
    _    <- renderReplace(elem, vNode, config)
  } yield ()
}
