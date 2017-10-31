package outwatch

import cats.effect.IO
import outwatch.dom.VDomModifier.{StringNode, VTree}

import scala.language.implicitConversions


package object dom extends Attributes with Tags with Handlers {

  type VNode = IO[VNode_]
  type VDomModifier = IO[VDomModifier_]

  implicit def stringNode(string: String): VDomModifier = IO.pure(StringNode(string))

  implicit def optionIsEmptyModifier(opt: Option[VDomModifier]): VDomModifier = opt getOrElse IO.pure(EmptyVDomModifier)

  implicit class ioVTreeMerge(tree: IO[VTree]) {
    def apply(args: VDomModifier*): IO[VTree] = {
      tree.flatMap(vtree => IO.pure(VTree(vtree.nodeType, vtree.modifiers ++ args)))
    }
  }
}
