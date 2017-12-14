package outwatch.dom

import com.raquo.domtypes.generic.builders._
import com.raquo.domtypes.generic.keys
import com.raquo.domtypes.generic.codecs._
import com.raquo.domtypes.generic.defs.attrs
import com.raquo.domtypes.generic.defs.reflectedAttrs
import com.raquo.domtypes.generic.defs.props
import com.raquo.domtypes.generic.defs.styles
import com.raquo.domtypes.generic.defs.sameRefTags._
import com.raquo.domtypes.jsdom.defs.eventProps._
import cats.effect.IO
import org.scalajs.dom
import helpers._
import monix.execution.{Ack, Cancelable}
import monix.reactive.OverflowStrategy.Unbounded

import scala.scalajs.js

private[outwatch] object DomTypesBuilder {
  type VNode = IO[VTree]
  type GenericVNode[T] = VNode

  trait VNodeBuilder extends TagBuilder[GenericVNode, VNode] {
    // we can ignore information about void tags here, because snabbdom handles this automatically for us based on the tagname.
    protected override def tag[Ref <: VNode](tagName: String, void: Boolean): VNode = IO.pure(VTree(tagName, Seq.empty))
  }

  object CodecBuilder {
    type Attribute[T, _] = AttributeBuilder[T]
    type Property[T, _] = PropertyBuilder[T]

    def encodeAttribute[V](codec: Codec[V, String]): V => Attr.Value = codec match {
      //The BooleanAsAttrPresenceCodec does not play well with snabbdom. it
      //encodes true as "" and false as null, whereas snabbdom needs true/false
      //of type boolean (not string) for toggling the presence of the attribute.
      case _: BooleanAsAttrPresenceCodec.type => identity
      case _ => codec.encode
    }
  }

  abstract class ObservableEventPropBuilder(target: dom.EventTarget) extends EventPropBuilder[Observable, dom.Event] {
    override def eventProp[V <: dom.Event](key: String): Observable[V] = Observable.create(Unbounded) { obs =>
      val eventHandler: js.Function1[V, Ack] = obs.onNext _
      target.addEventListener(key, eventHandler)
      Cancelable(() => target.removeEventListener(key, eventHandler))
    }
  }
}
import DomTypesBuilder._

trait Tags
  extends EmbedTags[GenericVNode, VNode]
  with GroupingTags[GenericVNode, VNode]
  with TextTags[GenericVNode, VNode]
  with FormTags[GenericVNode, VNode]
  with SectionTags[GenericVNode, VNode]
  with TableTags[GenericVNode, VNode]
  with VNodeBuilder
  with TagHelpers
  with TagsCompat

@deprecated("Use dsl.tags instead", "0.11.0")
object Tags extends Tags

trait TagsExtra
  extends DocumentTags[GenericVNode, VNode]
  with MiscTags[GenericVNode, VNode]
  with VNodeBuilder

trait Attributes
  extends Attrs
  with ReflectedAttrs
  with Props
  with Events
  with AttributeHelpers
  with OutwatchAttributes
  with AttributesCompat

@deprecated("Use dsl.attributes instead", "0.11.0")
object Attributes extends Attributes

// Attrs
trait Attrs
  extends attrs.Attrs[AttributeBuilder]
  with AttrBuilder[AttributeBuilder] {

  override protected def attr[V](key: String, codec: Codec[V, String]): AttributeBuilder[V] =
    new AttributeBuilder(key, CodecBuilder.encodeAttribute(codec))
}

// Reflected attrs

trait ReflectedAttrs
  extends reflectedAttrs.ReflectedAttrs[CodecBuilder.Attribute]
  with ReflectedAttrBuilder[CodecBuilder.Attribute] {

  override protected def reflectedAttr[V, DomPropV](
    attrKey: String,
    propKey: String,
    attrCodec: Codec[V, String],
    propCodec: Codec[V, DomPropV]
  ): AttributeBuilder[V] =
    new AttributeBuilder(attrKey, CodecBuilder.encodeAttribute(attrCodec))
    //or: new PropertyBuilder(propKey, propCodec.encode)
}

// Props
trait Props
  extends props.Props[CodecBuilder.Property]
  with PropBuilder[CodecBuilder.Property] {

  override protected def prop[V, DomV](key: String, codec: Codec[V, DomV]): PropertyBuilder[V] =
    new PropertyBuilder(key, codec.encode)
}

trait Events
  extends HTMLElementEventProps[SimpleEmitterBuilder]
  with EventPropBuilder[SimpleEmitterBuilder, dom.Event] {

  override def eventProp[V <: dom.Event](key: String): SimpleEmitterBuilder[V] =  EmitterBuilder[V](key)
}

abstract class WindowEvents
  extends ObservableEventPropBuilder(dom.window)
  with WindowEventProps[Observable]

abstract class DocumentEvents
  extends ObservableEventPropBuilder(dom.document)
  with DocumentEventProps[Observable]


private[outwatch] trait SimpleStyleBuilder extends StyleBuilders[IO[Style]] {

  override protected def buildDoubleStyleSetter(style: keys.Style[Double], value: Double): IO[Style] = style := value
  override protected def buildIntStyleSetter(style: keys.Style[Int],value: Int): IO[Style] = style := value
  override protected def buildStringStyleSetter(style: keys.Style[_],value: String): IO[Style] = new StyleBuilder[Any](style.cssName) := value
}


trait Styles
  extends styles.Styles[IO[Style]]
  with SimpleStyleBuilder

trait StylesExtra
  extends styles.Styles2[IO[Style]]
  with SimpleStyleBuilder
