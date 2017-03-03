package outwatch

import org.scalajs.dom.raw.{ClipboardEvent, DragEvent, KeyboardEvent}
import org.scalajs.dom.MouseEvent
import rxscalajs.Observable
import outwatch.dom.helpers._

/**
  *
  */
package object dom extends Attributes with Tags {


  def createInputHandler() = createHandler[InputEvent]()
  def createMouseHandler() = createHandler[MouseEvent]()
  def createKeyboardHandler() = createHandler[KeyboardEvent]()
  def createDragHandler() = createHandler[DragEvent]()
  def createClipboardHandler() = createHandler[ClipboardEvent]()
  def createStringHandler(defaultValues: String*) = createHandler[String](defaultValues: _*)
  def createBoolHandler(defaultValues: Boolean*) = createHandler[Boolean](defaultValues: _*)
  def createNumberHandler(defaultValues: Double*) = createHandler[Double](defaultValues: _*)



  def createHandler[T](defaultValues: T*): Observable[T] with Sink[T] = {
    Sink.createHandler[T](defaultValues: _*)
  }




}
