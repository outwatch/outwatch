package outwatch.dom

import cats.effect.IO
import monix.reactive.Observable
import org.scalajs.dom.Element
import outwatch.AsVDomModifier
import cats.syntax.functor._
import outwatch.dom.helpers.SeparatedModifiers

import scala.scalajs.js

sealed trait ChildCommand
object ChildCommand {

  case class Append(node: VNode) extends ChildCommand
  case class Prepend(node: VNode) extends ChildCommand
  case class Set(list: List[VNode]) extends ChildCommand
  case class Insert(index: Int, node: VNode) extends ChildCommand
  case class Replace(index: Int, node: VNode) extends ChildCommand
  case class Move(fromIndex: Int, toIndex: Int) extends ChildCommand
  case class Remove(index: Int) extends ChildCommand

  case class ReplaceId(id: ChildId, node: VNode) extends ChildCommand
  case class InsertBeforeId(id: ChildId, node: VNode) extends ChildCommand
  case class InsertBehindId(id: ChildId, node: VNode) extends ChildCommand
  case class MoveId(fromId: ChildId, toIndex: Int) extends ChildCommand
  case class MoveBeforeId(fromId: ChildId, toId: ChildId) extends ChildCommand
  case class MoveBehindId(fromId: ChildId, toId: ChildId) extends ChildCommand
  case class RemoveId(id: ChildId) extends ChildCommand

  private[outwatch] def stream(valueStream: ValueObservable[Seq[ChildCommand]]): IO[ValueObservable[VDomModifier]] = IO {
    val children = new js.Array[VTree]

    valueStream.map { cmds =>
      val idToIndex: ChildId => Int = {
        case ChildId.Key(key) => children.indexWhere { tree =>
          tree.proxy.fold(false)(_.key.fold(false)((k: Key.Value) => k == key))
        }
        case ChildId.Element(element) => children.indexWhere { tree =>
          tree.proxy.fold(false)(_.elm.fold(false)((e: Element) => e == element))
        }
      }

      def isSaneIndex(index: Int): Boolean = index >= 0 && index < children.length

      def replaceByIndex(index: Int, node: VNode): Unit = {
        children(index) = node.unsafeRunSync
      }

      def moveByIndex(fromIndex: Int, toIndex: Int): Unit = {
        if (isSaneIndex(fromIndex) && isSaneIndex(toIndex)) {
          val tree = children(fromIndex)
          children.remove(fromIndex)
          children.insert(toIndex, tree)
        }
      }

      def insertByIndex(index: Int, node: VNode): Unit = {
        if (isSaneIndex(index)) {
          children.insert(index, node.unsafeRunSync)
        }
      }

      def removeByIndex(index: Int): Unit = {
        if (isSaneIndex(index)) {
          children.remove(index)
          ()
        }
      }

      cmds foreach {
        case Append(node) =>
          children.push(node.unsafeRunSync)
          ()
        case Prepend(node) =>
          children.prepend(node.unsafeRunSync)
        case Set(list) =>
          children.clear()
          children.push(list.map(_.unsafeRunSync): _*)
          ()
        case Insert(index, node) =>
          insertByIndex(index, node)
        case InsertBeforeId(id, node) =>
          insertByIndex(idToIndex(id), node)
        case InsertBehindId(id, node) =>
          val index = idToIndex(id)
          insertByIndex(if (index == -1) -1 else index + 1, node)
        case Replace(index, node) =>
          replaceByIndex(index, node)
        case ReplaceId(id, node) =>
          replaceByIndex(idToIndex(id), node)
        case Move(fromIndex, toIndex) =>
          moveByIndex(fromIndex, toIndex)
        case MoveId(fromId, toIndex) =>
          moveByIndex(idToIndex(fromId), toIndex)
        case MoveBeforeId(fromId, toId) =>
          moveByIndex(idToIndex(fromId), idToIndex(toId))
        case MoveBehindId(fromId, toId) =>
          val toIdx = idToIndex(toId)
          moveByIndex(idToIndex(fromId), if (toIdx == -1) -1 else toIdx + 1)
        case Remove(index) =>
          removeByIndex(index)
        case RemoveId(id) =>
          removeByIndex(idToIndex(id))
      }

      IO.pure(CompositeModifier(children))
    }
  }
}

sealed trait ChildId
object ChildId {
  case class Key(key: outwatch.dom.Key.Value) extends ChildId
  case class Element(elem: org.scalajs.dom.Element) extends ChildId
}

