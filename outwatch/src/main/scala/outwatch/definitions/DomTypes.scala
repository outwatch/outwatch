package outwatch.definitions

import com.raquo.domtypes.generic.defs.complex.canonical
import com.raquo.domtypes.generic.defs.{attrs, props, reflectedAttrs, styles}
import com.raquo.domtypes.generic.{builders, codecs, keys}
import com.raquo.domtypes.jsdom.defs.eventProps
import com.raquo.domtypes.jsdom.defs.tags._
import org.scalajs.dom
import outwatch._
import outwatch.helpers._
import colibri.Observable
import scala.scalajs.js

private[outwatch] object BuilderTypes {
  type ReflectedAttribute[T, _] = AttributeBuilder[T, Attr]
  type Attribute[T] = BasicAttrBuilder[T]
  type Property[T, _] = PropBuilder[T]
  type EventEmitter[E <: dom.Event] = EmitterBuilder.Sync[E, RModifier]
  type HtmlTag[T] = HtmlVNode
  type SvgTag[T] = SvgVNode
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

private[outwatch] trait TagBuilder extends builders.HtmlTagBuilder[BuilderTypes.HtmlTag, dom.html.Element] with builders.SvgTagBuilder[BuilderTypes.SvgTag, dom.svg.Element] {
  // we can ignore information about void tags here, because snabbdom handles this automatically for us based on the tagname.
  //TODO: add element type to VTree for typed interface
  @inline protected override def htmlTag[Ref <: dom.html.Element](tagName: String, void: Boolean): HtmlVNode = HtmlVNode(tagName, js.Array[Modifier]())
  @inline protected override def svgTag[Ref <: dom.svg.Element](tagName: String, void: Boolean): SvgVNode = SvgVNode(tagName, js.Array[Modifier]())
}

trait Tags
  extends EmbedTags[BuilderTypes.HtmlTag]
  with GroupingTags[BuilderTypes.HtmlTag]
  with TextTags[BuilderTypes.HtmlTag]
  with FormTags[BuilderTypes.HtmlTag]
  with SectionTags[BuilderTypes.HtmlTag]
  with TableTags[BuilderTypes.HtmlTag]
  with TagBuilder
  with TagHelpers
  with EmbedTagDeprecations[BuilderTypes.HtmlTag]

trait TagsExtra
  extends DocumentTags[BuilderTypes.HtmlTag]
  with MiscTags[BuilderTypes.HtmlTag]
  with TagBuilder
  with DocumentTagDeprecations[BuilderTypes.HtmlTag]

trait TagsSvg
  extends SvgTags[BuilderTypes.SvgTag]
  with TagBuilder

// all Attributes
trait Attributes
  extends HtmlAttrs
  with Events
  with AttributeHelpers
  with OutwatchAttributes
  with HtmlAttributeDeprecations

// Html Attrs
trait HtmlAttrs
  extends attrs.HtmlAttrs[BuilderTypes.Attribute]
  with reflectedAttrs.ReflectedHtmlAttrs[BuilderTypes.ReflectedAttribute]
  with props.Props[BuilderTypes.Property]
  with canonical.CanonicalComplexHtmlKeys[BuilderTypes.ReflectedAttribute, BuilderTypes.Attribute, BuilderTypes.Property]
  with builders.HtmlAttrBuilder[BuilderTypes.Attribute]
  with builders.ReflectedHtmlAttrBuilder[BuilderTypes.ReflectedAttribute]
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
  override lazy val className: AccumAttrBuilder[String] = new AccumAttrBuilder[String](
    "class",
    identity,
    (v1, v2) => s"$v1 $v2"
  )
}

// Svg Attrs
trait SvgAttrs
  extends attrs.SvgAttrs[BuilderTypes.Attribute]
  with builders.SvgAttrBuilder[BuilderTypes.Attribute]
  with SvgAttributeDeprecations {

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
  extends builders.EventPropBuilder[Observable.Synchronous, dom.Event] {
  override def eventProp[V <: dom.Event](key: String): Observable.Synchronous[V] = Observable.ofEvent[V](target, key)
}

abstract class WindowEvents
  extends SourceEventPropBuilder(dom.window)
  with eventProps.WindowEventProps[Observable.Synchronous]

abstract class DocumentEvents
  extends SourceEventPropBuilder(dom.document)
  with eventProps.DocumentEventProps[Observable.Synchronous] {

  def isKeyDown(keyCode: Int): Observable[Boolean] = Observable.merge(
    outwatch.dsl.events.document.onKeyDown.collect { case e if e.keyCode == keyCode => true },
    outwatch.dsl.events.document.onKeyUp.collect { case e if e.keyCode == keyCode => false }
  ).prepend(false)
}

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
