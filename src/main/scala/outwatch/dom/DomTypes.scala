package outwatch.dom

import cats.Applicative
import com.raquo.domtypes.generic.builders
import com.raquo.domtypes.generic.keys
import com.raquo.domtypes.generic.codecs
import com.raquo.domtypes.generic.defs.attrs
import com.raquo.domtypes.generic.defs.reflectedAttrs
import com.raquo.domtypes.generic.defs.props
import com.raquo.domtypes.generic.defs.styles
import com.raquo.domtypes.generic.defs.sameRefTags._
import com.raquo.domtypes.jsdom.defs.eventProps
import cats.effect.{Effect, IO}
import org.scalajs.dom
import helpers._
import monix.execution.{Ack, Cancelable}
import monix.reactive.OverflowStrategy.Unbounded

import scala.scalajs.js

private[outwatch] object BuilderTypes {
  type Attribute[F[+_], T, _] = helpers.AttributeBuilder[F, T, Attr]
  type Property[F[+_], T, _] = helpers.PropBuilder[F, T]
  type EventEmitter[E <: dom.Event] = SimpleEmitterBuilder[E, Emitter]
}

private[outwatch] object CodecBuilder {
  def encodeAttribute[V](codec: codecs.Codec[V, String]): V => Attr.Value = codec match {
    //The BooleanAsAttrPresenceCodec does not play well with snabbdom. it
    //encodes true as "" and false as null, whereas snabbdom needs true/false
    //of type boolean (not string) for toggling the presence of the attribute.
    case _: codecs.BooleanAsAttrPresenceCodec.type => identity
    case _ => codec.encode
  }
}

// Tags

private[outwatch] trait TagBuilder[F[+_]] extends builders.TagBuilder[TagBuilder.Tag[F, ?], VTree[F]] {
  implicit def effectF: Effect[F]
  // we can ignore information about void tags here, because snabbdom handles this automatically for us based on the tagname.
  protected override def tag[Ref <: VTree[F]](tagName: String, void: Boolean): VTree[F] = VTree[F](tagName, Seq.empty)
}
private[outwatch] object TagBuilder {
  type Tag[F[+_], T] = VTree[F]
}

trait Tags[F[+_]]
  extends TagBuilder[F]
  with EmbedTags[TagBuilder.Tag[F, ?], VTree[F]]
  with GroupingTags[TagBuilder.Tag[F, ?], VTree[F]]
  with TextTags[TagBuilder.Tag[F, ?], VTree[F]]
  with FormTags[TagBuilder.Tag[F, ?], VTree[F]]
  with SectionTags[TagBuilder.Tag[F, ?], VTree[F]]
  with TableTags[TagBuilder.Tag[F, ?], VTree[F]]
  with TagHelpers[F]

@deprecated("Use dsl.tags instead", "0.11.0")
object Tags extends TagBuilder[IO] with Tags[IO] {
  implicit val effectF: Effect[IO] = IO.ioEffect
}

trait TagsExtra[F[+_]]
  extends MiscTags[TagBuilder.Tag[F, ?], VTree[F]]
  with DocumentTags[TagBuilder.Tag[F, ?], VTree[F]] { this: TagBuilder[F] =>}

// all Attributes

trait Attributes[F[+_]]
  extends Attrs[F]
  with ReflectedAttrs[F]
  with Props[F]
  with Events
  with AttributeHelpers[F]
  with OutwatchAttributes

@deprecated("Use dsl.attributes instead", "0.11.0")
object Attributes extends Attributes[IO]

// Attrs
trait Attrs[F[+_]]
  extends attrs.Attrs[BasicAttrBuilder[F, ?]]
  with builders.AttrBuilder[BasicAttrBuilder[F, ?]] {

  override protected def attr[V](key: String, codec: codecs.Codec[V, String]): BasicAttrBuilder[F, V] =
    new BasicAttrBuilder(key, CodecBuilder.encodeAttribute(codec))
}

// Reflected attrs
trait ReflectedAttrs[F[+_]]
  extends reflectedAttrs.ReflectedAttrs[BuilderTypes.Attribute[F, ?, ?]]
  with builders.ReflectedAttrBuilder[BuilderTypes.Attribute[F, ?, ?]] {

  // super.className.accum(" ") would have been nicer, but we can't do super.className on a lazy val
  override lazy val className = new AccumAttrBuilder[F, String]("class",
    stringReflectedAttr(attrKey = "class", propKey = "className"),
    _ + " " + _
  )

  override protected def reflectedAttr[V, DomPropV](
    attrKey: String,
    propKey: String,
    attrCodec: codecs.Codec[V, String],
    propCodec: codecs.Codec[V, DomPropV]
  ): BasicAttrBuilder[F, V] = new BasicAttrBuilder(attrKey, CodecBuilder.encodeAttribute(attrCodec))
    //or: new PropertyBuilder(propKey, propCodec.encode)
}

// Props
trait Props[F[+_]]
  extends props.Props[BuilderTypes.Property[F, ?, ?]]
  with builders.PropBuilder[BuilderTypes.Property[F, ?, ?]] {

  override protected def prop[V, DomV](key: String, codec: codecs.Codec[V, DomV]): PropBuilder[F, V] =
    new PropBuilder(key, codec.encode)
}


// Events
trait Events
  extends eventProps.HTMLElementEventProps[BuilderTypes.EventEmitter]
  with builders.EventPropBuilder[BuilderTypes.EventEmitter, dom.Event] {

  override def eventProp[V <: dom.Event](key: String): BuilderTypes.EventEmitter[V] = EmitterBuilder[V](key)
}


// Window / Document events

private[outwatch] abstract class ObservableEventPropBuilder(target: dom.EventTarget)
  extends builders.EventPropBuilder[Observable, dom.Event] {
  override def eventProp[V <: dom.Event](key: String): Observable[V] = Observable.create(Unbounded) { obs =>
    val eventHandler: js.Function1[V, Ack] = obs.onNext _
    target.addEventListener(key, eventHandler)
    Cancelable(() => target.removeEventListener(key, eventHandler))
  }
}

abstract class WindowEvents
  extends ObservableEventPropBuilder(dom.window)
  with eventProps.WindowEventProps[Observable]

abstract class DocumentEvents
  extends ObservableEventPropBuilder(dom.document)
  with eventProps.DocumentEventProps[Observable]

// Styles

private[outwatch] trait SimpleStyleBuilder[F[+_]] extends builders.StyleBuilders[F[Style]] {
  implicit def applicativeF: Applicative[F]

  override protected def buildDoubleStyleSetter(style: keys.Style[Double], value: Double): F[Style] =
    style := value
  override protected def buildIntStyleSetter(style: keys.Style[Int], value: Int): F[Style] =
    style := value
  override protected def buildStringStyleSetter(style: keys.Style[_], value: String): F[Style] =
    new BasicStyleBuilder[F, Any](style.cssName) := value
}

trait Styles[F[+_]]
  extends SimpleStyleBuilder[F]
  with styles.Styles[F[Style]]

trait StylesExtra[F[+_]]
  extends SimpleStyleBuilder[F]
  with styles.Styles2[F[Style]]
