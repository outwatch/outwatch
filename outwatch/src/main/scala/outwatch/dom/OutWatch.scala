package outwatch.dom

import cats.effect.Sync
import cats.implicits._
import monix.execution.Scheduler
import org.scalajs.dom
import org.scalajs.dom._
import outwatch.dom.helpers.SnabbdomOps
import outwatch.util.{Reducer, GlobalStore}
import snabbdom.{VNodeProxy, patch}

trait OutWatchOps[F[_]] {
  object OutWatch {
    def toSnabbdom(vNode: VNode)(implicit s: Scheduler, F: Sync[F]): F[VNodeProxy] = Sync[F].delay {
      SnabbdomOps.toSnabbdom(vNode)
    }

    def renderInto(element: dom.Element, vNode: VNode)(implicit s: Scheduler, F: Sync[F]): F[Unit] =
      toSnabbdom(vNode).map { node =>
        val elem = dom.document.createElement("div")
        element.appendChild(elem)
        patch(elem, node)
      }.void

    def renderReplace(element: dom.Element, vNode: VNode)(implicit s: Scheduler, F: Sync[F]): F[Unit] = 
      toSnabbdom(vNode).map { node =>
        val elementNode = snabbdom.tovnode(element)
        patch(elementNode, node)    
      }.void

    def renderInto(querySelector: String, vNode: VNode)(implicit s: Scheduler, F: Sync[F]): F[Unit] =
      renderInto(document.querySelector(querySelector), vNode)

    def renderReplace(querySelector: String, vNode: VNode)(implicit s: Scheduler, F: Sync[F]): F[Unit] =
      renderReplace(document.querySelector(querySelector), vNode)

    @deprecated("Use renderInto instead (or renderReplace)", "0.11.0")
    def render(querySelector: String, vNode: VNode)(implicit s: Scheduler, F: Sync[F]): F[Unit] = 
      renderInto(querySelector, vNode)

    def renderWithStore[A, M](
      initialAction: A,
      initialState: M,
      reducer: Reducer[A, M],
      querySelector: String,
      root: F[VNode]
    )(implicit s: Scheduler, F: Sync[F]): F[Unit] =
      new GlobalStore[F, A, M].renderWithStore(initialAction, initialState, reducer, querySelector, root)

    def renderWithStore[A, M](
      initialAction: A,
      initialState: M,
      reducer: Reducer[A, M],
      querySelector: String,
      root: VNode
    )(implicit s: Scheduler, F: Sync[F]): F[Unit] =
      new GlobalStore[F, A, M].renderWithStore(initialAction, initialState, reducer, querySelector, root.pure[F])
  }
}
