package outwatch.dom.helpers

import cats.effect.IO
import org.scalajs.dom._
import outwatch.dom._
import rxscalajs.Observable
import rxscalajs.subjects.BehaviorSubject
import rxscalajs.subscription.Subscription
import snabbdom._

import collection.breakOut
import scala.scalajs.js
import scala.scalajs.js.JSConverters._


object DomUtils {

  private def createDataObject(changeables: SeparatedReceivers,
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

    val insertHook = (p: VNodeProxy) => p.elm.foreach(e => insert.foreach(_.observer.next(e)))
    val deleteHook = (p: VNodeProxy) => p.elm.foreach(e => delete.foreach(_.observer.next(e)))
    val updateHook = createUpdateHook(update)
    val key = keys.lastOption.map(_.value).orUndefined

    DataObject.create(attrs, props, style, handlers, insertHook, deleteHook, updateHook, key)
  }

  private def createReceiverDataObject(changeables: SeparatedReceivers,
                                       properties: Seq[Property],
                                       eventHandlers: js.Dictionary[js.Function1[Event, Unit]]) = {

    val SeparatedProperties(insert, destroy, update, attributes, keys) = separateProperties(properties)

    val (attrs, props, style) = VDomProxy.attrsToSnabbDom(attributes)
    val subscriptionRef = STRef.empty[Subscription]
    val insertHook = createInsertHook(changeables, subscriptionRef, insert)
    val deleteHook = createDestroyHook(subscriptionRef, destroy)
    val updateHook = createUpdateHook(update)
    val key = keys.lastOption.map(_.value).getOrElse(changeables.hashCode.toString)

    DataObject.create(attrs, props, style, eventHandlers, insertHook, deleteHook, updateHook, key)
  }

  private def createUpdateHook(hooks: Seq[UpdateHook]) = (old: VNodeProxy, cur: VNodeProxy) => {
    for {
      o <- old.elm
      c <- cur.elm
    } {
      hooks.foreach(_.observer.next((o,c)))
    }
  }


  private def createInsertHook(changables: SeparatedReceivers,
                               subscriptionRef: STRef[Subscription],
                               hooks: Seq[InsertHook]) = (proxy: VNodeProxy) => {

    def toProxy(changable: (Seq[Attribute], Seq[VNode])): VNodeProxy = {
      val (attributes, nodes) = changable
      h(
        proxy.sel,
        proxy.data.withUpdatedAttributes(attributes),
        if (nodes.isEmpty) proxy.children else nodes.map(_.unsafeRunSync().asProxy)(breakOut): js.Array[VNodeProxy]
      )
    }

    val subscription = changables.observable
      .map(toProxy)
      .startWith(proxy)
      .pairwise
      .subscribe({ case (prev, crt) => patch(prev, crt) }, console.error(_))

    subscriptionRef.put(subscription).unsafeRunSync()

    proxy.elm.foreach((e: Element) => hooks.foreach(_.observer.next(e)))
  }

  private def createDestroyHook(subscription: STRef[Subscription], hooks: Seq[DestroyHook]) = (proxy: VNodeProxy) => {
    proxy.elm.foreach((e: Element) => hooks.foreach(_.observer.next(e)))
    subscription.update { s => s.unsubscribe(); s }.unsafeRunSync()
    ()
  }


  private[outwatch] final case class SeparatedModifiers(
    emitters: List[Emitter] = Nil,
    attributeReceivers: List[AttributeStreamReceiver] = Nil,
    properties: List[Property] = Nil,
    vNodes: List[ChildVNode] = Nil
  )
  private[outwatch] def separateModifiers(args: Seq[VDomModifier_]): SeparatedModifiers = {
    args.foldRight(SeparatedModifiers())(separatorFn)
  }

  private[outwatch] def separatorFn(mod: VDomModifier_, res: SeparatedModifiers): SeparatedModifiers = (mod, res) match {
    case (em: Emitter, sf) => sf.copy(emitters = em :: sf.emitters)
    case (rc: AttributeStreamReceiver, sf) => sf.copy(attributeReceivers = rc :: sf.attributeReceivers)
    case (pr: Property, sf) => sf.copy(properties = pr :: sf.properties)
    case (vn: ChildVNode, sf) => sf.copy(vNodes = vn :: sf.vNodes)
    case (vn: CompositeVDomModifier, sf) =>
      val modifiers = vn.modifiers.map(_.unsafeRunSync())
      val sm = separateModifiers(modifiers)
      sf.copy(
        emitters = sm.emitters ++ sf.emitters,
        attributeReceivers = sm.attributeReceivers ++ sf.attributeReceivers,
        properties = sm.properties ++ sf.properties,
        vNodes = sm.vNodes ++ sf.vNodes
      )
    case (EmptyVDomModifier, sf) => sf
  }

  private[outwatch] final case class SeparatedReceivers(
    childNodes: List[ChildVNode] = Nil,
    hasNodeStreams: Boolean = false,
    attributeStreamReceivers: List[AttributeStreamReceiver] = Nil
  ) {

    lazy val observable: Observable[(Seq[Attribute], Seq[VNode])] = {
      val childStreamReceivers = if (hasNodeStreams) {
        childNodes.foldRight(Observable.of(List.empty[VNode])) {
          case (vn: VNode_, obs) => obs.combineLatestWith(BehaviorSubject(IO.pure(vn)))((nodes, n) => n :: nodes)
          case (csr: ChildStreamReceiver, obs) => obs.combineLatestWith(csr.childStream)((nodes, n) => n :: nodes)
          case (csr: ChildrenStreamReceiver, obs) =>
            obs.combineLatestWith(csr.childrenStream.startWith(Seq.empty))((nodes, n) => n.toList ++ nodes)
        }
      } else {
        Observable.of(Seq.empty)
      }

      // only use last encountered observable per attribute
      val attributeReceivers: Observable[Seq[Attribute]] = Observable.combineLatest(
        attributeStreamReceivers
          .groupBy(_.attribute)
          .values
          .map(_.last.attributeStream)(breakOut)
      )

      attributeReceivers.combineLatest(childStreamReceivers)
    }

    lazy val nonEmpty: Boolean = {
      attributeStreamReceivers.nonEmpty || hasNodeStreams
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


  private final case class ChildrenNodes(
    children: List[VNode_] = Nil,
    hasStreams: Boolean = false
  )
  private def extractChildren(nodes: Seq[ChildVNode]): ChildrenNodes = nodes.foldRight(ChildrenNodes()) {
    case (vn: VNode_, cn) => cn.copy(children = vn :: cn.children)
    case (_: ChildStreamReceiver, cn) => cn.copy(hasStreams = true)
    case (_: ChildrenStreamReceiver, cn) => cn.copy(hasStreams = true)
  }

  private[outwatch] def ensureVNodeKey(vn: VNode_): VNode_ = {
    val withKey: VNode_ = vn match {
      case vtree: VTree =>
        val modifiers = vtree.modifiers
        val hasKey = modifiers.exists(m => m.unsafeRunSync().isInstanceOf[Key])
        val newModifiers = if (hasKey) modifiers else IO.pure(Key(vtree.hashCode.toString)) +: modifiers
        vtree.copy(modifiers = newModifiers)
      case sn: StringNode => sn
    }
    withKey
  }

  private[outwatch] def extractChildrenAndDataObject(args: Seq[VDomModifier_]): (Seq[VNode_], DataObject) = {
    val SeparatedModifiers(emitters, attributeReceivers, properties, nodes) = separateModifiers(args)

    val ChildrenNodes(children, hasChildStreams) = extractChildren(nodes)

    // if child streams exists, we want the static children in the same node have keys
    // for efficient patching when the streams change
    val childrenWithKey = if (hasChildStreams) children.map(ensureVNodeKey) else children

    val changeables = SeparatedReceivers(nodes, hasChildStreams, attributeReceivers)

    val eventHandlers = VDomProxy.emittersToSnabbDom(emitters)

    val dataObject = createDataObject(changeables, properties, eventHandlers)

    (childrenWithKey, dataObject)
  }

  def render(element: Element, vNode: VNode): IO[Unit] = for {
    node <- vNode
    elem <- IO(document.createElement("app"))
    _ <- IO(element.appendChild(elem))
    _ <- IO(patch(elem, node.asProxy))
  } yield ()
}
