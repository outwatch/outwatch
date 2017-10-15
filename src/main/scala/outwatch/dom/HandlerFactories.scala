package outwatch.dom

import org.scalajs.dom.{ClipboardEvent, DragEvent, KeyboardEvent, MouseEvent}

/**
  * Trait containing event handlers, so they can be mixed in to other objects if needed.
  */
trait HandlerFactories extends Handlers {

  implicit class HandlerCreateHelpers(handler: Handler.type) {
    def mouseEvents = Handler.create[MouseEvent]
    def keyboardEvents = Handler.create[KeyboardEvent]
    def dragEvents = Handler.create[DragEvent]
    def clipboardEvents = Handler.create[ClipboardEvent]
  }

}

