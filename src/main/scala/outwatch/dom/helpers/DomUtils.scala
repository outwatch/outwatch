package outwatch.dom.helpers

import org.scalajs.dom._
import outwatch.dom.VDomModifier.VTree
import outwatch.dom._
import rxscalajs.Observable
import rxscalajs.subscription.Subscription
import snabbdom._

import scala.concurrent.Promise
import scala.scalajs.js
import scala.scalajs.js.JSConverters._


object DomUtils {

  def constructVNode(nodeType: String,
                     eventEmitters: Seq[Emitter],
                     childStreamReceivers: Seq[ChildStreamReceiver],
                     childrenStreamReceivers: Option[ChildrenStreamReceiver],
                     attributes: Seq[Attribute],
                     attributeStreamReceivers: Seq[AttributeStreamReceiver],
                     children: Seq[VNode]): VNode = {

    val eventHandlers = VDomProxy.emittersToSnabbDom(eventEmitters)

    val adjustedAttrs =  VDomProxy.attrsToSnabbDom(attributes)


    val childReceivers: Observable[Seq[VNode]] = Observable.combineLatest(
      childStreamReceivers.map(_.childStream)
    )

    val childrenReceivers =
      childrenStreamReceivers.map(_.childrenStream)

    val attributeReceivers: Observable[Seq[Attribute]] = Observable.combineLatest(
      attributeStreamReceivers.map(_.attributeStream)
    )

    val changables = attributeReceivers.combineLatest(childrenReceivers.getOrElse(childReceivers))

    val dataObject: DataObject =
      if (childStreamReceivers.nonEmpty || attributeStreamReceivers.nonEmpty || childrenStreamReceivers.nonEmpty){
        createReceiverDataObject(changables, attributeStreamReceivers, adjustedAttrs, eventHandlers)
      } else {
        DataObject(adjustedAttrs, eventHandlers)
      }

    VTree(nodeType, children, dataObject, changables)
  }

  private def createReceiverDataObject(changeables: Observable[(Seq[Attribute], Seq[VNode])],
                               attributeStream: Seq[AttributeStreamReceiver],
                               attrs: js.Dictionary[String],
                               eventHandlers: js.Dictionary[js.Function1[Event, Unit]]) = {

    val subscriptionPromise = Promise[Option[Subscription]]
    val insertHook = createInsertHook(changeables, subscriptionPromise)
    val deleteHook = createDestoryHook(subscriptionPromise)

    if (attributeStream.exists(_.attribute == "value")){
      DataObject.createWithValue(attrs, eventHandlers, insertHook, deleteHook)
    } else {
      DataObject.createWithHooks(attrs, eventHandlers, insertHook, deleteHook)
    }
  }

  private def createDataObject(attrs: js.Dictionary[String], handlers: js.Dictionary[js.Function1[Event, Unit]]) = {
    DataObject(attrs, handlers)
  }



  private def createInsertHook(changables: Observable[(Seq[Attribute], Seq[VNode])],
                               promise: Promise[Option[Subscription]]) = (proxy: VNodeProxy) => {

    def toProxy(changable: (Seq[Attribute], Seq[VNode])): VNodeProxy = changable match {
      case (attributes, nodes) =>
        val updatedObj = DataObject.updateAttributes(proxy.data, attributes.map(a => (a.title, a.value)))
        h(proxy.sel, updatedObj, (proxy.children ++ nodes.map(_.asProxy)).toJSArray)
    }

    val subscription = changables
      .map(toProxy)
      .startWith(proxy)
      .pairwise
      .subscribe(tuple => patch(tuple._1, tuple._2), console.error(_))

    promise.success(Some(subscription))
    ()
  }

  private def createDestoryHook(subscriptionPromise: Promise[Option[AnonymousSubscription]]) = (proxy: VNodeProxy) => {
    import scala.concurrent.ExecutionContext.Implicits.global

    subscriptionPromise.future.foreach(_.foreach(_.unsubscribe()))
  }


  def seperateModifiers(args: VDomModifier*) = {
      args.foldRight((Seq[Emitter](), Seq[Receiver](), Seq[Attribute](), Seq[VNode]())) {
        case (em: Emitter, (ems, rcs, ats, vns)) => (em +: ems, rcs, ats, vns)
        case (rc: Receiver, (ems, rcs, ats, vns)) => (ems, rc +: rcs, ats, vns)
        case (at: Attribute, (ems, rcs, ats, vns)) => (ems, rcs, at +: ats,  vns)
        case (vn: VNode, (ems, rcs, ats, vns)) => (ems, rcs, ats, vn +: vns)
      }
  }

  def seperateReceivers(receivers: Seq[Receiver]) = {
    receivers.foldRight((Seq[ChildStreamReceiver](), Seq[ChildrenStreamReceiver](), Seq[AttributeStreamReceiver]())) {
      case (cr: ChildStreamReceiver, (crs, css, ars)) => (cr +: crs, css, ars)
      case (cs: ChildrenStreamReceiver, (crs, css, ars)) => (crs, cs +: css, ars)
      case (ar: AttributeStreamReceiver, (crs, css, ars)) => ( crs, css, ar +: ars)
    }
  }

  def hyperscriptHelper(nodeType: String)(args: VDomModifier*): VNode = {
    val (emitters, receivers, attributes, children) = seperateModifiers(args: _*)

    val (childReceivers, childrenReceivers, attributeReceivers) = seperateReceivers(receivers)

    constructVNode(nodeType,emitters,childReceivers, childrenReceivers.headOption, attributes,attributeReceivers,children)
  }

  def render(element: Element, vNode: VNode): Unit = {
    patch(element,vNode.asProxy)
  }


}
