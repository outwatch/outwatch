package outwatch.dom

import cats.effect.IO
import com.raquo.domtypes.generic.defs.reflectedAttrs.ReflectedAttrs
import org.scalajs.dom.{ClipboardEvent, DragEvent, KeyboardEvent, MouseEvent}

import scala.language.higherKinds

trait Handlers {
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
}

object Handlers extends Handlers


trait Attributes extends
  DomAttrs with
  DomReflectedAttrs with
  DomProps with
  DomEvents with
  OutwatchAttributes

@deprecated("Attributes is deprecated, use one of DomAttrs, DomReflectedAttrs,  DomProps, DomEvents or OutwatchAttributes", "0.11.0")
object Attributes extends Attributes


trait DomAttrsCompat[RA[_, _]] { self: ReflectedAttrs[RA] =>
  lazy val `class`: RA[String, String] = className

  lazy val `for`: RA[String, String] = forId
}

trait Tags extends DomTags with DomTagsExtra
object Tags extends Tags
