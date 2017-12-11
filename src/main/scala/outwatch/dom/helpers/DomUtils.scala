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

  private[outwatch] case class StreamStatus(numChild: Int = 0, numChildren: Int = 0) {
    def hasChildOrChildren: Boolean = (numChild + numChildren) > 0
    def hasMultipleChildren: Boolean = numChildren > 1
  }
  private[outwatch] trait Children {
    def ::(mod: StringModifier): Children
    def ::(node: ChildVNode): Children
  }
  object Children {
    private def toVNode(mod: StringModifier) = StringVNode(mod.string)
    private def toModifier(node: StringVNode) = StringModifier(node.string)

    private[outwatch] case object Empty extends Children {
      override def ::(mod: StringModifier): Children = StringModifiers(mod :: Nil)
      override def ::(node: ChildVNode): Children = node match {
        case s: StringVNode => toModifier(s) :: this
        case n => n :: VNodes(Nil, StreamStatus())
      }
    }
    private[outwatch] case class StringModifiers(modifiers: List[StringModifier]) extends Children {
      override def ::(mod: StringModifier): Children = copy(mod :: modifiers)
      override def ::(node: ChildVNode): Children = node match {
        case s: StringVNode => toModifier(s) :: this // this should never happen
        case n => n :: VNodes(modifiers.map(toVNode), StreamStatus())
      }
    }
    private[outwatch] case class VNodes(nodes: List[ChildVNode], streamStatus: StreamStatus) extends Children {
      override def ::(mod: StringModifier): Children = copy(toVNode(mod) :: nodes)
      override def ::(node: ChildVNode): Children = node match {
        case s: StaticVNode => copy(nodes = s :: nodes)
        case s: ChildStreamReceiver => copy(s :: nodes, streamStatus.copy(numChild = streamStatus.numChild + 1))
        case s: ChildrenStreamReceiver => copy(s :: nodes, streamStatus.copy(numChildren = streamStatus.numChildren + 1))
      }
    }
  }

  private def createDataObject(changeables: SeparatedReceivers,
                               properties: Seq[Property],
                               emitters: Seq[Emitter]): DataObject = {

    val eventHandlers = VDomProxy.emittersToSnabbDom(emitters)

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

    DataObject(attrs, props, style, eventHandlers,
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

    def toProxy(changable: (Seq[Attribute], Seq[IO[StaticVNode]])): VNodeProxy = {
      val (attributes, nodes) = changable
      val newData = proxy.data.withUpdatedAttributes(attributes)

      if (nodes.isEmpty) {
        if (proxy.children.isDefined) {
          hFunction(proxy.sel, newData, proxy.children.get)
        } else {
          hFunction(proxy.sel, newData, proxy.text)
        }
      } else {
        hFunction(proxy.sel,newData, nodes.map(_.unsafeRunSync().asProxy)(breakOut): js.Array[VNodeProxy])
      }
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
    children: Children = Children.Empty
  )
  private[outwatch] def separateModifiers(args: Seq[VDomModifier_]): SeparatedModifiers = {
    args.foldRight(SeparatedModifiers())(separatorFn)
  }

  private[outwatch] def separatorFn(mod: VDomModifier_, res: SeparatedModifiers): SeparatedModifiers = (mod, res) match {
    case (em: Emitter, sf) => sf.copy(emitters = em :: sf.emitters)
    case (rc: AttributeStreamReceiver, sf) => sf.copy(attributeReceivers = rc :: sf.attributeReceivers)
    case (pr: Property, sf) => sf.copy(properties = pr :: sf.properties)
    case (vn: ChildVNode, sf) => sf.copy(children = vn :: sf.children)
    case (sm: StringModifier, sf) => sf.copy(children = sm :: sf.children)
    case (vn: CompositeModifier, sf) => vn.modifiers.foldRight(sf)(separatorFn)
    case (EmptyModifier, sf) => sf
  }

  private[outwatch] final case class SeparatedReceivers(
    children: Children,
    attributeStreamReceivers: List[AttributeStreamReceiver]
  ) {

    private val (childNodes, childStreamStatus) = children match {
      case Children.VNodes(nodes, streamStatus) => (nodes, streamStatus)
      case _ => (Nil, StreamStatus())
    }

    lazy val observable: Observable[(Seq[Attribute], Seq[IO[StaticVNode]])] = {
      val childStreamReceivers = if (childStreamStatus.hasChildOrChildren) {
        childNodes.foldRight(Observable.of(List.empty[IO[StaticVNode]])) {
          case (vn: StaticVNode, obs) => obs.combineLatestWith(BehaviorSubject(IO.pure(vn)))((nodes, n) => n :: nodes)
          case (csr: ChildStreamReceiver, obs) => obs.combineLatestWith(csr.childStream)((nodes, n) => n :: nodes)
          case (csr: ChildrenStreamReceiver, obs) =>
            obs.combineLatestWith(
              if (childStreamStatus.hasMultipleChildren) csr.childrenStream.startWith(Seq.empty) else csr.childrenStream
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
      attributeStreamReceivers.nonEmpty || childStreamStatus.hasChildOrChildren
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

  // ensure a key is present in the VTree modifiers. used to ensure efficient
  // Snabbdom patch operation in the presence of children streams. if there
  // already is a key, our default key is overriden by the modifiers.
  private def ensureVTreeKey(vtree: VTree): VTree = {
    val defaultKey = Key(vtree.hashCode)
    val newModifiers = defaultKey +: vtree.modifiers
    vtree.copy(modifiers = newModifiers)
  }

  private def ensureVNodeKey[N >: VTree](node: N): N = node match {
    case vtree: VTree => ensureVTreeKey(vtree)
    case other => other
  }

  private[outwatch] def extractChildrenAndDataObject(args: Seq[VDomModifier_]): (Children, DataObject) = {
    val SeparatedModifiers(emitters, attributeReceivers, properties, children) = separateModifiers(args)

    val childrenWithKey = children match {
      // if child streams exists, we want the static children in the same node have keys
      // for efficient patching when the streams change
      case c@Children.VNodes(vnodes, streams) => if (streams.hasChildOrChildren) c.copy(vnodes.map(ensureVNodeKey)) else c
      case c => c
    }

    val changeables = SeparatedReceivers(childrenWithKey, attributeReceivers)
    val dataObject = createDataObject(changeables, properties, emitters)

    (childrenWithKey, dataObject)
  }

  def render(element: Element, vNode: VNode): IO[Unit] = for {
    node <- vNode
    elem <- IO(document.createElement("app"))
    _ <- IO(element.appendChild(elem))
    _ <- IO(patch(elem, node.asProxy))
  } yield ()
}
