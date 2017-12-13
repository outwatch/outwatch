package outwatch.dom

import com.raquo.domtypes.generic.builders._
import com.raquo.domtypes.generic.keys
import com.raquo.domtypes.generic.codecs._
import com.raquo.domtypes.generic.defs.attrs
import com.raquo.domtypes.generic.defs.reflectedAttrs
import com.raquo.domtypes.generic.defs.props
import com.raquo.domtypes.generic.defs.styles
import com.raquo.domtypes.jsdom.defs.tags._
import com.raquo.domtypes.jsdom.defs.eventProps._
import cats.effect.IO
import org.scalajs.dom
import helpers._

private[outwatch] object DomTypesBuilder {
  type VTree[Elem <: dom.Element] = IO[VTree_[Elem]]

  trait VNodeBuilder extends TagBuilder[VTree, dom.Element] {
    // we can ignore information about void tags here, because snabbdom handles this automatically for us based on the tagname.
    protected override def tag[Elem <: dom.Element](tagName: String, void: Boolean): VTree[Elem] = IO.pure(VTree_[Elem](tagName, Seq.empty))
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
    override def eventProp[V <: dom.Event](key: String): Observable[V] = Observable.create { obs =>
      target.addEventListener(key, obs.next _)
    }
  }

  trait SimpleStyleBuilder extends StyleBuilders[IO[Style]] {

    implicit def StyleIsBuilder[T](style: keys.Style[T]): StyleBuilder[T] = new StyleBuilder[T](style.cssName)

    override protected def buildDoubleStyleSetter(style: keys.Style[Double], value: Double): IO[Style] = style := value
    override protected def buildIntStyleSetter(style: keys.Style[Int],value: Int): IO[Style] = style := value
    override protected def buildStringStyleSetter(style: keys.Style[_],value: String): IO[Style] = new StyleBuilder[Any](style.cssName) := value
  }
}
import DomTypesBuilder._

class TagContext[Elem <: dom.Element]
  extends HTMLElementEventProps[({ type T[E <: dom.Event] = SimpleEmitterBuilder[E with TypedCurrentTargetEvent[Elem]] })#T]
  with EventPropBuilder[({ type T[E <: dom.Event] = SimpleEmitterBuilder[E with TypedCurrentTargetEvent[Elem]] })#T, dom.Event] {

  override def eventProp[V <: dom.Event](key: String) =  EmitterBuilder[V with TypedCurrentTargetEvent[Elem]](key)
}

trait Tags
  extends EmbedTags[VTree]
  with GroupingTags[VTree]
  with TextTags[VTree]
  with FormTags[VTree]
  with SectionTags[VTree]
  with TableTags[VTree]
  with TagsCompat
  with VNodeBuilder
  with TagHelpers
object Tags extends Tags

trait TagsExtra
  extends DocumentTags[VTree]
  with MiscTags[VTree]
  with VNodeBuilder
object TagsExtra extends TagsExtra

trait Attributes
  extends Attrs
  with ReflectedAttrs
  with Props
  with Events
  with Styles
  with AttributesCompat
  with OutwatchAttributes
object Attributes extends Attributes

trait Attrs
  extends attrs.Attrs[AttributeBuilder]
  with AttrBuilder[AttributeBuilder] {

  override protected def attr[V](key: String, codec: Codec[V, String]): AttributeBuilder[V] =
    new AttributeBuilder(key, CodecBuilder.encodeAttribute(codec))
}
object Attrs extends Attrs

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
object ReflectedAttrs extends ReflectedAttrs

trait Props
  extends props.Props[CodecBuilder.Property]
  with PropBuilder[CodecBuilder.Property] {

  override protected def prop[V, DomV](key: String, codec: Codec[V, DomV]): PropertyBuilder[V] =
    new PropertyBuilder(key, codec.encode)
}
object Props extends Props

trait Events
  extends HTMLElementEventProps[SimpleEmitterBuilder]
  with EventPropBuilder[SimpleEmitterBuilder, dom.Event] {

  override def eventProp[V <: dom.Event](key: String): SimpleEmitterBuilder[V] =  EmitterBuilder[V](key)
}
object Events extends Events

object WindowEvents
  extends ObservableEventPropBuilder(dom.window)
  with WindowEventProps[Observable]

object DocumentEvents
  extends ObservableEventPropBuilder(dom.document)
  with DocumentEventProps[Observable]

trait Styles
  extends styles.Styles[IO[Style]]
  with SimpleStyleBuilder
object Styles extends Styles

trait StylesExtra
  extends styles.Styles2[IO[Style]]
  with SimpleStyleBuilder
object StylesExtra extends StylesExtra
