package outwatch.dom.helpers

import org.scalajs.dom._
import outwatch.dom.VDomModifier.VTree
import outwatch.dom._
import rxscalajs.Observable
import rxscalajs.subscription.AnonymousSubscription
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

    val childReceivers: Observable[Seq[VNode]] = childStreamReceivers
      .map(_.childStream)
      .foldLeft(Observable.of(Seq[VNode]()))((acc, cur) => acc.combineLatestWith(cur)(_ :+ _))

    val childrenReceivers =
      childrenStreamReceivers.map(_.childrenStream)

    val attributeReceivers: Observable[Seq[Attribute]] = attributeStreamReceivers
      .map(_.attributeStream)
      .foldLeft(Observable.of(Seq[Attribute]()))((acc, cur) => acc.combineLatestWith(cur)(_ :+ _))

    val changables = attributeReceivers.combineLatest(childrenReceivers.getOrElse(childReceivers))

    val dataObject: DataObject =
      if (childStreamReceivers.nonEmpty || attributeStreamReceivers.nonEmpty || childrenStreamReceivers.nonEmpty){
        createReceiverDataObject(changables, attributeStreamReceivers, adjustedAttrs, eventHandlers)
      } else {
        DataObject(adjustedAttrs, eventHandlers)
      }

    VTree(nodeType, children, dataObject, changables)
  }

  def createReceiverDataObject(changeables: Observable[(Seq[Attribute], Seq[VNode])],
                               attributeStream: Seq[AttributeStreamReceiver],
                               attrs: js.Dictionary[String],
                               eventHandlers: js.Dictionary[js.Function1[_ <: Event, Unit]]) = {
    val subscriptionPromise = Promise[Option[AnonymousSubscription]]
    val insertHook = createInsertHook(changeables, subscriptionPromise)
    val deleteHook = createDestoryHook(subscriptionPromise)

    if (attributeStream.exists(_.attribute == "value")){
      DataObject.createWithValue(attrs, eventHandlers, insertHook, deleteHook)
    } else {
      DataObject.createWithHooks(attrs, eventHandlers, insertHook, deleteHook)
    }
  }



  def createInsertHook(changables: Observable[(Seq[Attribute], Seq[VNode])],
                       subscriptionPromise: Promise[Option[AnonymousSubscription]]) =
    (proxy: VNodeProxy) => {
      val subscription = changables
        .map(changable => {
          val updatedObj = DataObject.updateAttributes(proxy.data,changable._1.map(a => (a.title, a.value)))
          h(proxy.sel, updatedObj, (proxy.children ++ changable._2.map(_.asProxy)).toJSArray)
        })
        .startWith(proxy)
        .pairwise
        .subscribe(tuple => patch(tuple._1, tuple._2), e => println(e))

      subscriptionPromise.success(Some(subscription))
      ()
  }

  def createDestoryHook(subscriptionPromise: Promise[Option[AnonymousSubscription]]) = (proxy: VNodeProxy) => {
    import scala.concurrent.ExecutionContext.Implicits.global

    subscriptionPromise.future.foreach(_.foreach(_.unsubscribe()))
  }


  def seperateModifiers(args: VDomModifier*) = {
      args.foldRight((Seq[Emitter](), Seq[ChildStreamReceiver](),  Seq[ChildrenStreamReceiver](),
          Seq[Attribute](), Seq[AttributeStreamReceiver](), Seq[VNode]())) {
        case (em: Emitter, (ems, crs, csr, ats, ars, vns)) => (em +: ems, crs, csr, ats, ars, vns)
        case (cr: ChildStreamReceiver, (ems, crs, csr, ats, ars, vns)) => (ems, cr +: crs, csr, ats, ars, vns)
        case (cs: ChildrenStreamReceiver, (ems, crs, csr, ats, ars, vns)) => (ems, crs, cs +: csr, ats, ars, vns)
        case (at: Attribute, (ems, crs, csr, ats, ars, vns)) => (ems, crs, csr, at +: ats, ars, vns)
        case (ar: AttributeStreamReceiver, (ems, crs, csr, ats, ars, vns)) => (ems, crs, csr, ats, ar +: ars, vns)
        case (vn: VNode, (ems, crs, csr, ats, ars, vns)) => (ems, crs, csr, ats, ars, vn +: vns)
      }
  }


  def hyperscriptHelper(nodeType: String)(args: VDomModifier*): VNode = {
    val (emitters, childReceivers, childrenReceivers, attributes, attributeReceivers, children) =
      seperateModifiers(args: _*)

    constructVNode(nodeType,emitters,childReceivers, childrenReceivers.headOption, attributes,attributeReceivers,children)
  }

  def render(element: Element, vNode: VNode): Unit = {
    patch(element,vNode.asProxy)
  }


}
