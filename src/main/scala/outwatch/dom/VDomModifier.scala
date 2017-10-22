package outwatch.dom

import cats.Monad
import cats.effect.IO
import org.scalajs.dom._
import outwatch.Sink
import rxscalajs.dom.Response
import outwatch.dom.helpers.DomUtils
import scala.scalajs.js.|
import rxscalajs.{Observable, Observer}
import snabbdom.{DataObject, VNodeProxy, h}

import scala.scalajs.js
import collection.breakOut

sealed trait VDomModifier extends Any

sealed trait Emitter extends VDomModifier {
  val eventType: String
}

sealed trait Property extends VDomModifier

sealed trait Receiver extends VDomModifier

sealed trait VNodeIO[A] extends VDomModifier { self =>
  private[outwatch]val value: IO[A]
  def flatMap[B](f: A => VNodeIO[B]): VNodeIO[B] =
    Pure(value.flatMap(f andThen (_.value)))
  def map[B](f: A => B): VNodeIO[B] =
    Pure(value.map(f))

  //TODO only valid for VDomIO, others should never happen
  def apply(args: VDomModifier*): VNode = div(args: _*)
}

final case class VDomIO private(private[outwatch] val value: IO[VDom]) extends VNodeIO[VDom] {
  override def apply(args: VDomModifier*): VNode = VDomIO(value.map(vdom => vdom.update(args: _*)))
}
final case class Handler[A] private(private[outwatch] val value: IO[Observable[A] with Sink[A]]) extends VNodeIO[Observable[A] with Sink[A]]
final case class VDomHttp private(private[outwatch] val value: IO[Observable[Response]]) extends VNodeIO[Observable[Response]]
final case class Pure[A](value: IO[A]) extends VNodeIO[A]

final case class EventEmitter[E <: Event](eventType: String, sink: Observer[E]) extends Emitter
final case class StringEventEmitter(eventType: String, sink: Observer[String]) extends Emitter
final case class BoolEventEmitter(eventType: String, sink: Observer[Boolean]) extends Emitter
final case class NumberEventEmitter(eventType: String, sink: Observer[Double]) extends Emitter

sealed trait Attribute extends Property {
  val title: String
}

object Attribute {
  def apply(title: String, value: String | Boolean) = Attr(title, value)
}

final case class Attr(title: String, value: String | Boolean) extends Attribute
final case class Prop(title: String, value: String) extends Attribute
final case class Style(title: String, value: String) extends Attribute
final case class InsertHook(sink: Observer[Element]) extends Property
final case class DestroyHook(sink: Observer[Element]) extends Property
final case class UpdateHook(sink: Observer[(Element, Element)]) extends Property
final case class Key(value: String) extends Property

final case class AttributeStreamReceiver(attribute: String, attributeStream: Observable[Attribute]) extends Receiver
final case class ChildStreamReceiver(childStream: Observable[VNode]) extends Receiver
final case class ChildrenStreamReceiver(childrenStream: Observable[Seq[VNode]]) extends Receiver

case object EmptyVDomModifier extends VDomModifier

sealed trait VDom {
  def asProxy: VNodeProxy
  def update(args: VDomModifier*): VDom
}

object VDomModifier {
  class StringNode(string: String) extends VDom {
    val asProxy: VNodeProxy = VNodeProxy.fromString(string)
    def update(args: VDomModifier*) = this
  }

  implicit def stringToVNode(s: String): VNode = VDomIO(IO(new StringNode(s)))

  implicit def optionIsEmptyModifier(opt: Option[VDomModifier]): VDomModifier = opt getOrElse EmptyVDomModifier

  // TODO: instead of Seq[VDomModifier] use Vector or JSArray?
  // Fast concatenation and lastOption operations are important
  // Needs to be benchmarked in the Browser
  final case class VTree(nodeType: String,
                         modifiers: Seq[VDomModifier]) extends VDom {

    def asProxy = {
      val (children, attributeObject) = DomUtils.extractChildrenAndDataObject(modifiers)
      //TODO: use .sequence instead of unsafeRunSync?
      // import cats.instances.list._
      // import cats.syntax.traverse._
      // for { childProxies <- children.map(_.value).sequence }
      // yield h(..., childProxies.map(_.apsProxy)(breakOut))
      val childProxies: js.Array[VNodeProxy] = children.map(_.value.unsafeRunSync().asProxy)(breakOut)
      h(nodeType, attributeObject, childProxies)
    }

    def update(args: VDomModifier*): VDom = VTree(nodeType, modifiers ++ args)

  }
}

object VNodeIO {
  implicit def vNodeMonad: Monad[VNodeIO] = new Monad[VNodeIO] {
    def flatMap[A, B](fa: VNodeIO[A])(f: (A) => VNodeIO[B]): VNodeIO[B] =
      fa.flatMap(f)

    def tailRecM[A, B](a: A)(f: (A) => VNodeIO[Either[A, B]]) =
      Pure(Monad[IO].tailRecM(a)(f andThen (_.value)))

    def pure[A](x: A) = Pure(IO.pure(x))
  }
}



