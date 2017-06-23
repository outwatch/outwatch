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

  private case class Changeables(attributeStreamReceivers: Seq[AttributeStreamReceiver],
                                 childrenStreamReceivers: Seq[ChildrenStreamReceiver],
                                 childStreamReceivers: Seq[ChildStreamReceiver]) {
    lazy val observable: Observable[(Seq[Attribute], Seq[VNode])] = {
      val childReceivers: Observable[Seq[VNode]] = Observable.combineLatest(
        childStreamReceivers.map(_.childStream)
      )

      val childrenReceivers = childrenStreamReceivers.headOption.map(_.childrenStream)

      val attributeReceivers: Observable[Seq[Attribute]] = Observable.combineLatest(
        attributeStreamReceivers.map(_.attributeStream)
      )

      val allChildReceivers = childrenReceivers.getOrElse(childReceivers)

      attributeReceivers.combineLatest(allChildReceivers)
    }

    lazy val nonEmpty = {
      attributeStreamReceivers.nonEmpty || childrenStreamReceivers.nonEmpty || childStreamReceivers.nonEmpty
    }

    lazy val valueStreamExists = attributeStreamReceivers.exists(_.attribute == "value")
  }

  private def createDataObject(changeables: Changeables,
                               properties: Seq[Property],
                               eventHandlers: js.Dictionary[js.Function1[Event, Unit]]): DataObject = {

    if (changeables.nonEmpty){
      createReceiverDataObject(changeables, properties, eventHandlers)
    } else {
      createSimpleDataObject(properties, eventHandlers)
    }
  }

  private def createSimpleDataObject(props: Seq[Property], handlers: js.Dictionary[js.Function1[Event, Unit]]) = {

    val (insert, delete, update, attributes, keys) = separateProperties(props)
    val attrs = VDomProxy.attrsToSnabbDom(attributes)

    val insertHook = (p: VNodeProxy) => p.elm.foreach(e => insert.foreach(_.sink.next(e)))
    val deleteHook = (p: VNodeProxy) => p.elm.foreach(e => delete.foreach(_.sink.next(e)))
    val updateHook = createUpdateHook(update)
    val key = keys.headOption.map(_.value).orUndefined
    DataObject.createWithHooks(attrs, handlers, insertHook, deleteHook, updateHook, key)
  }


  private def createReceiverDataObject(changeables: Changeables,
                                       props: Seq[Property],
                                       eventHandlers: js.Dictionary[js.Function1[Event, Unit]]) = {

    val (insert, destroy, update, attributes, keys) = separateProperties(props)

    val attrs = VDomProxy.attrsToSnabbDom(attributes)
    val subscriptionPromise = Promise[Subscription]
    val insertHook = createInsertHook(changeables, subscriptionPromise, insert)
    val deleteHook = createDestroyHook(subscriptionPromise, destroy)
    val updateHook = createUpdateHook(update)
    val key = keys.headOption.map(_.value).orElse(Some(changeables.hashCode.toString)).orUndefined

    if (changeables.valueStreamExists){
      DataObject.createWithValue(attrs, eventHandlers, insertHook, deleteHook, updateHook, key)
    } else {
      DataObject.createWithHooks(attrs, eventHandlers, insertHook, deleteHook, updateHook, key)
    }
  }

  private def createUpdateHook(hooks: Seq[UpdateHook]) = (old: VNodeProxy, cur: VNodeProxy) => {
    old.elm.foreach(o => cur.elm.foreach(c => hooks.foreach(_.sink.next((o,c)))))
  }


  private def createInsertHook(changables: Changeables,
                               promise: Promise[Subscription],
                               hooks: Seq[InsertHook]) = (proxy: VNodeProxy) => {

    val callback = (e: Element) => hooks.foreach(_.sink.next(e))

    def toProxy(changable: (Seq[Attribute], Seq[VNode])): VNodeProxy = changable match {
      case (attributes, nodes) =>
        val updatedObj = DataObject.updateAttributes(proxy.data, attributes.map(a => (a.title, a.value)))
        h(proxy.sel, updatedObj, proxy.children ++ nodes.map(_.asProxy).toJSArray)
    }

    val subscription = changables.observable
      .map(toProxy)
      .startWith(proxy)
      .pairwise
      .subscribe(tuple => patch(tuple._1, tuple._2), console.error(_))

    promise.success(subscription)

    proxy.elm.foreach(callback)
  }

  private def createDestroyHook(promise: Promise[Subscription], hooks: Seq[DestroyHook]) = (proxy: VNodeProxy) => {
    import scala.concurrent.ExecutionContext.Implicits.global

    val callback = (e: Element) => hooks.foreach(_.sink.next(e))
    proxy.elm.foreach(callback)
    promise.future.foreach(_.unsubscribe())
  }


  def separateModifiers(args: Seq[VDomModifier]): (Seq[Emitter], Seq[Receiver], Seq[Property], Seq[VNode]) = {
    args.foldRight((Seq[Emitter](), Seq[Receiver](), Seq[Property](), Seq[VNode]()))(separatorFn)
  }

  type Result = (Seq[Emitter], Seq[Receiver], Seq[Property], Seq[VNode])

  def separatorFn(mod: VDomModifier, res: Result): Result = (mod, res) match {
    case (em: Emitter, (ems, rcs, prs, vns)) => (em +: ems, rcs, prs, vns)
    case (rc: Receiver, (ems, rcs, prs, vns)) => (ems, rc +: rcs, prs, vns)
    case (pr: Property, (ems, rcs, prs, vns)) => (ems, rcs, pr +: prs,  vns)
    case (vn: VNode, (ems, rcs, prs, vns)) => (ems, rcs, prs, vn +: vns)
  }


  def separateReceivers(receivers: Seq[Receiver]): (Seq[ChildStreamReceiver], Seq[ChildrenStreamReceiver], Seq[AttributeStreamReceiver]) = {
    receivers.foldRight((Seq[ChildStreamReceiver](), Seq[ChildrenStreamReceiver](), Seq[AttributeStreamReceiver]())) {
      case (cr: ChildStreamReceiver, (crs, css, ars)) => (cr +: crs, css, ars)
      case (cs: ChildrenStreamReceiver, (crs, css, ars)) => (crs, cs +: css, ars)
      case (ar: AttributeStreamReceiver, (crs, css, ars)) => ( crs, css, ar +: ars)
    }
  }

  def separateProperties(properties: Seq[Property]): (Seq[InsertHook], Seq[DestroyHook], Seq[UpdateHook], Seq[Attribute], Seq[Key]) = {
    properties.foldRight((Seq[InsertHook](), Seq[DestroyHook](), Seq[UpdateHook](), Seq[Attribute](), Seq[Key]())) {
      case (ih: InsertHook, (ihs, dhs, uhs, ats, keys)) => (ih +: ihs, dhs, uhs, ats, keys)
      case (dh: DestroyHook, (ihs, dhs, uhs, ats, keys)) => (ihs, dh +: dhs, uhs, ats, keys)
      case (uh: UpdateHook, (ihs, dhs, uhs, ats, keys)) => (ihs, dhs, uh +: uhs, ats, keys)
      case (at: Attribute, (ihs, dhs, uhs, ats, keys))  => (ihs, dhs, uhs, at +: ats, keys)
      case (key: Key, (ihs, dhs, uhs, ats, keys)) => (ihs, dhs, uhs, ats, key +: keys)
    }
  }

  def hyperscriptHelper(nodeType: String)(args: VDomModifier*): VNode = {
    val (emitters, receivers, properties, children) = separateModifiers(args)

    val (childReceivers, childrenReceivers, attributeReceivers) = separateReceivers(receivers)

    val changeables = Changeables(attributeReceivers, childrenReceivers, childReceivers)

    val eventHandlers = VDomProxy.emittersToSnabbDom(emitters)

    val dataObject = createDataObject(changeables, properties, eventHandlers)

    VTree(nodeType, children, dataObject)
  }

  def render(element: Element, vNode: VNode): Unit = {
    val elem = document.createElement("app")
    element.appendChild(elem)
    patch(elem,vNode.asProxy)
  }


}
