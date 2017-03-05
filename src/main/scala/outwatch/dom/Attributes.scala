package outwatch.dom

import outwatch.dom.helpers._

/**
  * Trait containing all attributes, so they can be mixed in to other objects if needed.
  */
trait Attributes {
  // General HTML5 atributes

  lazy val accept            = new AttributeBuilder[Any]("accept")
  lazy val acceptCharset     = new AttributeBuilder[Any]("accept-charset")
  lazy val action            = new AttributeBuilder[Any]("action")
  lazy val align             = new AttributeBuilder[Any]("align")
  lazy val alt               = new AttributeBuilder[String]("alt")
  lazy val autocomplete      = new AttributeBuilder[Any]("autocomplete")
  lazy val autofocus         = new BoolAttributeBuilder("autofocus")
  lazy val autoplay          = new BoolAttributeBuilder("autofocus")
  lazy val autosave          = new AttributeBuilder[Any]("autosave")
  lazy val blur              = new InputEventEmitterBuilder("blur")
  lazy val challenge         = new AttributeBuilder[Any]("challenge")
  lazy val change            = new InputEventEmitterBuilder("change")
  lazy val charset           = new AttributeBuilder[Any]("charset")
  lazy val checked           = BoolAttributeBuilder("checked")
  lazy val `class`           = new AttributeBuilder[String]("class")
  lazy val className         = `class`
  lazy val cls               = `class`
  lazy val click             = new MouseEventEmitterBuilder("click")
  lazy val cols              = new AttributeBuilder[Double]("cols")
  lazy val colspan           = new AttributeBuilder[Any]("colspan")
  lazy val contentEditable   = new BoolAttributeBuilder("contenteditable")
  lazy val contextMenu       = new MouseEventEmitterBuilder("contextmenu")
  lazy val controls          = new BoolAttributeBuilder("controls")
  lazy val coords            = new AttributeBuilder[Any]("coords")
  lazy val copy              = new ClipboardEventEmitterBuilder("copy")
  lazy val cut               = new ClipboardEventEmitterBuilder("cut")
  lazy val data              = new DynamicAttributeBuilder[Any](List("data"))
  lazy val datetime          = new AttributeBuilder[Any]("datetime")
  lazy val default           = new AttributeBuilder[Any]("default")
  lazy val dirname           = new AttributeBuilder[Any]("dirname")
  lazy val disabled          = BoolAttributeBuilder("disabled")
  lazy val download          = new AttributeBuilder[Any]("download")
  lazy val drag              = new DragEventEmitterBuilder("drag")
  lazy val dragEnd           = new DragEventEmitterBuilder("dragend")
  lazy val dragEnter         = new DragEventEmitterBuilder("dragenter")
  lazy val dragLeave         = new DragEventEmitterBuilder("dragleave")
  lazy val dragOver          = new DragEventEmitterBuilder("dragover")
  lazy val dragStart         = new DragEventEmitterBuilder("dragstart")
  lazy val draggable         = new AttributeBuilder[Any]("draggable")
  lazy val drop              = new DragEventEmitterBuilder("drop")
  lazy val dropzone          = new AttributeBuilder[Any]("dropzone")
  lazy val enctype           = new AttributeBuilder[Any]("enctype")
  lazy val `for`             = new AttributeBuilder[Any]("for")
  lazy val forLabel          = `for`
  lazy val formAction        = new AttributeBuilder[Any]("formaction")
  lazy val headers           = new AttributeBuilder[Any]("headers")
  lazy val hidden            = BoolAttributeBuilder("hidden")
  lazy val high              = new AttributeBuilder[Double]("high")
  lazy val href              = new AttributeBuilder[String]("href")
  lazy val icon              = new AttributeBuilder[Any]("icon")
  lazy val id                = new AttributeBuilder[String]("id")
  lazy val input             = new InputEventEmitterBuilder("input")
  @deprecated("Deprecated, use 'inputChecked' instead", "0.8.0")
  lazy val inputBool         = new BoolEventEmitterBuilder("change")
  lazy val inputChecked      = new BoolEventEmitterBuilder("change")
  lazy val inputNumber       = new NumberEventEmitterBuilder("input")
  lazy val inputString       = new StringEventEmitterBuilder("input")
  lazy val integrity         = new AttributeBuilder[Any]("integrity")
  lazy val isMap             = new BoolAttributeBuilder("ismap")
  lazy val itemProp          = new AttributeBuilder[Any]("itemprop")
  lazy val keyType           = new AttributeBuilder[Any]("keytype")
  lazy val keydown           = new KeyEventEmitterBuilder("keydown")
  lazy val keypress          = new KeyEventEmitterBuilder("keypress")
  lazy val keyup             = new KeyEventEmitterBuilder("keyup")
  lazy val kind              = new AttributeBuilder[Any]("kind")
  lazy val label             = new AttributeBuilder[Any]("label")
  lazy val lang              = new AttributeBuilder[Any]("lang")
  lazy val list              = new AttributeBuilder[Any]("list")
  lazy val loop              = new AttributeBuilder[Any]("loop")
  lazy val low               = new AttributeBuilder[Double]("low")
  lazy val max               = new AttributeBuilder[Double]("max")
  lazy val maxLength         = new AttributeBuilder[Int]("maxlength")
  lazy val media             = new AttributeBuilder[Any]("media")
  lazy val method            = new AttributeBuilder[Any]("method")
  lazy val min               = new AttributeBuilder[Double]("min")
  lazy val minLength         = new AttributeBuilder[Int]("minlength")
  lazy val mousedown         = new MouseEventEmitterBuilder("mousedown")
  lazy val mouseenter        = new MouseEventEmitterBuilder("mouseenter")
  lazy val mouseleave        = new MouseEventEmitterBuilder("mouseleave")
  lazy val mousemove         = new MouseEventEmitterBuilder("mousemove")
  lazy val mouseover         = new MouseEventEmitterBuilder("mouseover")
  lazy val multiple          = new AttributeBuilder[Any]("multiple")
  lazy val muted             = new BoolAttributeBuilder("muted")
  lazy val name              = new AttributeBuilder[String]("name")
  lazy val novalidate        = new BoolAttributeBuilder("novalidate")
  lazy val offline           = new EventEmitterBuilder("offline")
  lazy val online            = new EventEmitterBuilder("online")
  lazy val open              = new BoolAttributeBuilder("open")
  lazy val optimum           = new AttributeBuilder[Double]("open")
  lazy val paste             = new ClipboardEventEmitterBuilder("paste")
  lazy val pattern           = new AttributeBuilder[Any]("pattern")
  lazy val placeholder       = new AttributeBuilder[String]("placeholder")
  lazy val pointerLockChange = new MouseEventEmitterBuilder("pointerlockchange")
  lazy val pointerLockError  = new MouseEventEmitterBuilder("pointerlockerror")
  lazy val poster            = new AttributeBuilder[Any]("poster")
  lazy val preload           = new AttributeBuilder[Any]("preload")
  lazy val radiogroup        = new AttributeBuilder[Any]("radiogroup")
  lazy val readonly          = BoolAttributeBuilder("readonly")
  lazy val rel               = new AttributeBuilder[Any]("rel")
  lazy val required          = BoolAttributeBuilder("required")
  lazy val reset             = new EventEmitterBuilder("reset")
  lazy val resize            = new MouseEventEmitterBuilder("resize")
  lazy val reversed          = new AttributeBuilder[Any]("reversed")
  lazy val role              = new AttributeBuilder[String]("role")
  lazy val rows              = new AttributeBuilder[Double]("rows")
  lazy val rowspan           = new AttributeBuilder[Any]("rowspan")
  lazy val scope             = new AttributeBuilder[Any]("scope")
  lazy val select            = new MouseEventEmitterBuilder("select")
  lazy val selected          = BoolAttributeBuilder("selected")
  lazy val shape             = new AttributeBuilder[Any]("shape")
  lazy val size              = new AttributeBuilder[Int]("size")
  lazy val sizes             = new AttributeBuilder[Any]("sizes")
  lazy val spellCheck        = new AttributeBuilder[Any]("spellcheck")
  lazy val src               = new AttributeBuilder[Any]("src")
  lazy val srcset            = new AttributeBuilder[Any]("srcset")
  lazy val start             = new AttributeBuilder[Int]("start")
  lazy val step              = new AttributeBuilder[Double]("step")
  lazy val style             = new AttributeBuilder[Any]("style")
  lazy val submit            = new EventEmitterBuilder("submit")
  lazy val summary           = new AttributeBuilder[Any]("summary")
  lazy val tabindex          = new AttributeBuilder[Int]("tabindex")
  lazy val target            = new AttributeBuilder[Any]("target")
  lazy val title             = new AttributeBuilder[Any]("title")
  lazy val `type`            = new AttributeBuilder[Any]("type")
  lazy val tpe               = `type`
  lazy val inputType         = `type`
  lazy val usemap            = new AttributeBuilder[Any]("usemap")
  lazy val value             = new AttributeBuilder[Any]("value")
  lazy val wheel             = new MouseEventEmitterBuilder("wheel")
  lazy val wrap              = new AttributeBuilder[Any]("wrap")


  // Special OutWatch atributes

  /* A special attribute that takes a stream of single child nodes */
  lazy val child             = ChildStreamReceiverBuilder

  /* A special attribute that takes a stream of lists of child nodes */
  lazy val children          = ChildrenStreamReceiverBuilder


  /* Lifecycle hook for component insertion */
  lazy val insert            = InsertHookBuilder

  /* Lifecycle hook for component updates */
  lazy val update            = UpdateHookBuilder

  /* Lifecycle hook for component destruction */
  lazy val destroy           = DestroyHookBuilder
}
