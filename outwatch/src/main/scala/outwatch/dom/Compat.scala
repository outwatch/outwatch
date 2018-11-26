package outwatch.dom

import monix.execution.Scheduler
import org.scalajs.dom.{ClipboardEvent, DragEvent, KeyboardEvent, MouseEvent}
import outwatch.dom.helpers.{ChildStreamReceiverBuilder, ChildrenStreamReceiverBuilder}

trait Handlers {
  @deprecated("Use Handler.create[MouseEvent] instead", "0.11.0")
  def createMouseHandler()(implicit s: Scheduler) = Handler.create[MouseEvent]
  @deprecated("Use Handler.create[KeyboardEvent] instead", "0.11.0")
  def createKeyboardHandler()(implicit s: Scheduler) = Handler.create[KeyboardEvent]
  @deprecated("Use Handler.create[DragEvent] instead", "0.11.0")
  def createDragHandler()(implicit s: Scheduler) = Handler.create[DragEvent]
  @deprecated("Use Handler.create[ClipboardEvent] instead", "0.11.0")
  def createClipboardHandler()(implicit s: Scheduler) = Handler.create[ClipboardEvent]

  @deprecated("Use Handler.create[String] instead", "0.11.0")
  def createStringHandler(defaultValues: String)(implicit s: Scheduler) = Handler.create[String](defaultValues)
  @deprecated("Use Handler.create[Boolean] instead", "0.11.0")
  def createBoolHandler(defaultValues: Boolean)(implicit s: Scheduler) = Handler.create[Boolean](defaultValues)
  @deprecated("Use Handler.create[Double] instead", "0.11.0")
  def createNumberHandler(defaultValues: Double)(implicit s: Scheduler) = Handler.create[Double](defaultValues)

  @deprecated("Use Handler.create[T] instead", "0.11.0")
  def createHandler[T](defaultValues: T)(implicit s: Scheduler) = Handler.create[T](defaultValues)
}
object Handlers extends Handlers


/** OutWatch specific attributes used to asign child nodes to a VNode. */
trait OutWatchChildAttributesCompat {
  /** A special attribute that takes a stream of single child nodes. */
  @deprecated("Use the observable directly", "1.0.0")
  def child    = ChildStreamReceiverBuilder

  /** A special attribute that takes a stream of lists of child nodes. */
  @deprecated("Use the observable directly", "1.0.0")
  def children = ChildrenStreamReceiverBuilder
}


trait AttributesCompat extends OutWatchChildAttributesCompat { self: Attributes =>

  @deprecated("Use `type`, tpe or typ instead", "0.11.0")
  def inputType = tpe

  @deprecated("Use styleAttr instead", "0.11.0")
  def style = styleAttr

  @deprecated("Use contentAttr instead", "0.11.0")
  def content = contentAttr

  @deprecated("Use listId instead", "0.11.0")
  def list = listId

  @deprecated("Use onInput.value instead", "0.11.0")
  def inputString = onInput.value

  @deprecated("Use onInput.valueAsNumber instead", "0.11.0")
  def inputNumber = onInput.valueAsNumber

  @deprecated("Use onChange.checked instead", "0.11.0")
  def inputChecked = onChange.checked

  @deprecated("Use onClick instead", "0.11.0")
  def click = onClick

  @deprecated("Use onKeyDown instead", "0.11.0")
  def keydown = onKeyDown

  @deprecated("Use onSnabbdomInsert instead", "0.11.0")
  def insert = onSnabbdomInsert

  @deprecated("Use onSnabbdomPrePatch instead", "0.11.0")
  def prepatch = onSnabbdomPrePatch

  @deprecated("Use onSnabbdomUpdate instead", "0.11.0")
  def update = onSnabbdomUpdate

  @deprecated("Use onSnabbdomPostPatch instead", "0.11.0")
  def postpatch = onSnabbdomPostPatch

  @deprecated("Use onSnabbdomDestroy instead", "0.11.0")
  def destroy = onSnabbdomDestroy
}

trait TagsCompat { self: Tags =>
  @deprecated("Use textArea instead", "0.11.0")
  def textarea = textArea
}
