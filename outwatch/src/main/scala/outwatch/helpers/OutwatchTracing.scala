package outwatch.helpers

import colibri._
import colibri.helpers.UnhandledErrorReporter
import snabbdom.VNodeProxy
import org.scalajs.dom

object OutwatchTracing {
  private[outwatch] val patchSubject = Subject.publish[VNodeProxy]()
  private[outwatch] val errorSubject = Subject.publish[Throwable]()

  // a stream to be notified about snabbdom patches
  def patch: Observable[VNodeProxy] = patchSubject

  // a stream about unhandled errors in the reactive part of outwatch, with a
  // default subscription that will print the error to notify the user.
  val error: Observable[Throwable] = Observable.merge(errorSubject, UnhandledErrorReporter.error).mergeFailed.tap(reportError).publish.unsafeHot

  private def reportError(error: Throwable): Unit =
    dom.console.error(error.toString, error.getMessage, error.getStackTrace.mkString("\n"))
}
