package outwatch.dom

import cats.effect.Effect
import monix.execution.Scheduler
import org.scalajs.dom.{ClipboardEvent, DragEvent, KeyboardEvent, MouseEvent}

trait Handlers {
  @deprecated("Use Handler.create[MouseEvent] instead", "0.11.0")
  def createMouseHandler[F[_]:Effect]()(implicit s: Scheduler) = Handler.create[MouseEvent,F]
  @deprecated("Use Handler.create[KeyboardEvent] instead", "0.11.0")
  def createKeyboardHandler[F[_]:Effect]()(implicit s: Scheduler) = Handler.create[KeyboardEvent,F]
  @deprecated("Use Handler.create[DragEvent] instead", "0.11.0")
  def createDragHandler[F[_]:Effect]()(implicit s: Scheduler) = Handler.create[DragEvent,F]
  @deprecated("Use Handler.create[ClipboardEvent] instead", "0.11.0")
  def createClipboardHandler[F[_]:Effect]()(implicit s: Scheduler) = Handler.create[ClipboardEvent,F]

  @deprecated("Use Handler.create[String] instead", "0.11.0")
  def createStringHandler[F[_]:Effect](defaultValues: String*)(implicit s: Scheduler) = Handler.create[String,F](defaultValues: _*)
  @deprecated("Use Handler.create[Boolean] instead", "0.11.0")
  def createBoolHandler[F[_]:Effect](defaultValues: Boolean*)(implicit s: Scheduler) = Handler.create[Boolean,F](defaultValues: _*)
  @deprecated("Use Handler.create[Double] instead", "0.11.0")
  def createNumberHandler[F[_]:Effect](defaultValues: Double*)(implicit s: Scheduler) = Handler.create[Double,F](defaultValues: _*)

  @deprecated("Use Handler.create[T] instead", "0.11.0")
  def createHandler[F[_]:Effect, T](defaultValues: T*)(implicit s: Scheduler): F[Pipe[T, T]] = Handler.create[T,F](defaultValues: _*)
}
object Handlers extends Handlers

trait AttributesCompat { self: Attributes =>

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

  @deprecated("Use onPrepatch instead", "0.11.0")
  lazy val prepatch = onPrePatch

  @deprecated("Use onUpdate instead", "0.11.0")
  lazy val update = onUpdate

  @deprecated("Use onPostpatch instead", "0.11.0")
  lazy val postpatch = onPostPatch

  @deprecated("Use onDestroy instead", "0.11.0")
  lazy val destroy = onDestroy
}

trait TagsCompat[F[_]] { self: Tags[F] =>
  @deprecated("Use textArea instead", "0.11.0")
  lazy val textarea = textArea
}
