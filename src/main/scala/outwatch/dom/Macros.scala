package outwatch.dom

import org.scalajs.dom

import scala.annotation.compileTimeOnly

@compileTimeOnly("compile-time macro expansion")
private[outwatch] object VTreeApply {
  import scala.reflect.macros.blackbox.Context

  private def injectTagContext[T <: dom.Element : c.WeakTypeTag](c: Context)(src: c.Tree, injectable: c.universe.TermName): c.Tree = {
    import c.universe._

    val tType = weakTypeOf[T]
    val tEventType = weakTypeOf[TypedCurrentTargetEvent[T]]

    object Transformer extends c.universe.Transformer {
      override def transform(tree: c.Tree): c.Tree = {
        if (tree.tpe == null) tree
        else if (tree.tpe =:= weakTypeOf[TagContext.Unassigned.type]) q"$injectable"
        else if (tree.isType && tree.tpe =:= weakTypeOf[TagContext.DummyElement]) q"$tType"
        else if (tree.isType && tree.tpe =:= weakTypeOf[TypedCurrentTargetEvent[TagContext.DummyElement]]) q"$tEventType"
        else if (tree.isType && tree.tpe <:< weakTypeOf[TypedCurrentTargetEvent[TagContext.DummyElement]]) {
          tree.tpe match {
            //TODO: why do we need to force transformation of parts of the refined type?
            case RefinedType(components, _) =>
              val types = components.map(tpe => transform(q"$tpe"))
              tq"${types.head} with ..${types.tail}"
            case _ => tree
          }
        }
        else {
          tree match {
            // case q"outwatch.dom.`package`.UsableDummyElement($x)" => q"$x" // replacement does not trigger typecheck of expression
            case q"TagContext.this.DummyElement.UsableElement($x).$fun" => q"$x.$fun" //TODO does this always work?
            case q"outwatch.dom.`package`.DummyElementUsableEvent[$_, $_]($x)" => q"$x"
            case _ => super.transform(tree)
          }
        }
      }
    }

    Transformer.transform(src)
  }

  def impl[Elem <: dom.Element : c.WeakTypeTag](c: Context)(newModifiers: c.Expr[VDomModifier]*): c.Expr[VTree[Elem]] = {
    import c.universe._

    val elemType = weakTypeOf[Elem]
    val tagContextName = TermName("_injected_tag_context_")
    val injectedMofifiers = newModifiers.map(expr => injectTagContext(c)(expr.tree, tagContextName))

    val tree = q"""
      ${c.prefix}.apply(($tagContextName: _root_.outwatch.dom.TagContext[$elemType]) => Seq(..${injectedMofifiers.toList}))
    """

    // println(s"TREE: $tree")

    val untypedTree = c.untypecheck(tree)
    c.Expr[VTree[Elem]](untypedTree)
  }
}
