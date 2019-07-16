package outwatch.dom

import cats.effect.IO
import monix.execution.Scheduler
import org.scalajs.dom
import org.scalajs.dom._
import outwatch.dom.helpers.SnabbdomOps
import outwatch.util.Store
import snabbdom.{VNodeProxy, patch}

object OutWatch {

  def toSnabbdom(vNode: VNode)(implicit s: Scheduler): IO[VNodeProxy] = IO {
    SnabbdomOps.toSnabbdom(vNode)
  }

  def renderInto(element: dom.Element, vNode: VNode)(implicit s: Scheduler): IO[Unit] = for {
    node <- toSnabbdom(vNode)
    _ <- IO {
      val elem = dom.document.createElement("div")
      element.appendChild(elem)
      patch(elem, node)
    }
  } yield ()

  def renderReplace(element: dom.Element, vNode: VNode)(implicit s: Scheduler): IO[Unit] = for {
    node <- toSnabbdom(vNode)
    _ <- IO {
      val elementNode = snabbdom.tovnode(element)
      patch(elementNode, node)
    }
  } yield ()

  def renderInto(querySelector: String, vNode: VNode)(implicit s: Scheduler): IO[Unit] =
    renderInto(document.querySelector(querySelector), vNode)

  def renderReplace(querySelector: String, vNode: VNode)(implicit s: Scheduler): IO[Unit] =
    renderReplace(document.querySelector(querySelector), vNode)

  @deprecated("Use renderInto instead (or renderReplace)", "0.11.0")
  def render(querySelector: String, vNode: VNode)(implicit s: Scheduler): IO[Unit] = renderInto(querySelector, vNode)

  def renderWithStore[A, M](
    initialAction: A,
    initialState: M,
    reducer: Store.Reducer[A, M],
    querySelector: String,
    root: IO[VNode]
  )(implicit s: Scheduler): IO[Unit] =
    Store.renderWithStore(initialAction, initialState, reducer, querySelector, root)

  def renderWithStore[A, M](
    initialAction: A,
    initialState: M,
    reducer: Store.Reducer[A, M],
    querySelector: String,
    root: VNode
  )(implicit s: Scheduler): IO[Unit] =
    Store.renderWithStore(initialAction, initialState, reducer, querySelector, IO.pure(root))
}
