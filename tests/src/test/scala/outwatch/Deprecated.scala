package outwatch

import org.scalajs.dom.Event

object Deprecated {
  @deprecated("", "") trait IgnoreWarnings {
    def initEvent(e: Event)(eventTypeArg: String, canBubbleArg: Boolean,
      cancelableArg: Boolean): Unit = e.initEvent(eventTypeArg, canBubbleArg, cancelableArg)
  }

  object IgnoreWarnings extends IgnoreWarnings
}
