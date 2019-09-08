package outwatch.dom.definitions

import com.raquo.domtypes.generic.defs.complex.canonical
import com.raquo.domtypes.generic.defs.{attrs, props, reflectedAttrs, styles}
import com.raquo.domtypes.generic.{builders, codecs, keys}
import com.raquo.domtypes.jsdom.defs.eventProps
import com.raquo.domtypes.jsdom.defs.tags._
import org.scalajs.dom
import outwatch.dom._
import outwatch.dom.helpers._
import outwatch.reactive.EventSourceStream

import scala.scalajs.js

private[outwatch] object BuilderTypes {
  type Attribute[T, _] = AttributeBuilder[T, Attr]
  type Property[T, _] = PropBuilder[T]
  type EventEmitter[E <: dom.Event] = EmitterBuilder.Sync[E, VDomModifier]
}

private[outwatch] object CodecBuilder {
  def encodeAttribute[V](codec: codecs.Codec[V, String]): V => Attr.Value = codec match {
    //The BooleanAsAttrPresenceCodec does not play well with snabbdom. it
    //encodes true as "" and false as null, whereas snabbdom needs true/false
    //of type boolean (not string) for toggling the presence of the attribute.
    case _: codecs.BooleanAsAttrPresenceCodec.type => identity
    case _: codecs.IntAsStringCodec.type => identity
    case _: codecs.DoubleAsStringCodec.type => identity
    case _ => codec.encode
  }
}

// Tags

private[outwatch] trait TagBuilder extends builders.HtmlTagBuilder[TagBuilder.Tag, dom.html.Element] with builders.SvgTagBuilder[TagBuilder.Tag, dom.svg.Element] {
  // we can ignore information about void tags here, because snabbdom handles this automatically for us based on the tagname.
  //TODO: add element type to VTree for typed interface
  protected override def htmlTag[Ref <: dom.html.Element](tagName: String, void: Boolean): HtmlVNode = HtmlVNode(tagName, js.Array())
  protected override def svgTag[Ref <: dom.svg.Element](tagName: String, void: Boolean): SvgVNode = SvgVNode(tagName, js.Array())
}

private[outwatch] object TagBuilder {
  type Tag[T] = BasicVNode
}

trait Tags
  extends EmbedTags[TagBuilder.Tag]
  with GroupingTags[TagBuilder.Tag]
  with TextTags[TagBuilder.Tag]
  with FormTags[TagBuilder.Tag]
  with SectionTags[TagBuilder.Tag]
  with TableTags[TagBuilder.Tag]
  with TagBuilder
  with TagHelpers

trait TagsExtra
  extends DocumentTags[TagBuilder.Tag]
  with MiscTags[TagBuilder.Tag]
  with TagBuilder

trait TagsSvg
  extends SvgTags[TagBuilder.Tag]
  with TagBuilder

// all Attributes
trait Attributes
  extends HtmlAttrs
  with Events
  with AttributeHelpers
  with OutwatchAttributes

// Html Attrs
trait HtmlAttrs
  extends attrs.HtmlAttrs[BasicAttrBuilder]
  with reflectedAttrs.ReflectedHtmlAttrs[BuilderTypes.Attribute]
  with props.Props[BuilderTypes.Property]
  with canonical.CanonicalComplexHtmlKeys[BuilderTypes.Attribute, BasicAttrBuilder, BuilderTypes.Property]
  with builders.HtmlAttrBuilder[BasicAttrBuilder]
  with builders.ReflectedHtmlAttrBuilder[BuilderTypes.Attribute]
  with builders.PropBuilder[BuilderTypes.Property] {

  override protected def htmlAttr[V](key: String, codec: codecs.Codec[V, String]): BasicAttrBuilder[V] =
    new BasicAttrBuilder(key, CodecBuilder.encodeAttribute(codec))

  override protected def reflectedAttr[V, DomPropV](
    attrKey: String,
    propKey: String,
    attrCodec: codecs.Codec[V, String],
    propCodec: codecs.Codec[V, DomPropV]
  ): BasicAttrBuilder[V] = new BasicAttrBuilder(attrKey, CodecBuilder.encodeAttribute(attrCodec))
  //or: new PropertyBuilder(propKey, propCodec.encode)

  override protected def prop[V, DomV](key: String, codec: codecs.Codec[V, DomV]): PropBuilder[V] =
    new PropBuilder(key, codec.encode)

  // super.className.accum(" ") would have been nicer, but we can't do super.className on a lazy val
  override lazy val className = new AccumAttrBuilder[String](
    "class",
    reflectedAttr(attrKey = "class", propKey = "className", attrCodec = codecs.StringAsIsCodec, propCodec = codecs.StringAsIsCodec), (v1, v2) => s"$v1 $v2"
  )
}

// Svg Attrs
trait SvgAttrs
  extends attrs.SvgAttrs[BasicAttrBuilder]
  with builders.SvgAttrBuilder[BasicAttrBuilder] {

  // According to snabbdom documentation, the namespace can be ignore as it is handled automatically.
  override protected def svgAttr[V](key: String, codec: codecs.Codec[V, String], namespace: Option[String]): BasicAttrBuilder[V] =
    new BasicAttrBuilder(key, CodecBuilder.encodeAttribute(codec))
}

// Events
trait Events
  extends eventProps.HTMLElementEventProps[BuilderTypes.EventEmitter]
  with builders.EventPropBuilder[BuilderTypes.EventEmitter, dom.Event] {

  override def eventProp[V <: dom.Event](key: String): BuilderTypes.EventEmitter[V] =  EmitterBuilder.fromEvent[V](key)
}


// Window / Document events

private[outwatch] abstract class SourceEventPropBuilder(target: dom.EventTarget)
  extends builders.EventPropBuilder[EventSourceStream, dom.Event] {
  override def eventProp[V <: dom.Event](key: String): EventSourceStream[V] = EventSourceStream[V](target, key)
}

abstract class WindowEvents
  extends SourceEventPropBuilder(dom.window)
  with eventProps.WindowEventProps[EventSourceStream]

abstract class DocumentEvents
  extends SourceEventPropBuilder(dom.document)
  with eventProps.DocumentEventProps[EventSourceStream]

// Styles

private[outwatch] trait SimpleStyleBuilder extends builders.StyleBuilders[Style] {
  override protected def buildDoubleStyleSetter(style: keys.Style[Double], value: Double): Style = new BasicStyleBuilder[Double](style.cssName) := value
  override protected def buildIntStyleSetter(style: keys.Style[Int], value: Int): Style = new BasicStyleBuilder[Int](style.cssName) := value
  override protected def buildStringStyleSetter(style: keys.Style[_], value: String): Style = new BasicStyleBuilder[Any](style.cssName) := value
}

trait Styles
  extends styles.Styles[Style]
  with SimpleStyleBuilder

trait StylesExtra
  extends styles.Styles2[Style]
  with SimpleStyleBuilder
