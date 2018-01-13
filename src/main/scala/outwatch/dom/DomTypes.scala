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
import org.scalajs.dom
import helpers._
import monix.execution.{Ack, Cancelable}
import monix.reactive.OverflowStrategy.Unbounded
import cats.effect.Effect

import scala.scalajs.js


trait Types[F[_]] {
  implicit val e:Effect[F]

  type VNode = F[VTree]
  type GenericVNode[_] = VNode
}

// abstract class Types[F[_]:Effect] {
//   type VNode = F[VTree]
//   type GenericVNode[_] = VNode
// }

private[outwatch] object DomTypesBuilder {
  trait VNodeBuilder[F[_]] extends TagBuilder[Types[F]#GenericVNode, Types[F]#VNode] {
    implicit val e:Effect[F]
    // we can ignore information about void tags here, because snabbdom handles this automatically for us based on the tagname.
    protected override def tag[Ref <: Types[F]#VNode](tagName: String, void: Boolean): Types[F]#VNode = Effect[F].pure(VTree(tagName, Seq.empty))
  }

  object CodecBuilder {
    type Attribute[T, _] = ValueBuilder[T, Attr]
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

trait Tags[F[_]]
  extends EmbedTags[Types[F]#GenericVNode, Types[F]#VNode]
  with GroupingTags[Types[F]#GenericVNode, Types[F]#VNode]
  with TextTags[Types[F]#GenericVNode, Types[F]#VNode]
  with FormTags[Types[F]#GenericVNode, Types[F]#VNode]
  with SectionTags[Types[F]#GenericVNode, Types[F]#VNode]
  with TableTags[Types[F]#GenericVNode, Types[F]#VNode]
  with VNodeBuilder[F]
  with TagHelpers[F]
  with TagsCompat[F]

// @deprecated("Use dsl.tags instead", "0.11.0")
// object Tags extends Tags

trait TagsExtra[F[_]]
  extends DocumentTags[Types[F]#GenericVNode, Types[F]#VNode]
  with MiscTags[Types[F]#GenericVNode, Types[F]#VNode]
  with VNodeBuilder[F]

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

  // super.className.accum(" ") would have been nicer, but we can't do super.className on a lazy val
  override lazy val className = new AccumAttributeBuilder[String]("class",
    stringReflectedAttr(attrKey = "class", propKey = "className"),
    _ + " " + _
  )

  override protected def reflectedAttr[V, DomPropV](
    attrKey: String,
    propKey: String,
    attrCodec: Codec[V, String],
    propCodec: Codec[V, DomPropV]
  ) = new AttributeBuilder(attrKey, CodecBuilder.encodeAttribute(attrCodec))
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


private[outwatch] trait SimpleStyleBuilder[F[+_]] extends StyleBuilders[F[Style]] {
  implicit val e:Effect[F]
  override protected def buildDoubleStyleSetter(style: keys.Style[Double], value: Double): F[Style] = style.:=[F](value)
  override protected def buildIntStyleSetter(style: keys.Style[Int],value: Int): F[Style] = style.:=[F](value)
  override protected def buildStringStyleSetter(style: keys.Style[_],value: String): F[Style] = new BasicStyleBuilder[Any](style.cssName).:=[F](value)
}


trait Styles[F[_]]
  extends styles.Styles[F[Style]]
  with SimpleStyleBuilder[F]

trait StylesExtra[F[_]]
  extends styles.Styles2[F[Style]]
  with SimpleStyleBuilder[F]
