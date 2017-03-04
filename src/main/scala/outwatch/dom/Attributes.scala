package outwatch.dom

import outwatch.dom.helpers._

/**
 * Trait containing the contents of the `Attributes` module, so they can be
 * mixed in to other objects if needed. This should contain "all" attributes
 * and mix in other traits (defined above) as needed to get full coverage.
 */
trait Attributes
  extends ClipBoardEventAttributes
  with FormEventAttributes
  with GlobalAttributes
  with InputAttributes
  with KeyboardEventAttributes
  with MediaAttributes
  with MiscellaneousAttributes
  with MouseEventAttributes
  with OutWatchChildAttributes
  with OutWatchLifeCycleAttributes
  with TableAttributes
  with WindowEventAttrs

/**
 * Clipboard Events
 */
trait ClipBoardEventAttributes {
  lazy val copy   = new ClipboardEventEmitterBuilder("copy")
  lazy val cut    = new ClipboardEventEmitterBuilder("cut")
  lazy val paste  = new ClipboardEventEmitterBuilder("paste")
}

/**
  * Form Events that are triggered by actions inside an HTML form. However,
  * these events apply to almost all HTML elements but are most commonly used
  * in form elements.
  */
trait FormEventAttributes {
  lazy val blur         = new InputEventEmitterBuilder("blur")
  lazy val change       = new InputEventEmitterBuilder("change")
  lazy val focus        = new InputEventEmitterBuilder("focus")
  lazy val input        = new InputEventEmitterBuilder("input")
  @deprecated("Deprecated, use 'inputChecked' instead", "0.8.0")
  lazy val inputBool    = new BoolEventEmitterBuilder("change")
  lazy val inputChecked = new BoolEventEmitterBuilder("change")
  lazy val inputNumber  = new NumberEventEmitterBuilder("input")
  lazy val inputString  = new StringEventEmitterBuilder("input")
  lazy val invalid      = new BoolEventEmitterBuilder("invalid")
  lazy val reset        = new EventEmitterBuilder("reset")
  lazy val search       = new EventEmitterBuilder("search")
  lazy val submit       = new EventEmitterBuilder("submit")
}

/**
  * Global attributes are attributes common to all HTML elements; they can be
  * used on all elements, though the attributes may have no effect on some
  * elements.
  *
  * Global attributes may be specified on all HTML elements, even those not
  * specified in the standard. That means that any non-standard elements must
  * still permit these attributes, even though using those elements means that
  * the document is no longer HTML5-compliant. For example, HTML5-compliant
  * browsers hide content marked as `<foo hidden>...<foo>`, even though `<foo>` is
  * not a valid HTML element.
  */
trait GlobalAttributes {
  lazy val accesskey        = new AttributeBuilder[String]("accesskey")
  lazy val `class`          = new AttributeBuilder[String]("class")
  lazy val className        = `class`
  lazy val cls              = `class`
  lazy val contentEditable  = new BoolAttributeBuilder("contenteditable")
  lazy val data             = new DynamicAttributeBuilder[Any](List("data"))
  lazy val dir              = new AttributeBuilder[Any]("dir")
  lazy val draggable        = new AttributeBuilder[Any]("draggable")
  lazy val dropzone         = new AttributeBuilder[Any]("dropzone")
  lazy val hidden           = BoolAttributeBuilder("hidden")
  lazy val id               = new AttributeBuilder[String]("id")
  lazy val itemId           = new AttributeBuilder[Any]("itemid")
  lazy val itemProp         = new AttributeBuilder[Any]("itemprop")
  lazy val itemref          = new AttributeBuilder[Any]("itemref")
  lazy val itemscope        = new AttributeBuilder[Any]("itemscope")
  lazy val itemtype         = new AttributeBuilder[Any]("itemtype")
  lazy val lang             = new AttributeBuilder[Any]("lang")
  lazy val slot             = new AttributeBuilder[Any]("slot")
  lazy val spellCheck       = new AttributeBuilder[Any]("spellcheck")
  lazy val style            = new AttributeBuilder[Any]("style")
  lazy val tabindex         = new AttributeBuilder[Int]("tabindex")
  lazy val title            = new AttributeBuilder[Any]("title")
  lazy val translate        = new AttributeBuilder[Any]("translate")

  /**
   * ARIA is a set of special accessibility attributes which can be added
   * to any markup, but is especially suited to HTML. The role attribute
   * defines what the general type of object is (such as an article, alert,
   * or slider). Additional ARIA attributes provide other useful properties,
   * such as a description for a form or the current value of a progressbar.
   *
   * MDN
   */
  object Aria {
    // TODO: Add all aria-* types
    //private def attr(name: String): BoolAttributeBuilder = BoolAttributeBuilder(s"aria-$name")
  }
}
/**
  * Attributes applicable only to the input element. This set is broken out
  * because it may be useful to identify the attributes of the input element
  * separately from other groupings. The attributes permitted by the input
  * element are likely the most complex of any element in HTML5.
  *
  * This group also contains some attributes that are applicable to the form
  * element itself.
  */
trait InputAttributes {
  lazy val accept         = new AttributeBuilder[Any]("accept")
  lazy val acceptCharset  = new AttributeBuilder[Any]("accept-charset")
  lazy val action         = new AttributeBuilder[Any]("action")
  lazy val autocomplete   = new AttributeBuilder[Any]("autocomplete")
  lazy val autofocus      = new BoolAttributeBuilder("autofocus")
  lazy val checked        = BoolAttributeBuilder("checked")
  lazy val cols           = new AttributeBuilder[Double]("cols")
  lazy val enctype        = new AttributeBuilder[Any]("enctype")
  lazy val `for`          = new AttributeBuilder[Any]("for")
  lazy val forLabel       = `for`
  lazy val form           = new AttributeBuilder[Any]("form")
  lazy val formaction     = new AttributeBuilder[Any]("formaction")
  lazy val formenctype    = new AttributeBuilder[Any]("formenctype")
  lazy val formmethod     = new AttributeBuilder[Any]("formmethod")
  lazy val formnovalidate = new AttributeBuilder[Any]("formnovalidate")
  lazy val formtarget     = new AttributeBuilder[Any]("formtarget")
  lazy val list           = new AttributeBuilder[Any]("list")
  lazy val max            = new AttributeBuilder[Double]("max")
  lazy val maxLength      = new AttributeBuilder[Int]("maxlength")
  lazy val method         = new AttributeBuilder[Any]("method")
  lazy val min            = new AttributeBuilder[Double]("min")
  lazy val minLength      = new AttributeBuilder[Int]("minlength")
  lazy val multiple       = new AttributeBuilder[Any]("multiple")
  lazy val name           = new AttributeBuilder[String]("name")
  lazy val novalidate     = new BoolAttributeBuilder("novalidate")
  lazy val pattern        = new AttributeBuilder[Any]("pattern")
  lazy val placeholder    = new AttributeBuilder[String]("placeholder")
  lazy val readonly       = BoolAttributeBuilder("readonly")
  lazy val required       = BoolAttributeBuilder("required")
  lazy val rows           = new AttributeBuilder[Double]("rows")
  lazy val selected       = BoolAttributeBuilder("selected")
  lazy val size           = new AttributeBuilder[Int]("size")
  lazy val step           = new AttributeBuilder[Double]("step")
  lazy val target         = new AttributeBuilder[Any]("target")
  lazy val value          = new AttributeBuilder[Any]("value")
  lazy val wrap           = new AttributeBuilder[Any]("wrap")

  object InputType {
    // TODO: Add all allowed input types
  }
}
/**
  * Keyboard Events - triggered by user actions on the keyboard or similar user
  * actions.
  */
trait KeyboardEventAttributes {
  lazy val keydown  = new KeyEventEmitterBuilder("keydown")
  lazy val keypress = new KeyEventEmitterBuilder("keypress")
  lazy val keyup    = new KeyEventEmitterBuilder("keyup")
}

/**
  * Attributes applicable to media elements like `<audio>`, `<video>` etc.
  */
trait MediaAttributes {
  lazy val autoplay     = new BoolAttributeBuilder("autoplay")
  lazy val buffered     = new BoolAttributeBuilder("buffered")
  lazy val crossorigin  = new AttributeBuilder[String]("crossorigin")
  lazy val controls     = new BoolAttributeBuilder("controls")
  lazy val default      = new BoolAttributeBuilder("default")
  lazy val kind         = new AttributeBuilder[Any]("kind")
  lazy val loop         = new BoolAttributeBuilder("loop")
  lazy val muted        = new BoolAttributeBuilder("muted")
  lazy val preload      = new AttributeBuilder[Any]("preload")
  lazy val poster       = new AttributeBuilder[Any]("poster")
  lazy val volume       = new AttributeBuilder[Float]("volume")
}

/**
 * Miscellaneous attributes that are applicable to one or more elements.
 */
trait MiscellaneousAttributes {
  lazy val align        = new AttributeBuilder[Any]("align")
  lazy val alt          = new AttributeBuilder[String]("alt")
  lazy val charset      = new AttributeBuilder[Any]("charset")
  lazy val content      = new AttributeBuilder[String]("content")
  lazy val coords       = new AttributeBuilder[Any]("coords")
  lazy val datetime     = new AttributeBuilder[Any]("datetime")
  lazy val disabled     = BoolAttributeBuilder("disabled")
  lazy val download     = new AttributeBuilder[Any]("download")
  lazy val height       = new AttributeBuilder[Double]("height")
  lazy val high         = new AttributeBuilder[Any]("high")
  lazy val href         = new AttributeBuilder[String]("href")
  lazy val httpEquiv    = new AttributeBuilder[String]("http-equiv")
  lazy val icon         = new AttributeBuilder[Any]("icon")
  lazy val integrity    = new AttributeBuilder[Any]("integrity")
  lazy val isMap        = new BoolAttributeBuilder("ismap")
  lazy val label        = new AttributeBuilder[Any]("label")
  lazy val low          = new AttributeBuilder[Double]("low")
  lazy val media        = new AttributeBuilder[Any]("media")
  lazy val open         = new BoolAttributeBuilder("open")
  lazy val optimum      = new AttributeBuilder[Double]("open")
  lazy val radiogroup   = new AttributeBuilder[Any]("radiogroup")
  lazy val rel          = new AttributeBuilder[Any]("rel")
  lazy val reversed     = new AttributeBuilder[Any]("reversed")
  lazy val role         = new AttributeBuilder[String]("role")
  lazy val scoped       = new AttributeBuilder[Any]("scoped")
  lazy val shape        = new AttributeBuilder[Any]("shape")
  lazy val sizes        = new AttributeBuilder[Any]("sizes")
  lazy val src          = new AttributeBuilder[Any]("src")
  lazy val srcset       = new AttributeBuilder[Any]("srcset")
  lazy val start        = new AttributeBuilder[Int]("start")
  lazy val `type`       = new AttributeBuilder[Any]("type")
  lazy val tpe          = `type`
  lazy val inputType    = `type`
  lazy val unselectable = new AttributeBuilder[Double]("unselectable")
  lazy val usemap       = new AttributeBuilder[Any]("usemap")
  lazy val width        = new AttributeBuilder[Double]("width")
  lazy val xmlns        = new AttributeBuilder[Any]("xmlns")

  @deprecated("The HTML keygen element, that this attribute belongs to has been deprecated in the HTML spec", "0.9.0")
  lazy val challenge         = new AttributeBuilder[Any]("challenge")
  @deprecated("The HTML keygen element, that this attribute belongs to has been deprecated in the HTML spec", "0.9.0")
  lazy val keyType           = new AttributeBuilder[Any]("keytype")
}

/**
  * Mouse Events: triggered by a mouse, or similar user actions.
  */
trait MouseEventAttributes {
  lazy val click              = new MouseEventEmitterBuilder("click")
  lazy val contextMenu        = new MouseEventEmitterBuilder("contextmenu")
  lazy val ondblclick         = new MouseEventEmitterBuilder("ondblclick")
  lazy val drag               = new DragEventEmitterBuilder("drag")
  lazy val dragEnd            = new DragEventEmitterBuilder("dragend")
  lazy val dragEnter          = new DragEventEmitterBuilder("dragenter")
  lazy val dragLeave          = new DragEventEmitterBuilder("dragleave")
  lazy val dragOver           = new DragEventEmitterBuilder("dragover")
  lazy val dragStart          = new DragEventEmitterBuilder("dragstart")
  lazy val drop               = new DragEventEmitterBuilder("drop")
  lazy val mousedown          = new MouseEventEmitterBuilder("mousedown")
  lazy val mouseenter         = new MouseEventEmitterBuilder("mouseenter")
  lazy val mouseleave         = new MouseEventEmitterBuilder("mouseleave")
  lazy val mousemove          = new MouseEventEmitterBuilder("mousemove")
  lazy val mouseover          = new MouseEventEmitterBuilder("mouseover")
  lazy val mouseout           = new MouseEventEmitterBuilder("mouseout")
  lazy val mouseup            = new MouseEventEmitterBuilder("mouseup")
  lazy val pointerLockChange  = new MouseEventEmitterBuilder("pointerlockchange")
  lazy val pointerLockError   = new MouseEventEmitterBuilder("pointerlockerror")
  lazy val resize             = new MouseEventEmitterBuilder("resize")
  lazy val scroll             = new MouseEventEmitterBuilder("scroll")
  lazy val select             = new MouseEventEmitterBuilder("select")

  // TODO: Create a WheelEventEmitterBuilder
  lazy val wheel              = new MouseEventEmitterBuilder("wheel")

}

/**
  * OutWatch specific attributes used to asign child nodes to a VNode
  */
trait OutWatchChildAttributes {
  /**
    * A special attribute that takes a stream of single child nodes
    */
  lazy val child    = ChildStreamReceiverBuilder

  /**
    * A special attribute that takes a stream of lists of child nodes
    */
  lazy val children = ChildrenStreamReceiverBuilder
}

/**
  * Outwatch component life cycle hooks
  */
trait OutWatchLifeCycleAttributes {
  /**
    * Lifecycle hook for component insertion
    */
  lazy val insert   = InsertHookBuilder

  /**
    * Lifecycle hook for component updates
    */
  lazy val update   = UpdateHookBuilder

  /**
    * Lifecycle hook for component destruction
    */
  lazy val destroy  = DestroyHookBuilder
}

/**
  * Attributes applicable to the table element and its children
  */
trait TableAttributes {
  lazy val colspan  = new AttributeBuilder[Any]("colspan")
  lazy val headers  = new AttributeBuilder[Any]("headers")
  lazy val rowspan  = new AttributeBuilder[Any]("rowspan")
  lazy val scope    = new AttributeBuilder[Any]("scope")
  lazy val summary  = new AttributeBuilder[Any]("summary")
}

/**
 * Window Events
 */
trait WindowEventAttrs {
  lazy val offline = new EventEmitterBuilder("offline")
  lazy val online  = new EventEmitterBuilder("online")
}
