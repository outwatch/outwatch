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

  implicit class RichHandler[T](handler: Handler[T]) {
    def comap(read: Observable[T] => Observable[T]): Handler[T] = {
      Sink.ObservableSink(handler, read(handler))
    }

    def comapMap(read: T => T): Handler[T] = {
      Sink.ObservableSink(handler, handler.map(read))
    }

    def contramap(write: Observable[T] => Observable[T]): Handler[T] = {
      Sink.ObservableSink(handler.redirect[T](write), handler)
    }

    def contramapMap(write: T => T): Handler[T] = {
      Sink.ObservableSink(handler.redirect[T](_.map(write)), handler)
    }


    def imap[S](read: Observable[T] => Observable[S])(write: Observable[S] => Observable[T]): Handler[S] = {
      Sink.ObservableSink(handler.redirect[S](write), read(handler))
    }

    def imapMap[S](read: T => S)(write: S => T): Handler[S] = {
      Sink.ObservableSink(handler.redirect[S](_.map(write)), handler.map(read))
    }

    def lens[S](seed: T)(read: T => S)(write: (T, S) => T): Handler[S] = {
      val redirected = handler.redirect[S](_.withLatestFrom(handler.startWith(seed)).map { case (a,b) => write(b,a) })
      Sink.ObservableSink(redirected, handler.map(read))
    }
  }
}

object Handlers extends Handlers
