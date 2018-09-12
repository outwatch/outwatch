package outwatch.dom.helpers

import monix.reactive.Observable
import outwatch.dom._

import scala.collection.mutable

private[outwatch] object SeparatedModifiers {
  private[outwatch] def from(modifiers: Seq[Modifier]): SeparatedModifiers = {
    val m = new SeparatedModifiers()
    m.append(modifiers)
    m
  }
}

private[outwatch] class SeparatedModifiers {
  val properties = new SeparatedProperties
  val emitters = new mutable.ArrayBuffer[Emitter]()
  val children = new Children

  def append(modifiers: Seq[Modifier]): Unit = modifiers.foreach {
    case em: Emitter =>
      emitters += em
    case cm: CompositeModifier =>
      append(cm.modifiers)
    case s: StringVNode =>
      children.nodes += s
    case s: VTree =>
      children.nodes += s
      children.hasVTree = true
    case s: ModifierStreamReceiver =>
      children.nodes += s
      children.hasStream = true
    case key: Key =>
      properties.keys += key
    case a : Attr =>
      properties.attributes.attrs += a
    case p : Prop =>
      properties.attributes.props += p
    case s : Style =>
      properties.attributes.styles += s
    case h: DomMountHook =>
      properties.hooks.domMountHooks += h
    case h: DomUnmountHook =>
      properties.hooks.domUnmountHooks += h
    case h: DomUpdateHook =>
      properties.hooks.domUpdateHooks += h
    case h: InsertHook =>
      properties.hooks.insertHooks += h
    case h: PrePatchHook =>
      properties.hooks.prePatchHooks += h
    case h: UpdateHook =>
      properties.hooks.updateHooks += h
    case h: PostPatchHook =>
      properties.hooks.postPatchHooks += h
    case h: DestroyHook =>
      properties.hooks.destroyHooks += h
    case EmptyModifier =>
      ()
  }
}

private[outwatch] class Children {
  val nodes = new mutable.ArrayBuffer[ChildVNode]()
  var hasStream: Boolean = false
  var hasVTree: Boolean = false
}

private[outwatch] class SeparatedProperties {
  val attributes = new SeparatedAttributes()
  val hooks = new SeparatedHooks()
  val keys = new mutable.ArrayBuffer[Key]()
}

private[outwatch] class SeparatedAttributes {
  val attrs = new mutable.ArrayBuffer[Attr]()
  val props = new mutable.ArrayBuffer[Prop]()
  val styles = new mutable.ArrayBuffer[Style]()
}

private[outwatch] class SeparatedHooks {
  val insertHooks = new mutable.ArrayBuffer[InsertHook]()
  val prePatchHooks = new mutable.ArrayBuffer[PrePatchHook]()
  val updateHooks = new mutable.ArrayBuffer[UpdateHook]()
  val postPatchHooks = new mutable.ArrayBuffer[PostPatchHook]()
  val destroyHooks = new mutable.ArrayBuffer[DestroyHook]()
  val domMountHooks = new mutable.ArrayBuffer[DomMountHook]()
  val domUnmountHooks = new mutable.ArrayBuffer[DomUnmountHook]()
  val domUpdateHooks = new mutable.ArrayBuffer[DomUpdateHook]()
}

private[outwatch] sealed trait ContentKind
private[outwatch] object ContentKind {
  case class Dynamic(observable: Observable[Modifier], initialValue: Modifier) extends ContentKind
  case class Static(modifier: Modifier) extends ContentKind
}

// StreamableModifiers takes a list of modifiers. It constructs an Observable
// of updates from dynamic modifiers in this list.
private[outwatch] class StreamableModifiers(modifiers: Seq[Modifier]) {

  //TODO: hidden signature of this method (we need StaticModifier as a type)
  //handleStreamedModifier: Modifier => Either[StaticModifier, Observable[StaticModifier]]
  private val handleStreamedModifier: Modifier => ContentKind = {
    case ModifierStreamReceiver(modStream) =>
      val observable = modStream.observable.switchMap[Modifier] { mod =>
        handleStreamedModifier(mod.unsafeRunSync) match {
          //TODO: why is startWith different and leaks a subscription? see tests with: stream.startWith(initialValue :: Nil)
          case ContentKind.Dynamic(stream, initialValue) => Observable.concat(Observable.now(initialValue), stream)
          case ContentKind.Static(mod) => Observable.now(mod)
        }
      }

      handleStreamedModifier(modStream.value.fold[Modifier](EmptyModifier)(_.unsafeRunSync)) match {
        case ContentKind.Dynamic(initialObservable, mod) =>
          val combinedObservable = observable.publishSelector { observable =>
            Observable.merge(initialObservable.takeUntil(observable), observable)
          }
          ContentKind.Dynamic(combinedObservable, mod)
        case ContentKind.Static(mod) =>
          ContentKind.Dynamic(observable, mod)
      }

    case CompositeModifier(modifiers) if (modifiers.nonEmpty) =>
      val streamableModifiers = new StreamableModifiers(modifiers)
      if (streamableModifiers.updaterObservables.isEmpty) {
        ContentKind.Static(CompositeModifier(modifiers))
      } else {
        ContentKind.Dynamic(
          streamableModifiers.observable.map(CompositeModifier(_)),
          CompositeModifier(streamableModifiers.initialModifiers))
      }


    case mod => ContentKind.Static(mod)
  }


  // the nodes array has a fixed size - each static child node is one element
  // and the dynamic nodes can place one element on each update and start with
  // EmptyModifier, and we reserve an array element for each attribute
  // receiver.
  val initialModifiers = new Array[Modifier](modifiers.size)

  // for each node which might be dynamic, we have an Observable of Modifier updates
  val updaterObservables = new collection.mutable.ArrayBuffer[Observable[Array[Modifier] => Array[Modifier]]]

  // an observable representing the current state of this VNode. We take all
  // state update functions we have from dynamic modifiers and then scan over
  // them starting with the initial state.
  val observable = {
    var i = 0;
    while (i < modifiers.size) {
      val index = i
      handleStreamedModifier(modifiers(index)) match {
        case ContentKind.Dynamic(stream, initialValue) =>
          initialModifiers(index) = initialValue
          updaterObservables += stream.map { mod =>
            (array: Array[Modifier]) => array.updated(index, mod)
          }
        case ContentKind.Static(mod) =>
          initialModifiers(index) = mod
      }
      i += 1
    }

    Observable.merge(updaterObservables: _*)
      .scan(initialModifiers)((modifiers, update) => update(modifiers))
  }
}

// Receivers represent a VNode with its static/streamable children and its
// attribute streams. it is about capturing the dynamic content of a node.
// it is considered "empty" if it is only static. Otherwise it provides an
// Observable to stream the current modifiers of this node.
private[outwatch] final class Receivers(childNodes: Seq[ChildVNode]) {
  private val streamableModifiers = new StreamableModifiers(childNodes)
  def initialState: Array[Modifier] = streamableModifiers.initialModifiers
  def observable: Observable[Array[Modifier]] = streamableModifiers.observable
}
