package outwatch.dom.helpers

import cats.effect.implicits._
import cats.effect.{Effect, IO}
import cats.implicits._
import monix.execution.Ack.Continue
import monix.execution.Scheduler
import monix.execution.cancelables.SingleAssignCancelable
import org.scalajs.dom
import outwatch.dom._
import snabbdom._

import scala.collection.breakOut
import scala.scalajs.js
import scala.scalajs.js.JSConverters._

trait SnabbdomFactory[F[+_]] { this:VDomModifierFactory[F] with SeparatedModifiersFactory[F] =>
  implicit val effectF: Effect[F]

  private[outwatch] trait SnabbdomStyles { this: SeparatedStyles =>
    def toSnabbdom: js.Dictionary[Style.Value] = {
      val styleDict = js.Dictionary[Style.Value]()

      val delayedDict = js.Dictionary[String]()
      val removeDict = js.Dictionary[String]()
      val destroyDict = js.Dictionary[String]()

      styles.foreach {
        case s: BasicStyle => styleDict(s.title) = s.value
        case s: DelayedStyle => delayedDict(s.title) = s.value
        case s: RemoveStyle => removeDict(s.title) = s.value
        case s: DestroyStyle => destroyDict(s.title) = s.value
        case a: AccumStyle =>
          styleDict(a.title) = styleDict.get(a.title).map(s =>
            a.accum(s.asInstanceOf[String], a.value): Style.Value
          ).getOrElse(a.value)

      }

      if (delayedDict.nonEmpty) styleDict("delayed") = delayedDict: Style.Value
      if (removeDict.nonEmpty) styleDict("remove") = removeDict: Style.Value
      if (destroyDict.nonEmpty) styleDict("destroy") = destroyDict: Style.Value

      styleDict
    }
  }

  private[outwatch] trait SnabbdomAttributes { this:SeparatedAttributes =>

    type jsDict[T] = js.Dictionary[T]

    def toSnabbdom: (jsDict[Attr.Value], jsDict[Prop.Value], jsDict[Style.Value]) = {
      val attrsDict = js.Dictionary[Attr.Value]()
      val propsDict = js.Dictionary[Prop.Value]()

      attrs.foreach {
        case a: BasicAttr => attrsDict(a.title) = a.value
        case a: AccumAttr => attrsDict(a.title) = attrsDict.get(a.title).map(a.accum(_, a.value)).getOrElse(a.value)
      }
      props.foreach { p => propsDict(p.title) = p.value }

      (attrsDict, propsDict, styles.toSnabbdom)
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

  private[outwatch] trait SnabbdomHooks { this:SeparatedHooks =>
    private def createHookSingle(hooks: Seq[Hook[dom.Element]]): js.UndefOr[Hooks.HookSingleFn] = {
      Option(hooks).filter(_.nonEmpty).map[Hooks.HookSingleFn](hooks =>
        (p: VNodeProxy) => for (e <- p.elm) hooks.foreach(_.observer.onNext(e))
      ).orUndefined
    }

    private def createHookPair(hooks: Seq[Hook[(dom.Element, dom.Element)]]): js.UndefOr[Hooks.HookPairFn] = {
      Option(hooks).filter(_.nonEmpty).map[Hooks.HookPairFn](hooks =>
        (old: VNodeProxy, cur: VNodeProxy) => for (o <- old.elm; c <- cur.elm) hooks.foreach(_.observer.onNext((o, c)))
      ).orUndefined
    }

    private def createHookPairOption(hooks: Seq[Hook[(Option[dom.Element], Option[dom.Element])]]
                                    ): js.UndefOr[Hooks.HookPairFn] = {
      Option(hooks).filter(_.nonEmpty).map[Hooks.HookPairFn](hooks =>
        (old: VNodeProxy, cur: VNodeProxy) => hooks.foreach(_.observer.onNext((old.elm.toOption, cur.elm.toOption)))
      ).orUndefined
    }

    private def createInsertHook(receivers: Receivers,
                                 subscription: SingleAssignCancelable,
                                 hooks: Seq[InsertHook]
                                )(implicit s: Scheduler): Hooks.HookSingleFn = (proxy: VNodeProxy) => {

      def toProxy(state: VNodeState): F[VNodeProxy] = {
        val newData = SeparatedAttributes.from(state.attributes.values.toSeq).updateDataObject(proxy.data)

        if (state.nodes.isEmpty) {
          if (proxy.children.isDefined) {
            effectF.pure(hFunction(proxy.sel, newData, proxy.children.get))
          } else {
            effectF.pure(hFunction(proxy.sel, newData, proxy.text))
          }
        } else {
          val nodes = state.nodes.reduceLeft(_ ++ _).toList.sequence
          nodes.map(list => hFunction(proxy.sel, newData, list.map(_.toSnabbdom).toJSArray))
        }
      }

      subscription := receivers.observable
        .map(toProxy)
        .startWith(Seq(effectF.pure(proxy)))
        .bufferSliding(2, 1)
        .subscribe(
          { case Seq(old, crt) => (old, crt).mapN(patch.apply).runAsync(_ => IO.unit).unsafeRunSync(); Continue },
          error => dom.console.error(error.getMessage + "\n" + error.getStackTrace.mkString("\n"))
        )

      proxy.elm.foreach((e: dom.Element) => hooks.foreach(_.observer.onNext(e)))
    }


    private def createDestroyHook(
                                   subscription: SingleAssignCancelable, hooks: Seq[DestroyHook]
                                 ): Hooks.HookSingleFn = (proxy: VNodeProxy) => {
      proxy.elm.foreach((e: dom.Element) => hooks.foreach(_.observer.onNext(e)))
      subscription.cancel()
      ()
    }

    def toSnabbdom(receivers: Receivers)(implicit s: Scheduler): Hooks = {
      val (insertHook, destroyHook) = if (receivers.nonEmpty) {
        val subscription = SingleAssignCancelable()
        val insertHook: js.UndefOr[Hooks.HookSingleFn] = createInsertHook(receivers, subscription, insertHooks)
        val destroyHook: js.UndefOr[Hooks.HookSingleFn] = createDestroyHook(subscription, destroyHooks)
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

    private[outwatch] def createDataObject(receivers: Receivers)(implicit s: Scheduler): DataObject = {

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

    private[outwatch] def toSnabbdom(nodeType: String)(implicit s: Scheduler): VNodeProxy = {

      // if child streams exists, we want the static children in the same node have keys
      // for efficient patching when the streams change
      val childrenWithKey = children.ensureKey
      val dataObject = createDataObject(Receivers(childrenWithKey, attributeReceivers))

      childrenWithKey match {
        case Children.VNodes(vnodes, _) =>
          implicit val scheduler = s
          val childProxies: js.Array[VNodeProxy] = vnodes.collect { case s: StaticVNode => s.toSnabbdom }(breakOut)
          hFunction(nodeType, dataObject, childProxies)
        case Children.StringModifiers(textChildren) =>
          hFunction(nodeType, dataObject, textChildren.map(_.string).mkString)
        case Children.Empty() =>
          hFunction(nodeType, dataObject)
      }
    }
  }

}
