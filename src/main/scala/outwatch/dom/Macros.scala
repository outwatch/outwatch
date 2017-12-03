package outwatch.dom

import org.scalajs.dom

import scala.annotation.compileTimeOnly

private[outwatch] object MacroMessages {
  val error = "Events can only be used in arguments of the VTree.apply method. Otherwise, you need to provide an implicit TagContext or use onElement explicitly (e.g. onClick.onElement[Element] --> sink)."
}

@compileTimeOnly("compile-time macro expansion")
private[outwatch] object Macros {
  import scala.reflect.macros.blackbox.Context

  private def injectTagContext[Elem <: dom.Element : c.WeakTypeTag](c: Context)(src: c.Tree, injectable: c.universe.TermName): c.Tree = {
    import c.universe._

    val elemType = weakTypeOf[Elem]
    val eventType = weakTypeOf[TypedCurrentTargetEvent[Elem]]
    val contextType = weakTypeOf[TagContext[Elem]]
    val withStringType = weakTypeOf[TagWithString[Elem]]
    val withNumberType = weakTypeOf[TagWithNumber[Elem]]
    val withBooleanType = weakTypeOf[TagWithBoolean[Elem]]

    object Transformer extends c.universe.Transformer {
      private def isReplaceType(tree: Tree): Boolean = tree.isType && !(tree.tpe =:= weakTypeOf[Nothing])

      override def transform(tree: c.Tree): c.Tree = {
        if (tree.tpe == null) tree
        else if (isReplaceType(tree) && tree.tpe =:= weakTypeOf[TagContext.DummyElement]) q"$elemType"
        else if (isReplaceType(tree) && tree.tpe <:< weakTypeOf[TypedCurrentTargetEvent[TagContext.DummyElement]]) q"$eventType"
        else {
          tree match {
            case q"dom.this.TagContext.DummyUnassignedTagContext" => q"$injectable"
            case q"dom.this.TagContext.AnyUnassignedTagContext[$_]" => q"$injectable"
            case q"outwatch.dom.`package`.DummyElementUsableEvent[$_, $_]($arg)" => q"$arg"
            case q"TagContext.this.DummyElement.DummyWithProperties($arg).$fun" => q"$arg.$fun"
            case q"TagContext.this.DummyElement.DummyWithString" => q"implicitly[$withStringType]"
            case q"TagContext.this.DummyElement.DummyWithNumber" => q"implicitly[$withNumberType]"
            case q"TagContext.this.DummyElement.DummyWithBoolean" => q"implicitly[$withBooleanType]"
            case _ => super.transform(tree)
          }
        }
      }
    }

    Transformer.transform(src)
  }

  def vtreeImpl[Elem <: dom.Element : c.WeakTypeTag](c: Context)(newModifiers: c.Expr[VDomModifier]*): c.Expr[VTree[Elem]] = {
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
