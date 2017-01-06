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
                     properties: Seq[Property],
                     attributeStreamReceivers: Seq[AttributeStreamReceiver],
                     children: Seq[VNode]): VNode = {

    val eventHandlers = VDomProxy.emittersToSnabbDom(eventEmitters)


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
        createReceiverDataObject(changables, attributeStreamReceivers, properties, eventHandlers)
      } else {
        createDataObject(properties, eventHandlers)
      }

    VTree(nodeType, children, dataObject, changables)
  }

  private def createReceiverDataObject(changeables: Observable[(Seq[Attribute], Seq[VNode])],
                               attributeStream: Seq[AttributeStreamReceiver], props: Seq[Property],
                               eventHandlers: js.Dictionary[js.Function1[Event, Unit]]) = {

    val (insert, delete, update, attributes) = separateProperties(props)

    val attrs = VDomProxy.attrsToSnabbDom(attributes)
    val subscriptionPromise = Promise[Option[Subscription]]
    val insertHook = createInsertHook(changeables, subscriptionPromise, insert.foreach(_.sink.next(())))
    val deleteHook = createDestoryHook(subscriptionPromise, delete.foreach(_.sink.next(())))
    val updateHook = () => update.foreach(_.sink.next(()))

    if (attributeStream.exists(_.attribute == "value")){
      DataObject.createWithValue(attrs, eventHandlers, insertHook, deleteHook, updateHook)
    } else {
      DataObject.createWithHooks(attrs, eventHandlers, insertHook, deleteHook, updateHook)
    }
  }

  private def createDataObject(props: Seq[Property], handlers: js.Dictionary[js.Function1[Event, Unit]]) = {

    val (insert, delete, update, attributes) = separateProperties(props)
    val attrs = VDomProxy.attrsToSnabbDom(attributes)

    val insertHook = (p: VNodeProxy) => insert.foreach(_.sink.next(()))
    val deleteHook = (p: VNodeProxy) => delete.foreach(_.sink.next(()))
    val updateHook = () => update.foreach(_.sink.next(()))
    DataObject.createWithHooks(attrs, handlers, insertHook, deleteHook, updateHook)
  }



  private def createInsertHook(changables: Observable[(Seq[Attribute], Seq[VNode])],
                               promise: Promise[Option[Subscription]],
                               callback: => Unit) = (proxy: VNodeProxy) => {

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

    callback

    promise.success(Some(subscription))
    ()
  }

  private def createDestoryHook(promise: Promise[Option[Subscription]], callback: => Unit) = (proxy: VNodeProxy) => {
    import scala.concurrent.ExecutionContext.Implicits.global

    callback
    promise.future.foreach(_.foreach(_.unsubscribe()))
  }


  def separateModifiers(args: VDomModifier*) = {
    args.foldRight((Seq[Emitter](), Seq[Receiver](), Seq[Property](), Seq[VNode]())) {
      case (em: Emitter, (ems, rcs, prs, vns)) => (em +: ems, rcs, prs, vns)
      case (rc: Receiver, (ems, rcs, prs, vns)) => (ems, rc +: rcs, prs, vns)
      case (pr: Property, (ems, rcs, prs, vns)) => (ems, rcs, pr +: prs,  vns)
      case (vn: VNode, (ems, rcs, prs, vns)) => (ems, rcs, prs, vn +: vns)
    }
  }


  def separateReceivers(receivers: Seq[Receiver]) = {
    receivers.foldRight((Seq[ChildStreamReceiver](), Seq[ChildrenStreamReceiver](), Seq[AttributeStreamReceiver]())) {
      case (cr: ChildStreamReceiver, (crs, css, ars)) => (cr +: crs, css, ars)
      case (cs: ChildrenStreamReceiver, (crs, css, ars)) => (crs, cs +: css, ars)
      case (ar: AttributeStreamReceiver, (crs, css, ars)) => ( crs, css, ar +: ars)
    }
  }

  def separateProperties(properties: Seq[Property]) = {
    properties.foldRight((Seq[InsertHook](), Seq[DestroyHook](), Seq[UpdateHook](), Seq[Attribute]())) {
      case (ih: InsertHook, (ihs, dhs, uhs, ats)) => (ih +: ihs, dhs, uhs, ats)
      case (dh: DestroyHook, (ihs, dhs, uhs, ats)) => (ihs, dh +: dhs, uhs, ats)
      case (uh: UpdateHook, (ihs, dhs, uhs, ats)) => (ihs, dhs, uh +: uhs, ats)
      case (at: Attribute, (ihs, dhs, uhs, ats))  => (ihs, dhs, uhs, at +: ats)
    }
  }

  def hyperscriptHelper(nodeType: String)(args: VDomModifier*): VNode = {
    val (emitters, receivers, properties, children) = separateModifiers(args: _*)

    val (childReceivers, childrenReceivers, attributeReceivers) = separateReceivers(receivers)

    constructVNode(nodeType,
      emitters,
      childReceivers,
      childrenReceivers.headOption,
      properties,
      attributeReceivers,
      children
    )
  }

  def render(element: Element, vNode: VNode): Unit = {
    patch(element,vNode.asProxy)
  }


}
