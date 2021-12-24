package outwatch

import cats.effect.Sync
import cats.implicits._
import org.scalajs.dom
import org.scalajs.dom._
import outwatch.interpreter.SnabbdomOps
import snabbdom.{VNodeProxy, patch}

case class RenderConfig(
  errorModifier: Throwable => VDomModifier
)
object RenderConfig {
  import dsl._

  private lazy val isLocalhost = dom.window.location.host.startsWith("localhost:") || dom.window.location.host == "localhost"

  def default: RenderConfig = if (isLocalhost) showError else ignoreError

  def showError: RenderConfig = RenderConfig(
    error => div(backgroundColor := "#FDF2F5", color := "#D70022", padding := "5px", display.inlineBlock, s"ERROR: $error")
  )

  def ignoreError: RenderConfig = RenderConfig(
    _ => VDomModifier.empty
  )
}

object OutWatch {

  def toSnabbdom[F[_]](vNode: VNode, config: RenderConfig = RenderConfig.default)(implicit F: Sync[F]): F[VNodeProxy] = F.delay {
    SnabbdomOps.toSnabbdom(vNode, config)
  }

  def renderInto[F[_]](element: dom.Element, vNode: VNode, config: RenderConfig = RenderConfig.default)(implicit F: Sync[F]): F[Unit] =
    toSnabbdom(vNode, config).map { node =>
      val elem = dom.document.createElement("div")
      element.appendChild(elem)
      patch(elem, node)
    }.void

  // workaround for https://github.com/lampepfl/dotty/issues/14096
  def renderInto[F[_]](element: dom.Element, vNode: VNode)(implicit F: Sync[F]): F[Unit] =
    toSnabbdom(vNode, RenderConfig.default).map { node =>
      val elem = dom.document.createElement("div")
      element.appendChild(elem)
      patch(elem, node)
    }.void

  def renderReplace[F[_]: Sync](element: dom.Element, vNode: VNode, config: RenderConfig = RenderConfig.default): F[Unit] =
    toSnabbdom(vNode, config).map { node =>
      val elementNode = snabbdom.tovnode(element)
      patch(elementNode, node)
    }.void

  // workaround for https://github.com/lampepfl/dotty/issues/14096
  def renderReplace[F[_]: Sync](element: dom.Element, vNode: VNode): F[Unit] =
    toSnabbdom(vNode, RenderConfig.default).map { node =>
      val elementNode = snabbdom.tovnode(element)
      patch(elementNode, node)
    }.void

  def renderInto[F[_]: Sync](querySelector: String, vNode: VNode): F[Unit] =
    renderInto(querySelector, vNode, RenderConfig.default)

  def renderReplace[F[_]: Sync](querySelector: String, vNode: VNode): F[Unit] =
    renderReplace(querySelector, vNode, RenderConfig.default)

  def renderInto[F[_]: Sync](querySelector: String, vNode: VNode, config: RenderConfig): F[Unit] =
    renderInto(document.querySelector(querySelector), vNode, config)

  def renderReplace[F[_]: Sync](querySelector: String, vNode: VNode, config: RenderConfig): F[Unit] =
    renderReplace(document.querySelector(querySelector), vNode, config)
}
