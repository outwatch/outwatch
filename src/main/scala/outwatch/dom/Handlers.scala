package outwatch.dom

import org.scalajs.dom.MouseEvent
import org.scalajs.dom.raw.{ClipboardEvent, DragEvent, KeyboardEvent}
import outwatch.Sink
import outwatch.dom.helpers.InputEvent
import rxscalajs.Observable
import cats.effect.IO

/**
  * Trait containing event handlers, so they can be mixed in to other objects if needed.
  */
trait Handlers {
  type Handler[A] = Observable[A] with Sink[A]

  def createInputHandler() = createHandler[InputEvent]()
  def createMouseHandler() = createHandler[MouseEvent]()
  def createKeyboardHandler() = createHandler[KeyboardEvent]()
  def createDragHandler() = createHandler[DragEvent]()
  def createClipboardHandler() = createHandler[ClipboardEvent]()
  def createStringHandler(defaultValues: String*) = createHandler[String](defaultValues: _*)
  def createBoolHandler(defaultValues: Boolean*) = createHandler[Boolean](defaultValues: _*)
  def createNumberHandler(defaultValues: Double*) = createHandler[Double](defaultValues: _*)

  def createHandler[T](defaultValues: T*): IO[Handler[T]] = Sink.createHandler[T](defaultValues: _*)
}

object Handlers extends Handlers
