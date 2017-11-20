package outwatch.dom

import cats.effect.IO
import org.scalajs.dom.{ClipboardEvent, DragEvent, KeyboardEvent, MouseEvent}
import outwatch.dom.helpers.InputEvent

/**
  * Trait containing event handlers, so they can be mixed in to other objects if needed.
  */
trait HandlerFactories {

  @deprecated("Use Handler.inputEvents instead", "0.11.0")
  def createInputHandler() = Pipe.create[InputEvent]
  @deprecated("Use Handler.mouseEvents instead", "0.11.0")
  def createMouseHandler() = Pipe.create[MouseEvent]
  @deprecated("Use Handler.keyboardEvents instead", "0.11.0")
  def createKeyboardHandler() = Pipe.create[KeyboardEvent]
  @deprecated("Use Handler.dragEvents instead", "0.11.0")
  def createDragHandler() = Pipe.create[DragEvent]
  @deprecated("Use Handler.clipboardEvents instead", "0.11.0")
  def createClipboardHandler() = Pipe.create[ClipboardEvent]

  @deprecated("Use Handler.create[String] instead", "0.11.0")
  def createStringHandler(defaultValues: String*) = Pipe.create[String](defaultValues: _*)
  @deprecated("Use Handler.create[Boolean] instead", "0.11.0")
  def createBoolHandler(defaultValues: Boolean*) = Pipe.create[Boolean](defaultValues: _*)
  @deprecated("Use Handler.create[Double] instead", "0.11.0")
  def createNumberHandler(defaultValues: Double*) = Pipe.create[Double](defaultValues: _*)

  @deprecated("Use Handler.create[T] instead", "0.11.0")
  def createHandler[T](defaultValues: T*): IO[Pipe[T, T]] = Pipe.create[T](defaultValues: _*)


  implicit class HandlerCreateHelpers(handler: Handler.type) {
    lazy val inputEvents = Pipe.create[InputEvent]
    lazy val mouseEvents = Pipe.create[MouseEvent]
    lazy val keyboardEvents = Pipe.create[KeyboardEvent]
    lazy val dragEvents = Pipe.create[DragEvent]
    lazy val clipboardEvents = Pipe.create[ClipboardEvent]
  }

}

