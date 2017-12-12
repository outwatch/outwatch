package outwatch.dom.helpers

import cats.effect.IO
import org.scalajs.dom
import outwatch.dom.{Attr, Attribute, DestroyHook, Emitter, EmptyAttribute, Hook, InsertHook, Key, Prop, StaticVNode, Style}
import rxscalajs.subscription.Subscription
import snabbdom._

import scala.scalajs.js.JSConverters._
import scala.collection.breakOut
import scala.scalajs.js

private[outwatch] trait SnabbdomAttributes { self: SeparatedAttributes =>

  type jsDict[T] = js.Dictionary[T]

  def toSnabbdom: (jsDict[Attr.Value], jsDict[Prop.Value], jsDict[String]) = {
    val attrsDict = js.Dictionary[Attr.Value]()
    val propsDict = js.Dictionary[Prop.Value]()
    val styleDict = js.Dictionary[String]()

    attributes.foreach {
      case a: Attr => attrsDict(a.title) = a.value
      case p: Prop => propsDict(p.title) = p.value
      case s: Style => styleDict(s.title) = s.value
      case EmptyAttribute =>
    }

    (attrsDict, propsDict, styleDict)
  }

  private def merge[T](first: js.Dictionary[T], second: js.Dictionary[T]) = {
    val result = js.Dictionary.empty[T]
    first.foreach { case (key, value) => result(key) = value }
    second.foreach { case (key, value) => result(key) = value }
    result
  }

  def updateDataObject(obj: DataObject): DataObject = {

    val (attrs, props, style) = toSnabbdom
    DataObject(
      attrs = merge(obj.attrs, attrs),
      props = merge(obj.props, props),
      style = merge(obj.style, style),
      on = obj.on, hook = obj.hook, key = obj.key
    )
  }
}

private[outwatch] trait SnabbdomHooks { self: SeparatedHooks =>

  private def createHookSingle(hooks: Seq[Hook[dom.Element]]): js.UndefOr[Hooks.HookSingleFn] = {
    Option(hooks).filter(_.nonEmpty).map[Hooks.HookSingleFn](hooks =>
      (p: VNodeProxy) => for (e <- p.elm) hooks.foreach(_.observer.next(e))
    ).orUndefined
  }

  private def createHookPair(hooks: Seq[Hook[(dom.Element, dom.Element)]]): js.UndefOr[Hooks.HookPairFn] = {
    Option(hooks).filter(_.nonEmpty).map[Hooks.HookPairFn](hooks =>
      (old: VNodeProxy, cur: VNodeProxy) => for (o <- old.elm; c <- cur.elm) hooks.foreach(_.observer.next((o, c)))
    ).orUndefined
  }

  private def createHookPairOption(hooks: Seq[Hook[(Option[dom.Element], Option[dom.Element])]]
  ): js.UndefOr[Hooks.HookPairFn] = {
    Option(hooks).filter(_.nonEmpty).map[Hooks.HookPairFn](hooks =>
      (old: VNodeProxy, cur: VNodeProxy) => hooks.foreach(_.observer.next((old.elm.toOption, cur.elm.toOption)))
    ).orUndefined
  }

  private def createInsertHook(receivers: Receivers,
    subscriptionRef: STRef[Subscription],
    hooks: Seq[InsertHook]
  ): Hooks.HookSingleFn = (proxy: VNodeProxy) => {

    def toProxy(changable: (Seq[Attribute], Seq[IO[StaticVNode]])): VNodeProxy = {
      val (attributes, nodes) = changable
      val newData = SeparatedAttributes(attributes.toList).updateDataObject(proxy.data)

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

    val subscription = receivers.observable
      .map(toProxy)
      .startWith(proxy)
      .pairwise
      .subscribe({ case (prev, crt) => patch(prev, crt) }, dom.console.error(_))

    subscriptionRef.put(subscription).unsafeRunSync()

    proxy.elm.foreach((e: dom.Element) => hooks.foreach(_.observer.next(e)))
  }


  private def createDestroyHook(
    subscription: STRef[Subscription],hooks: Seq[DestroyHook]
  ): Hooks.HookSingleFn = (proxy: VNodeProxy) => {
    proxy.elm.foreach((e: dom.Element) => hooks.foreach(_.observer.next(e)))
    subscription.update { s => s.unsubscribe(); s }.unsafeRunSync()
    ()
  }

  def toSnabbdom(receivers: Receivers): Hooks = {
    val (insertHook, destroyHook) = if (receivers.nonEmpty) {
      val subscriptionRef = STRef.empty[Subscription]
      val insertHook: js.UndefOr[Hooks.HookSingleFn] = createInsertHook(receivers, subscriptionRef, insertHooks)
      val destroyHook: js.UndefOr[Hooks.HookSingleFn] = createDestroyHook(subscriptionRef, destroyHooks)
      (insertHook, destroyHook)
    }
    else {
      val insertHook = createHookSingle(insertHooks)
      val destroyHook = createHookSingle(destroyHooks)
      (insertHook, destroyHook)
    }
    val prePatchHook = createHookPairOption(prePatchHooks)
    val updateHook = createHookPair(updateHooks)
    val postPatchHook = createHookPair(postPatchHooks)

    Hooks(insertHook, prePatchHook, updateHook, postPatchHook, destroyHook)
  }
}

private[outwatch] trait SnabbdomEmitters { self: SeparatedEmitters =>

  private def emittersToFunction(emitters: Seq[Emitter]): js.Function1[dom.Event, Unit] = {
    (event: dom.Event) => emitters.foreach(_.trigger(event))
  }

  def toSnabbdom: js.Dictionary[js.Function1[dom.Event, Unit]] = {
    emitters
      .groupBy(_.eventType)
      .mapValues(emittersToFunction)
      .toJSDictionary
  }
}

private[outwatch] trait SnabbdomModifiers { self: SeparatedModifiers =>

  private[outwatch] def createDataObject(receivers: Receivers): DataObject = {

    val keyOption = properties.keys.lastOption
    val key = if (receivers.nonEmpty) {
      keyOption.fold[Key.Value](receivers.hashCode)(_.value): js.UndefOr[Key.Value]
    } else {
      keyOption.map(_.value).orUndefined
    }

    val (attrs, props, style) = properties.attributes.toSnabbdom
    DataObject(
      attrs, props, style, emitters.toSnabbdom,
      properties.hooks.toSnabbdom(receivers),
      key
    )
  }

  private[outwatch] def toSnabbdom(nodeType: String): VNodeProxy = {

    // if child streams exists, we want the static children in the same node have keys
    // for efficient patching when the streams change
    val childrenWithKey = children.ensureKey
    val dataObject = createDataObject(Receivers(childrenWithKey, attributeReceivers))

    childrenWithKey match {
      case Children.VNodes(vnodes, _) =>
        val childProxies: js.Array[VNodeProxy] = vnodes.collect { case s: StaticVNode => s.asProxy }(breakOut)
        hFunction(nodeType, dataObject, childProxies)
      case Children.StringModifiers(textChildren) =>
        hFunction(nodeType, dataObject, textChildren.map(_.string).mkString)
      case Children.Empty =>
        hFunction(nodeType, dataObject)
    }
  }
}
