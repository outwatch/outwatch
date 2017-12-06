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

    val SeparatedProperties(insert, prepatch, update, postpatch, destroy, attributes, keys) = separateProperties(properties)
    val (attrs, props, style) = VDomProxy.attrsToSnabbDom(attributes)

    val insertHook = createHookSingle(insert)
    val prePatchHook = createHookPairOption(prepatch)
    val updateHook = createHookPair(update)
    val postPatchHook = createHookPair(postpatch)
    val destroyHook = createHookSingle(destroy)
    val key = keys.lastOption.map(_.value).orUndefined

    DataObject(attrs, props, style, handlers,
      Hooks(insertHook, prePatchHook, updateHook, postPatchHook, destroyHook),
      key
    )
  }

  private def createReceiverDataObject(changeables: SeparatedReceivers,
                                       properties: Seq[Property],
                                       eventHandlers: js.Dictionary[js.Function1[Event, Unit]]) = {

    val SeparatedProperties(insert, prepatch, update, postpatch, destroy, attributes, keys) = separateProperties(properties)

    val (attrs, props, style) = VDomProxy.attrsToSnabbDom(attributes)
    val subscriptionRef = STRef.empty[Subscription]

    val insertHook = createInsertHook(changeables, subscriptionRef, insert)
    val prePatchHook = createHookPairOption(prepatch)
    val updateHook = createHookPair(update)
    val postPatchHook = createHookPair(postpatch)
    val destroyHook = createDestroyHook(subscriptionRef, destroy)
    val key = keys.lastOption.fold[Key.Value](changeables.hashCode)(_.value)

    DataObject(
      attrs, props, style, eventHandlers,
      Hooks(insertHook, prePatchHook, updateHook, postPatchHook, destroyHook),
      key
    )
  }

  private def createHookSingle(hooks: Seq[Hook[Element]]): js.UndefOr[Hooks.HookSingleFn] = {
    Option(hooks).filter(_.nonEmpty).map[Hooks.HookSingleFn](hooks =>
      (p: VNodeProxy) => for (e <- p.elm) hooks.foreach(_.observer.next(e))
    ).orUndefined
  }

  private def createHookPair(hooks: Seq[Hook[(Element, Element)]]): js.UndefOr[Hooks.HookPairFn] = {
    Option(hooks).filter(_.nonEmpty).map[Hooks.HookPairFn](hooks =>
      (old: VNodeProxy, cur: VNodeProxy) => for (o <- old.elm; c <- cur.elm) hooks.foreach(_.observer.next((o, c)))
    ).orUndefined
  }

  private def createHookPairOption(hooks: Seq[Hook[(Option[Element], Option[Element])]]): js.UndefOr[Hooks.HookPairFn] = {
    Option(hooks).filter(_.nonEmpty).map[Hooks.HookPairFn](hooks =>
      (old: VNodeProxy, cur: VNodeProxy) => hooks.foreach(_.observer.next((old.elm.toOption, cur.elm.toOption)))
    ).orUndefined
  }

  private def createInsertHook(changables: SeparatedReceivers,
                               subscriptionRef: STRef[Subscription],
                               hooks: Seq[InsertHook]): Hooks.HookSingleFn = (proxy: VNodeProxy) => {

    def toProxy(changable: (Seq[Attribute], Seq[VNode])): VNodeProxy = {
      val (attributes, nodes) = changable
      hFunction(
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

  private def createDestroyHook(subscription: STRef[Subscription], hooks: Seq[DestroyHook]): Hooks.HookSingleFn = (proxy: VNodeProxy) => {
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
    multipleChildrenStreams: Boolean = false,
    attributeStreamReceivers: List[AttributeStreamReceiver] = Nil
  ) {

    lazy val observable: Observable[(Seq[Attribute], Seq[VNode])] = {
      val childStreamReceivers = if (hasNodeStreams) {
        childNodes.foldRight(Observable.of(List.empty[VNode])) {
          case (vn: VNode_, obs) => obs.combineLatestWith(BehaviorSubject(IO.pure(vn)))((nodes, n) => n :: nodes)
          case (csr: ChildStreamReceiver, obs) => obs.combineLatestWith(csr.childStream)((nodes, n) => n :: nodes)
          case (csr: ChildrenStreamReceiver, obs) =>
            obs.combineLatestWith(
              if (multipleChildrenStreams) csr.childrenStream.startWith(Seq.empty) else csr.childrenStream
            )((nodes, n) => n.toList ++ nodes)
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
    prePatchHooks: List[PrePatchHook] = Nil,
    updateHooks: List[UpdateHook] = Nil,
    postPatchHooks: List[PostPatchHook] = Nil,
    destroyHooks: List[DestroyHook] = Nil,
    attributeHooks: List[Attribute] = Nil,
    keys: List[Key] = Nil
  )
  private[outwatch] def separateProperties(properties: Seq[Property]): SeparatedProperties = {
    properties.foldRight(SeparatedProperties()) {
      case (ih: InsertHook, sp) => sp.copy(insertHooks = ih :: sp.insertHooks)
      case (pph: PrePatchHook, sp) => sp.copy(prePatchHooks = pph :: sp.prePatchHooks)
      case (uh: UpdateHook, sp) => sp.copy(updateHooks = uh :: sp.updateHooks)
      case (pph: PostPatchHook, sp) => sp.copy(postPatchHooks = pph :: sp.postPatchHooks)
      case (dh: DestroyHook, sp) => sp.copy(destroyHooks = dh :: sp.destroyHooks)
      case (at: Attribute, sp) => sp.copy(attributeHooks = at :: sp.attributeHooks)
      case (key: Key, sp) => sp.copy(keys = key :: sp.keys)
    }
  }


  private final case class ChildrenNodes(
    children: List[VNode_] = Nil,
    hasStreams: Boolean = false,
    childrenStreams: Int = 0
  )
  private def extractChildren(nodes: Seq[ChildVNode]): ChildrenNodes = nodes.foldRight(ChildrenNodes()) {
    case (vn: VNode_, cn) => cn.copy(children = vn :: cn.children)
    case (_: ChildStreamReceiver, cn) => cn.copy(hasStreams = true)
    case (_: ChildrenStreamReceiver, cn) => cn.copy(hasStreams = true, childrenStreams = cn.childrenStreams + 1)
  }

  // ensure a key is present in the VTree modifiers
  // used to ensure efficient Snabbdom patch operation in the presence of children streams
  private def ensureVTreeKey(vtree: VTree): VTree = {
    val hasKey = vtree.modifiers.exists(m => m.unsafeRunSync().isInstanceOf[Key])
    val newModifiers = if (hasKey) vtree.modifiers else IO.pure(Key(this.hashCode)) +: vtree.modifiers
    vtree.copy(modifiers = newModifiers)
  }

  private[outwatch] def ensureVNodeKey[N >: VNode_](node: N): N = node match {
    case vtree: VTree => ensureVTreeKey(vtree)
    case other => other
  }

  private[outwatch] def extractChildrenAndDataObject(args: Seq[VDomModifier_]): (Seq[VNode_], DataObject) = {
    val SeparatedModifiers(emitters, attributeReceivers, properties, nodes) = separateModifiers(args)

    val ChildrenNodes(children, hasChildStreams, childrenStreams) = extractChildren(nodes)

    // if child streams exists, we want the static children in the same node have keys
    // for efficient patching when the streams change
    val childrenWithKey = if (hasChildStreams) children.map(ensureVNodeKey) else children
    val nodesWithKey = if (hasChildStreams) nodes.map(ensureVNodeKey) else nodes

    val changeables = SeparatedReceivers(nodesWithKey, hasChildStreams, childrenStreams > 1, attributeReceivers)

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
