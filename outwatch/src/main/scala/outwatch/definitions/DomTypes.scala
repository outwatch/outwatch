package outwatch.definitions

import com.raquo.domtypes.generic.defs.complex.canonical
import com.raquo.domtypes.generic.defs.{attrs, props, reflectedAttrs, styles}
import com.raquo.domtypes.generic.{builders, codecs, keys}
import com.raquo.domtypes.jsdom.defs.eventProps
import com.raquo.domtypes.jsdom.defs.tags
import org.scalajs.dom
import outwatch._
import outwatch.helpers._
import colibri.Observable
import colibri.jsdom.EventObservable

import scala.scalajs.js

private[outwatch] object BuilderTypes {
  type ReflectedAttribute[T, _] = AttributeBuilder[T, Attr]
  type Attribute[T] = AttributeBuilder[T, Attr]
  type Property[T, _] = PropBuilder[T]
  type EventEmitter[E <: dom.Event] = EmitterBuilder.Sync[E, VModifier]
  type HtmlTag[T] = HtmlVNode
  type SvgTag[T] = SvgVNode
}

private object CodecBuilder {
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
  @inline protected override def htmlTag[Ref <: dom.html.Element](tagName: String, void: Boolean): HtmlVNode = HtmlVNode(tagName, js.Array())
  @inline protected override def svgTag[Ref <: dom.svg.Element](tagName: String, void: Boolean): SvgVNode = SvgVNode(tagName, js.Array())
}

trait Tags
  extends tags.EmbedTags[BuilderTypes.HtmlTag]
  with tags.GroupingTags[BuilderTypes.HtmlTag]
  with tags.TextTags[BuilderTypes.HtmlTag]
  with tags.FormTags[BuilderTypes.HtmlTag]
  with tags.SectionTags[BuilderTypes.HtmlTag]
  with tags.TableTags[BuilderTypes.HtmlTag]
  with TagBuilder
  with TagHelpers
  with EmbedTagDeprecations[BuilderTypes.HtmlTag]

trait TagsExtra
  extends tags.DocumentTags[BuilderTypes.HtmlTag]
  with tags.MiscTags[BuilderTypes.HtmlTag]
  with TagBuilder
  with DocumentTagDeprecations[BuilderTypes.HtmlTag]

trait SvgTags
  extends tags.SvgTags[BuilderTypes.SvgTag]
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

  // super.styleAttr.accum(";") would have been nicer, but we can't do super.styleAttr on a lazy val
  override lazy val styleAttr: AccumAttrBuilder[String] = new AccumAttrBuilder[String](
    "style",
    identity,
    (v1, v2) => s"$v1;$v2"
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

trait WindowEvents
  extends builders.EventPropBuilder[EventObservable, dom.Event]
  with eventProps.WindowEventProps[EventObservable] {

  override def eventProp[V <: dom.Event](key: String): EventObservable[V] = EventObservable[V](dom.window, key)
}

trait DocumentEvents
  extends builders.EventPropBuilder[EventObservable, dom.Event]
  with eventProps.DocumentEventProps[EventObservable] {

  override def eventProp[V <: dom.Event](key: String): EventObservable[V] = EventObservable[V](dom.document, key)

  def isKeyDown(keyCode: Int): Observable[Boolean] = Observable.merge(
    onKeyDown.collect { case e if e.keyCode == keyCode => true },
    onKeyUp.collect { case e if e.keyCode == keyCode => false }
  ).prepend(false)
}

// Styles

private[outwatch] trait SimpleStyleBuilder extends builders.StyleBuilders[Style] {
  override protected def buildDoubleStyleSetter(style: keys.Style[Double], value: Double): Style = BasicStyle(style.name, value.toString)
  override protected def buildIntStyleSetter(style: keys.Style[Int], value: Int): Style = BasicStyle(style.name, value.toString)
  override protected def buildStringStyleSetter(style: keys.Style[_], value: String): Style = BasicStyle(style.name, value.toString)
}

trait Styles
  extends styles.Styles[Style]
  with SimpleStyleBuilder

trait StylesExtra
  extends styles.Styles2[Style]
  with SimpleStyleBuilder
