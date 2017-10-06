package outwatch.dom

import cats.Monad
import cats.effect.IO
import org.scalajs.dom._
import outwatch.Sink

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

sealed trait VNodeIO[A] extends VDomModifier {
  private[outwatch]val value: IO[A]
  def flatMap[B](f: A => VNodeIO[B]): VNodeIO[B] =
    Pure(value.flatMap(f andThen (_.value)))
  def map[B](f: A => B): VNodeIO[B] =
    Pure(value.map(f))
}

final case class VDomIO private(private[outwatch] val value: IO[VDom]) extends VNodeIO[VDom]
final case class Handler[A] private(private[outwatch] val value: IO[Observable[A] with Sink[A]]) extends VNodeIO[Observable[A] with Sink[A]]
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
  val asProxy: VNodeProxy
}

object VDomModifier {
  class StringNode(string: String) extends VDom {
    val asProxy: VNodeProxy = VNodeProxy.fromString(string)
  }

  implicit def stringToVNode(s: String): VNode = VDomIO(IO(new StringNode(s)))

  implicit def optionIsEmptyModifier(opt: Option[VDomModifier]): VDomModifier = opt getOrElse EmptyVDomModifier

  final case class VTree(nodeType: String,
                   children: Seq[VNode],
                   attributeObject: DataObject
                  ) extends VDom {


    lazy val childProxies: js.Array[VNodeProxy] = children.map(_.value.unsafeRunSync().asProxy)(breakOut)

    val asProxy = h(nodeType, attributeObject, childProxies)

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



