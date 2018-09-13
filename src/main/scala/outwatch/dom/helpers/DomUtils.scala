package outwatch.dom.helpers

import monix.reactive.Observable
import outwatch.dom._
import scala.scalajs.js

private[outwatch] object SeparatedModifiers {
  private[outwatch] def from(modifiers: js.Array[_ <: Modifier]): SeparatedModifiers = {
    val m = new SeparatedModifiers()
    m.append(modifiers)
    m
  }
}

private[outwatch] class SeparatedModifiers {
  val properties = new SeparatedProperties
  val emitters = new js.Array[Emitter]()
  val children = new Children

  def append(modifiers: js.Array[_ <: Modifier]): Unit = modifiers.foreach {
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
  val nodes = new js.Array[ChildVNode]()
  var hasStream: Boolean = false
  var hasVTree: Boolean = false
}

private[outwatch] class SeparatedProperties {
  val attributes = new SeparatedAttributes()
  val hooks = new SeparatedHooks()
  val keys = new js.Array[Key]()
}

private[outwatch] class SeparatedAttributes {
  val attrs = new js.Array[Attr]()
  val props = new js.Array[Prop]()
  val styles = new js.Array[Style]()
}

private[outwatch] class SeparatedHooks {
  val insertHooks = new js.Array[InsertHook]()
  val prePatchHooks = new js.Array[PrePatchHook]()
  val updateHooks = new js.Array[UpdateHook]()
  val postPatchHooks = new js.Array[PostPatchHook]()
  val destroyHooks = new js.Array[DestroyHook]()
  val domMountHooks = new js.Array[DomMountHook]()
  val domUnmountHooks = new js.Array[DomUnmountHook]()
  val domUpdateHooks = new js.Array[DomUpdateHook]()
}

private[outwatch] sealed trait ContentKind
private[outwatch] object ContentKind {
  case class Dynamic(observable: Observable[Modifier], initialValue: Modifier) extends ContentKind
  case class Static(modifier: Modifier) extends ContentKind
}

// StreamableModifiers takes a list of modifiers. It constructs an Observable
// of updates from dynamic modifiers in this list.
private[outwatch] class StreamableModifiers[T <: Modifier](modifiers: js.Array[T]) {

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
  val initialModifiers = new js.Array[Modifier](modifiers.size)

  // for each node which might be dynamic, we have an Observable of Modifier updates
  val updaterObservables = new js.Array[Observable[(Int, Modifier)]]

  // an observable representing the current state of this VNode. We take all
  // state update functions we have from dynamic modifiers and then scan over
  // them starting with the initial state.
  private val innerObservable = {
    var i = 0;
    var j = 0;
    while (i < modifiers.size) {
      val index = i
      val jndex = j
      handleStreamedModifier(modifiers(index)) match {
        case ContentKind.Dynamic(stream, initialValue) =>
          initialModifiers(index) = initialValue
          updaterObservables(jndex) = stream.map { mod =>
            (index, mod)
          }
          j += 1
        case ContentKind.Static(mod) =>
          initialModifiers(index) = mod
      }
      i += 1
    }

    Observable.merge(updaterObservables: _*)
      .map { case (index, mod) =>
        initialModifiers(index) = mod
      }
  }

  val observable = innerObservable.map(_ => initialModifiers)
}

// Receivers represent a VNode with its static/streamable children and its
// attribute streams. it is about capturing the dynamic content of a node.
// it is considered "empty" if it is only static. Otherwise it provides an
// Observable to stream the current modifiers of this node.
private[outwatch] final class Receivers(childNodes: js.Array[ChildVNode]) {
  private val streamableModifiers = new StreamableModifiers(childNodes)
  def initialState: js.Array[Modifier] = streamableModifiers.initialModifiers
  def observable: Observable[js.Array[Modifier]] = streamableModifiers.observable
}
