package outwatch.dom.helpers

import outwatch.reactive._
import snabbdom.VNodeProxy
import org.scalajs.dom

object OutwatchTracing {
  private[outwatch] val patchSubject = SinkSourceHandler.publish[VNodeProxy]
  private[outwatch] val errorSubject = SinkSourceHandler.publish[Throwable]

  // a stream to be notified about snabbdom patches
  def patch: SourceStream[VNodeProxy] = patchSubject

  // a stream about unhandled errors in the reactive part of outwatch, with a
  // default subscription that will print the error to notify the user.
  def error: SourceStream[Throwable] = SourceStream.merge(errorSubject, UnhandledErrorReporter.error).withDefaultSubscription(SinkObserver.create[Throwable](reportError, reportError))

  private def reportError(error: Throwable): Unit =
    dom.console.error(error.getMessage + "\n" + error.getStackTrace.mkString("\n"))
}
