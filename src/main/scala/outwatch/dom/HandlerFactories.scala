package outwatch.dom

import org.scalajs.dom.{ClipboardEvent, DragEvent, KeyboardEvent, MouseEvent}
import outwatch.dom.helpers.InputEvent

/**
  * Trait containing event handlers, so they can be mixed in to other objects if needed.
  */
trait HandlerFactories extends Handlers {

  implicit class HandlerCreateHelpers(handler: Handler.type) {
    lazy val inputEvents = Handler.create[InputEvent]
    lazy val mouseEvents = Handler.create[MouseEvent]
    lazy val keyboardEvents = Handler.create[KeyboardEvent]
    lazy val dragEvents = Handler.create[DragEvent]
    lazy val clipboardEvents = Handler.create[ClipboardEvent]
  }

}

