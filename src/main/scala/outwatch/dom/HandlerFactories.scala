package outwatch.dom

import cats.effect.IO
import org.scalajs.dom.{ClipboardEvent, DragEvent, KeyboardEvent, MouseEvent}
import outwatch.dom.helpers.InputEvent

/**
  * Trait containing event handlers, so they can be mixed in to other objects if needed.
  */
trait HandlerFactories {

  @deprecated("Use Handler.inputEvents instead", "0.11.0")
  def createInputHandler() = Handler.create[InputEvent]
  @deprecated("Use Handler.mouseEvents instead", "0.11.0")
  def createMouseHandler() = Handler.create[MouseEvent]
  @deprecated("Use Handler.keyboardEvents instead", "0.11.0")
  def createKeyboardHandler() = Handler.create[KeyboardEvent]
  @deprecated("Use Handler.dragEvents instead", "0.11.0")
  def createDragHandler() = Handler.create[DragEvent]
  @deprecated("Use Handler.clipboardEvents instead", "0.11.0")
  def createClipboardHandler() = Handler.create[ClipboardEvent]

  @deprecated("Use Handler.create[String] instead", "0.11.0")
  def createStringHandler(defaultValues: String*) = Handler.create[String](defaultValues: _*)
  @deprecated("Use Handler.create[Boolean] instead", "0.11.0")
  def createBoolHandler(defaultValues: Boolean*) = Handler.create[Boolean](defaultValues: _*)
  @deprecated("Use Handler.create[Double] instead", "0.11.0")
  def createNumberHandler(defaultValues: Double*) = Handler.create[Double](defaultValues: _*)

  @deprecated("Use Handler.create[T] instead", "0.11.0")
  def createHandler[T](defaultValues: T*): IO[Pipe[T, T]] = Handler.create[T](defaultValues: _*)


  implicit class HandlerCreateHelpers(handler: Handler.type) {
    lazy val inputEvents = Handler.create[InputEvent]
    lazy val mouseEvents = Handler.create[MouseEvent]
    lazy val keyboardEvents = Handler.create[KeyboardEvent]
    lazy val dragEvents = Handler.create[DragEvent]
    lazy val clipboardEvents = Handler.create[ClipboardEvent]
  }

}

