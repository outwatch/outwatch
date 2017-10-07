package outwatch.dom.helpers

import cats.effect.IO
import org.scalajs.dom._
import org.scalajs.dom.raw.HTMLInputElement
import outwatch.dom._
import rxscalajs.Observable
import rxscalajs.subscription.Subscription
import snabbdom._
import collection.breakOut

import scala.concurrent.{Future, Promise}
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

      val childrenReceivers = childrenStreamReceivers.lastOption.map(_.childrenStream)


      val attributeReceivers: Observable[Seq[Attribute]] = Observable.combineLatest(
        // only use last encountered observable per attribute
        attributeStreamReceivers
          .groupBy(_.attribute)
          .values
          .map(_.last.attributeStream)(breakOut)
      )

      val allChildReceivers = childrenReceivers.getOrElse(childReceivers)

      attributeReceivers.combineLatest(allChildReceivers)
    }

    lazy val nonEmpty: Boolean = {
      attributeStreamReceivers.nonEmpty || childrenStreamReceivers.nonEmpty || childStreamReceivers.nonEmpty
    }

    lazy val valueStreamExists: Boolean = attributeStreamReceivers.exists(_.attribute == "value")
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

  private def createSimpleDataObject(properties: Seq[Property], handlers: js.Dictionary[js.Function1[Event, Unit]]) = {

    val SeparatedProperties(insert, delete, update, attributes, keys) = separateProperties(properties)
    val (attrs, props, style) = VDomProxy.attrsToSnabbDom(attributes)

    val insertHook = (p: VNodeProxy) => p.elm.foreach(e => insert.foreach(_.sink.next(e)))
    val deleteHook = (p: VNodeProxy) => p.elm.foreach(e => delete.foreach(_.sink.next(e)))
    val updateHook = createUpdateHook(update)
    val key = keys.lastOption.map(_.value).orUndefined

    DataObject.create(attrs, props, style, handlers, insertHook, deleteHook, updateHook, key)
  }

  private def seq[A, B](f1: (A, B) => Unit,f2: (A, B) => Unit): (A, B) => Unit = (a: A, b: B) => {
    f1(a, b)
    f2(a, b)
  }

  private val valueSyncHook: (VNodeProxy, VNodeProxy) => Unit = (_, node) => {
    node.elm.foreach { elm =>
      val input = elm.asInstanceOf[HTMLInputElement]
      if (input.value != input.getAttribute("value")) {
        input.value = input.getAttribute("value")
      }
    }
  }

  private def createReceiverDataObject(changeables: Changeables,
                                       properties: Seq[Property],
                                       eventHandlers: js.Dictionary[js.Function1[Event, Unit]]) = {

    val SeparatedProperties(insert, destroy, update, attributes, keys) = separateProperties(properties)

    val (attrs, props, style) = VDomProxy.attrsToSnabbDom(attributes)
    val subscriptionRef = STRef.empty[Subscription]
    val insertHook = createInsertHook(changeables, subscriptionRef, insert)
    val deleteHook = createDestroyHook(subscriptionRef, destroy)
    val updateHook = createUpdateHook(update)
    val key = keys.lastOption.map(_.value).getOrElse(changeables.hashCode.toString)

    val updateHookHelper = if (changeables.valueStreamExists) {
      seq(updateHook, valueSyncHook)
    } else {
      updateHook
    }

    DataObject.create(attrs, props, style, eventHandlers, insertHook, deleteHook, updateHookHelper, key)
  }

  private def createUpdateHook(hooks: Seq[UpdateHook]) = (old: VNodeProxy, cur: VNodeProxy) => {
    old.elm.foreach(o => cur.elm.foreach(c => hooks.foreach(_.sink.next((o,c)))))
  }


  private def createInsertHook(changables: Changeables,
                               subscriptionRef: STRef[Subscription],
                               hooks: Seq[InsertHook]) = (proxy: VNodeProxy) => {

    def toProxy(changable: (Seq[Attribute], Seq[VNode])): VNodeProxy = changable match {
      case (attributes, nodes) =>
        val updatedObj = proxy.data.withUpdatedAttributes(attributes)
        h(proxy.sel, updatedObj, proxy.children ++ (nodes.map(_.asProxy.unsafeRunSync())(breakOut):js.Array[VNodeProxy])) //TODO why unsafe?
    }

    val subscription = changables.observable
      .map(toProxy)
      .startWith(proxy)
      .pairwise
      .subscribe(tuple => patch(tuple._1, tuple._2), console.error(_))

    subscriptionRef.put(subscription).unsafeRunSync()

    proxy.elm.foreach((e: Element) => hooks.foreach(_.sink.next(e)))
  }

  private def createDestroyHook(subscription: STRef[Subscription], hooks: Seq[DestroyHook]) = (proxy: VNodeProxy) => {
    proxy.elm.foreach((e: Element) => hooks.foreach(_.sink.next(e)))
    subscription.update { s => s.unsubscribe(); s }.unsafeRunSync()
    ()
  }


  private[outwatch] final case class SeparatedModifiers(
    emitters: List[Emitter] = Nil,
    receivers: List[Receiver] = Nil,
    properties: List[Property] = Nil,
    vNodes: List[VNode] = Nil
  )
  private[outwatch] def separateModifiers(args: Seq[VDomModifier]): SeparatedModifiers = {
    args.foldRight(SeparatedModifiers())(separatorFn)
  }

  private[outwatch] def separatorFn(mod: VDomModifier, res: SeparatedModifiers): SeparatedModifiers = (mod, res) match {
    case (em: Emitter, sf) => sf.copy(emitters = em :: sf.emitters)
    case (rc: Receiver, sf) => sf.copy(receivers = rc :: sf.receivers)
    case (pr: Property, sf) => sf.copy(properties = pr :: sf.properties)
    case (vn: VNode, sf) => sf.copy(vNodes = vn :: sf.vNodes)
    case (EmptyVDomModifier, sf) => sf
  }


  private[outwatch] final case class SeparatedReceivers(
    childStream: List[ChildStreamReceiver] = Nil,
    childrenStream: List[ChildrenStreamReceiver] = Nil,
    attributeStream: List[AttributeStreamReceiver] = Nil
  )
  private[outwatch] def separateReceivers(receivers: Seq[Receiver]): SeparatedReceivers = {
    receivers.foldRight(SeparatedReceivers()) {
      case (cr: ChildStreamReceiver, sr) => sr.copy(childStream = cr :: sr.childStream)
      case (cs: ChildrenStreamReceiver, sr) => sr.copy(childrenStream = cs :: sr.childrenStream)
      case (ar: AttributeStreamReceiver, sr) => sr.copy(attributeStream = ar :: sr.attributeStream)
    }
  }

  private[outwatch] final case class SeparatedProperties(
    insertHooks: List[InsertHook] = Nil,
    destroyHooks: List[DestroyHook] = Nil,
    updateHooks: List[UpdateHook] = Nil,
    attributeHooks: List[Attribute] = Nil,
    keys: List[Key] = Nil
  )
  private[outwatch] def separateProperties(properties: Seq[Property]): SeparatedProperties = {
    properties.foldRight(SeparatedProperties()) {
      case (ih: InsertHook, sp) => sp.copy(insertHooks = ih :: sp.insertHooks)
      case (dh: DestroyHook, sp) => sp.copy(destroyHooks = dh :: sp.destroyHooks)
      case (uh: UpdateHook, sp) => sp.copy(updateHooks = uh :: sp.updateHooks)
      case (at: Attribute, sp)  => sp.copy(attributeHooks = at :: sp.attributeHooks)
      case (key: Key, sp) => sp.copy(keys = key :: sp.keys)
    }
  }

  private[outwatch] def extractChildrenAndDataObject(args: Seq[VDomModifier]): (List[VNode], DataObject) = {
    val SeparatedModifiers(emitters, receivers, properties, children) = separateModifiers(args)

    val SeparatedReceivers(childReceivers, childrenReceivers, attributeReceivers) = separateReceivers(receivers)

    val changeables = Changeables(attributeReceivers, childrenReceivers, childReceivers)

    val eventHandlers = VDomProxy.emittersToSnabbDom(emitters)

    val dataObject = createDataObject(changeables, properties, eventHandlers)

    (children, dataObject)
  }

  def render(element: Element, vNode: VNode): IO[Unit] = for {
    elem <- IO(document.createElement("app"))
    _ <- IO(element.appendChild(elem))
    node <- vNode.asProxy
    _ <- IO(patch(elem, node))
  } yield ()
}
