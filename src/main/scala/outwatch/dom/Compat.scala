package outwatch.dom

import cats.effect.IO
import monix.execution.Scheduler
import org.scalajs.dom.{ClipboardEvent, DragEvent, KeyboardEvent, MouseEvent}
import outwatch.dom.helpers.{ChildStreamReceiverBuilder, ChildrenStreamReceiverBuilder}

trait Handlers {
  @deprecated("Use Handler.create[MouseEvent] instead", "0.11.0")
  def createMouseHandler()(implicit s: Scheduler) = Handler.create[IO, MouseEvent]
  @deprecated("Use Handler.create[KeyboardEvent] instead", "0.11.0")
  def createKeyboardHandler()(implicit s: Scheduler) = Handler.create[IO, KeyboardEvent]
  @deprecated("Use Handler.create[DragEvent] instead", "0.11.0")
  def createDragHandler()(implicit s: Scheduler) = Handler.create[IO, DragEvent]
  @deprecated("Use Handler.create[ClipboardEvent] instead", "0.11.0")
  def createClipboardHandler()(implicit s: Scheduler) = Handler.create[IO, ClipboardEvent]

  @deprecated("Use Handler.create[String] instead", "0.11.0")
  def createStringHandler(defaultValues: String*)(implicit s: Scheduler) = Handler.create[IO, String](defaultValues: _*)
  @deprecated("Use Handler.create[Boolean] instead", "0.11.0")
  def createBoolHandler(defaultValues: Boolean*)(implicit s: Scheduler) = Handler.create[IO, Boolean](defaultValues: _*)
  @deprecated("Use Handler.create[Double] instead", "0.11.0")
  def createNumberHandler(defaultValues: Double*)(implicit s: Scheduler) = Handler.create[IO, Double](defaultValues: _*)

  @deprecated("Use Handler.create[T] instead", "0.11.0")
  def createHandler[T](defaultValues: T*)(implicit s: Scheduler): IO[Pipe[T, T]] = Handler.create[IO, T](defaultValues: _*)
}
object Handlers extends Handlers


/** OutWatch specific attributes used to asign child nodes to a VNode. */
trait OutWatchChildAttributesCompat {
  /** A special attribute that takes a stream of single child nodes. */
  @deprecated("Use the observable directly", "1.0.0")
  lazy val child    = ChildStreamReceiverBuilder

  /** A special attribute that takes a stream of lists of child nodes. */
  @deprecated("Use the observable directly", "1.0.0")
  lazy val children = ChildrenStreamReceiverBuilder
}


trait AttributesCompat extends OutWatchChildAttributesCompat { self: Attributes =>

  @deprecated("Use `type`, tpe or typ instead", "0.11.0")
  lazy val inputType = tpe

  @deprecated("Use styleAttr instead", "0.11.0")
  lazy val style = styleAttr

  @deprecated("Use contentAttr instead", "0.11.0")
  lazy val content = contentAttr

  @deprecated("Use listId instead", "0.11.0")
  lazy val list = listId

  @deprecated("Use onInput.value instead", "0.11.0")
  lazy val inputString = onInput.value

  @deprecated("Use onInput.valueAsNumber instead", "0.11.0")
  lazy val inputNumber = onInput.valueAsNumber

  @deprecated("Use onChange.checked instead", "0.11.0")
  lazy val inputChecked = onChange.checked

  @deprecated("Use onClick instead", "0.11.0")
  lazy val click = onClick

  @deprecated("Use onKeyDown instead", "0.11.0")
  lazy val keydown = onKeyDown

  @deprecated("Use onInsert instead", "0.11.0")
  lazy val insert = onInsert

  @deprecated("Use onPrePatch instead", "0.11.0")
  lazy val prepatch = onPrePatch

  @deprecated("Use onUpdate instead", "0.11.0")
  lazy val update = onUpdate

  @deprecated("Use onPostPatch instead", "0.11.0")
  lazy val postpatch = onPostPatch

  @deprecated("Use onDestroy instead", "0.11.0")
  lazy val destroy = onDestroy
}

trait TagsCompat { self: Tags[IO] =>
  @deprecated("Use textArea instead", "0.11.0")
  lazy val textarea = textArea
}
