// scalastyle:off file.size.limit

/** Documentation marked "MDN" is thanks to Mozilla Contributors
  * at https://developer.mozilla.org/en-US/docs/Web/API and available
  * under the Creative Commons Attribution-ShareAlike v2.5 or later.
  * http://creativecommons.org/licenses/by-sa/2.5/
  *
  * Other attribute documentation is thanks to Li Haoyi's Scalatags and is under
  * the MIT License.
  * http://opensource.org/licenses/MIT
  */
package outwatch.dom

import org.scalajs.dom._
import outwatch.dom.helpers._

/** Trait containing the contents of the `Attributes` module, so they can be
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
  with MediaEventAttributes
  with MiscellaneousAttributes
  with MiscellaneousEventAttributes
  with MouseEventAttributes
  with OutWatchChildAttributes
  with SnabbdomKeyAttributes
  with OutWatchLifeCycleAttributes
  with TableAttributes
  with TouchEventAttributes
  with WindowEventAttrs

/** Clipboard Events */
trait ClipBoardEventAttributes {
  /** Fires when the user copies the content of an element. */
  lazy val copy   = EmitterBuilder[ClipboardEvent]("copy")

  /** Fires when the user cuts the content of an element. */
  lazy val cut    = EmitterBuilder[ClipboardEvent]("cut")

  /** Fires when the user pastes some content in an element. */
  lazy val paste  = EmitterBuilder[ClipboardEvent]("paste")
}

/** Form Events that are triggered by actions inside an HTML form. However,
  * these events apply to almost all HTML elements but are most commonly used
  * in form elements.
  */
trait FormEventAttributes {
  /** The blur event is raised when an element loses focus.
    *
    * MDN
    */
  lazy val blur         = EmitterBuilder[InputEvent]("blur")

  /** The change event is fired for input, select, and textarea elements
    * when a change to the element's value is committed by the user.
    *
    * MDN
    */
  lazy val change       = EmitterBuilder[InputEvent]("change")

  /** The focus event is raised when the user sets focus on the given element.
    *
    * MDN
    */
  lazy val focus        = EmitterBuilder[InputEvent]("focus")

  /** The input event is fired when an element gets user input. */
  lazy val input        = EmitterBuilder[InputEvent]("input")


  @deprecated("Deprecated, use 'inputChecked' instead", "0.8.0")
  lazy val inputBool    = inputChecked


  /** The input event is fired when an element gets user input. */
  lazy val inputChecked = change.map(_.target.checked)


  /** The input event is fired when an element gets user input. */
  lazy val inputNumber  = input.map(_.target.valueAsNumber)


  /** The input event is fired when an element gets user input. */
  lazy val inputString  = input.map(_.target.value)

  /** This event is fired when an element becomes invalid. */
  lazy val invalid      = EmitterBuilder[Event]("invalid")

  /** The reset event is fired when a form is reset.
    *
    * MDN
    */
  lazy val reset        = EmitterBuilder[Event]("reset")

  /** Fires when the user writes something in a search field
    * (for `&lt;input="search"&gt;`).
    */
  lazy val search       = EmitterBuilder[Event]("search")

  /** The submit event is raised when the user clicks a submit button in a form
    * (`&lt;input type="submit"/&gt;`).
    *
    * MDN
    */
  lazy val submit       = EmitterBuilder[Event]("submit")
}

/** Global attributes are attributes common to all HTML elements; they can be
  * used on all elements, though the attributes may have no effect on some
  * elements.
  *
  * Global attributes may be specified on all HTML elements, even those not
  * specified in the standard. That means that any non-standard elements must
  * still permit these attributes, even though using those elements means that
  * the document is no longer HTML5-compliant. For example, HTML5-compliant
  * browsers hide content marked as `&lt;foo hidden&gt;...&lt;foo&gt;`, even though `&lt;foo&gt;` is
  * not a valid HTML element.
  *
  * MDN
  */
trait GlobalAttributes {
  /** Specifies a shortcut key to activate/focus an element. */
  lazy val accesskey        = new AttributeBuilder[String]("accesskey")

  /** This attribute is a space-separated list of the classes of the element.
    * Classes allows CSS and Javascript to select and access specific elements
    * via the class selectors or functions like the DOM method
    * document.getElementsByClassName. You can use cls as an alias for this
    * attribute so you don't have to backtick-escape this attribute.
    *
    * MDN
    */
  lazy val `class`          = new AttributeBuilder[String]("class")

  /** Shorthand for the `class` attribute. */
  lazy val className        = `class`

  /** Shorthand for the `class` attribute. */
  lazy val cls              = `class`

  /** Specifies whether the content of an element is editable or not. */
  lazy val contenteditable  = new BoolAttributeBuilder("contenteditable")
  @deprecated("Deprecated, use 'contenteditable' instead", "0.9.0")
  lazy val contentEditable  = contenteditable

  /** This class of attributes, called custom data attributes, allows proprietary
    * information to be exchanged between the HTML and its DOM representation that
    * may be used by scripts. All such custom data are available via the HTMLElement
    * interface of the element the attribute is set on. The HTMLElement.dataset
    * property gives access to them.
    *
    * The `*` in `data-*` may be replaced by any name following the production
    * rule of xml names with the following restrictions:
    *
    *   the name must not start with xml, whatever case is used for these letters;
    *   the name must not contain any semicolon (U+003A);
    *   the name must not contain capital A to Z letters.
    *
    * Note that the HTMLElement.dataset attribute is a StringMap and the name of
    * the custom data attribute data-test-value will be accessible via
    * HTMLElement.dataset.testValue as any dash (U+002D) is replaced by the
    * capitalization of the next letter (camelCase).
    *
    * MDN
    */
  lazy val data             = new DynamicAttributeBuilder[Any](List("data"))

  /** Specifies the text direction for the content in an element. The valid
    * values are:
    *
    *   `ltr`:  Default. Left-to-right text direction
    *
    *   `rtl`:  Right-to-left text direction
    *
    *   `auto`: Let the browser figure out the text direction, based on the
    *           content, (only recommended if the text direction is unknown)
    */
  lazy val dir              = new AttributeBuilder[Any]("dir")

  /** A Boolean attribute that specifies whether an element is draggable or not. */
  lazy val draggable        = new AttributeBuilder[Any]("draggable")

  /** Specifies whether the dragged data is copied, moved, or linked, when dropped. */
  lazy val dropzone         = new AttributeBuilder[Any]("dropzone")

  /** Specifies that an element is not yet, or is no longer, relevant and
    * consequently hidden from view of the user.
    */
  lazy val hidden           = new BoolAttributeBuilder("hidden")

  /** This attribute defines a unique identifier (ID) which must be unique in
    * the whole document. Its purpose is to identify the element when linking
    * (using a fragment identifier), scripting, or styling (with CSS).
    *
    * MDN
    */
  lazy val id               = new AttributeBuilder[String]("id")


  /** The itemid global attribute is the unique, global identifier of an item.
    * itemid attributes can only be specified on elements that have both
    * itemscope and itemtype attributes. Also, the itemid can only be specified
    * on elements with an itemscope attribute whose corresponding itemtype refers
    * to or defines a vocabulary that supports global identifiers.
    *
    * MDN
    */
  lazy val itemid           = new AttributeBuilder[Any]("itemid")
  @deprecated("Deprecated, use 'itemid' instead", "0.9.0")
  lazy val itemId           = itemid


  /** The itemprop global attribute is used to add properties to an item.
    * Every HTML element can have an itemprop attribute specified, and an
    * itemprop consists of a name-value pair. Each name-value pair is called a
    * property, and a group of one or more properties forms an item. Property
    * values are either a string or a URL and can be associated with a very wide
    * range of elements including `&lt;audio&gt;`, `&lt;embed&gt;`,
    * `&lt;iframe&gt;`, `&lt;img&gt;`, `&lt;link&gt;`, `&lt;object&gt;`,
    * `&lt;source&gt;`, `&lt;track&gt;`, and `&lt;video&gt;`.
    *
    * MDN
    */
  lazy val itemprop         = new AttributeBuilder[Any]("itemprop")
  @deprecated("Deprecated, use 'itemprop' instead", "0.9.0")
  lazy val itemProp         = itemprop


  /** The global attribute itemref Properties that are not descendants of an
    * element with the itemscope attribute can be associated with the item using
    * an itemref. itemref provides a list of element id's (not itemids) with
    * additional properties elsewhere in the document.
    * The itemref attribute can only be specified on elements that have an
    * itemscope attribute specified.
    *
    * MDN
    */
  lazy val itemref          = new AttributeBuilder[Any]("itemref")

  /** The global attribute itemscope (usually) works along with itemtype to
    * specify that the HTML contained in a block is about a particular item.
    * itemscope creates the Item and defines the scope of the itemtype
    * associated with it. itemtype is a valid URL of a vocabulary (such as
    * schema.org) that describes the item and its properties context. In the
    * examples below the vocabulary used is from schema.org. Every HTML element
    * may have an itemscope attribute specified. An itemscope element that
    * doesn't have an associated itemtype has an itemref.
    *
    * MDN
  */
  lazy val itemscope        = new AttributeBuilder[Any]("itemscope")

  /** The global attribute itemtype specifies the URL of the vocabulary that
    * will be used to define itemprop's (item properties) in the data structure.
    * itemscope is used to set the scope of  where in the data structure the
    * vocabulary set by itemtype will be active.
    *
    * MDN
    */
  lazy val itemtype         = new AttributeBuilder[Any]("itemtype")

  /** This attribute participates in defining the language of the element, the
    * language that non-editable elements are written in or the language that
    * editable elements should be written in. The tag contains one single entry
    * value in the format defines in the Tags for Identifying Languages (BCP47)
    * IETF document. If the tag content is the empty string the language is set
    * to unknown; if the tag content is not valid, regarding to BCP47, it is set
    * to invalid.
    *
    * MDN
    */
  lazy val lang             = new AttributeBuilder[Any]("lang")


  /** Assigns a slot in a shadow DOM shadow tree to an element: An element with
    * a slot attribute is assigned to the slot created by the `&lt;slot&gt;` element
    * whose name attribute's value matches that slot attribute's value.
    *
    * MDN
    */
  lazy val slot             = new AttributeBuilder[Any]("slot")

  /** This enumerated attribute defines whether the element may be checked for
    * spelling errors.
    *
    * MDN
    */
  lazy val spellcheck       = new AttributeBuilder[Any]("spellcheck")
  @deprecated("Deprecated, use 'spellcheck' instead", "0.9.0")
  lazy val spellCheck       = new AttributeBuilder[Any]("spellcheck")

  /** This attribute contains CSS styling declarations to be applied to the
    * element. Note that it is recommended for styles to be defined in a separate
    * file or files. This attribute and the style element have mainly the
    * purpose of allowing for quick styling, for example for testing purposes.
    *
    * MDN
    */
  lazy val style            = new AttributeBuilder[Any]("style")

  /** This integer attribute indicates if the element can take input focus (is
    * focusable), if it should participate to sequential keyboard navigation, and
    * if so, at what position. It can takes several values:
    *
    *   a negative value means that the element should be focusable, but should
    *   not be reachable via sequential keyboard navigation;
    *
    *   `0` means that the element should be focusable and reachable via sequential
    *   keyboard navigation, but its relative order is defined by the platform
    *   convention;
    *
    *   a positive value which means should be focusable and reachable via
    *   sequential keyboard navigation; its relative order is defined by the value
    *   of the attribute: the sequential follow the increasing number of the
    *   tabindex. If several elements share the same tabindex, their relative order
    *   follows their relative position in the document).
    *
    * An element with a `0` value, an invalid value, or no tabindex value should
    * be placed after elements with a positive tabindex in the sequential
    * keyboard navigation order.
    */
  lazy val tabindex         = new AttributeBuilder[Int]("tabindex")

  /** This attribute contains a text representing advisory information related to
    * the element it belongs too. Such information can typically, but not
    * necessarily, be presented to the user as a tooltip.
    *
    * MDN
    */
  lazy val title            = new AttributeBuilder[Any]("title")

  /** Specifies whether the content of an element should be translated or not. */
  lazy val translate        = new AttributeBuilder[Any]("translate")

  /** ARIA is a set of special accessibility attributes which can be added
    * to any markup, but is especially suited to HTML. The role attribute
    * defines what the general type of object is (such as an article, alert,
    * or slider). Additional ARIA attributes provide other useful properties,
    * such as a description for a form or the current value of a progressbar.
    *
    * MDN
    */
  object Aria {
    private def aria(name: String): BoolAttributeBuilder = new BoolAttributeBuilder(s"aria-$name")

    /** Identifies the currently active descendant of a composite widget. */
    lazy val activedescendant = aria("activedescendant")
    /** Indicates whether assistive technologies will present all, or only parts
      * of, the changed region based on the change notifications defined by the
      * aria-relevant attribute. See related aria-relevant.
      */
    lazy val atomic = aria("atomic")

    /** Indicates whether user input completion suggestions are provided. */
    lazy val autocomplete = aria("autocomplete")
    /** Indicates whether an element, and its subtree, are currently being updated. */
    lazy val busy = aria("busy")

    /** Indicates the current "checked" state of checkboxes, radio buttons, and
      * other widgets. See related aria-pressed and aria-selected.
      */
    lazy val checked = aria("checked")

    /** Identifies the element (or elements) whose contents or presence are
      * controlled by the current element. See related aria-owns.
      */
    lazy val controls = aria("controls")

    /** Identifies the element (or elements) that describes the object. See
      * related aria-labelledby.
      */
    lazy val describedby = aria("describedby")

    /** Indicates that the element is perceivable but disabled, so it is not
      * editable or otherwise operable. See related aria-hidden and aria-readonly.
      */
    lazy val disabled = aria("disabled")

    /** Indicates what functions can be performed when the dragged object is
      * released on the drop target. This allows assistive technologies to
      * convey the possible drag options available to users, including whether a
      * pop-up menu of choices is provided by the application. Typically, drop
      * effect functions can only be provided once an object has been grabbed
      * for a drag operation as the drop effect functions available are dependent
      * on the object being dragged.
      */
    lazy val dropeffect = aria("dropeffect")

    /** Indicates whether the element, or another grouping element it controls,
      * is currently expanded or collapsed.
      */
    lazy val expanded = aria("expanded")

    /** Identifies the next element (or elements) in an alternate reading order
      * of content which, at the user's discretion, allows assistive technology
      * to override the general default of reading in document source order.
      */
    lazy val flowto = aria("flowto")

    /** Indicates an element's "grabbed" state in a drag-and-drop operation. */
    lazy val grabbed = aria("grabbed")

    /** Indicates that the element has a popup context menu or sub-level menu. */
    lazy val haspopup = aria("haspopup")

    /** Indicates that the element and all of its descendants are not visible or
      * perceivable to any user as implemented by the author. See related aria-disabled.
      */
    lazy val hidden = aria("hidden")

    /** Indicates the entered value does not conform to the format expected by
      * the application.
      */
    lazy val invalid = aria("invalid")

    /** Defines a string value that labels the current element. See related
      * aria-labelledby.
      */
    lazy val label = aria("label")

    /** Identifies the element (or elements) that labels the current element.
      * See related aria-label and aria-describedby.
      */
    lazy val labelledby = aria("labelledby")

    /** Defines the hierarchical level of an element within a structure.
      */
    lazy val level = aria("level")

    /** Indicates that an element will be updated, and describes the types of
      * updates the user agents, assistive technologies, and user can expect
      * from the live region.
      */
    lazy val live = aria("live")

    /** Indicates whether a text box accepts multiple lines of input or only a
      * single line.
      */
    lazy val multiline = aria("multiline")

    /** Indicates that the user may select more than one item from the current
      * selectable descendants.
      */
    lazy val multiselectable = aria("multiselectable")

    /** Indicates whether the element and orientation is horizontal or vertical. */
    lazy val orientation = aria("orientation")

    /** Identifies an element (or elements) in order to define a visual,
      * functional, or contextual parent/child relationship between DOM elements
      * where the DOM hierarchy cannot be used to represent the relationship.
      * See related aria-controls.
      */
    lazy val owns = aria("owns")

    /** Defines an element's number or position in the current set of listitems
      * or treeitems. Not required if all elements in the set are present in the
      * DOM. See related aria-setsize.
      */
    lazy val posinset = aria("posinset")

    /** Indicates the current "pressed" state of toggle buttons. See related
      * aria-checked and aria-selected.
      */
    lazy val pressed = aria("pressed")

    /** Indicates that the element is not editable, but is otherwise operable.
      * See related aria-disabled.
      */
    lazy val readonly = aria("readonly")

    /** Indicates what user agent change notifications (additions, removals, etc.)
      * assistive technologies will receive within a live region. See related
      * aria-atomic.
      */
    lazy val relevant = aria("relevant")

    /** Indicates that user input is required on the element before a form may
      * be submitted.
      */
    lazy val required = aria("required")

    /** Indicates the current "selected" state of various widgets. See related
      * aria-checked and aria-pressed.
      */
    lazy val selected = aria("selected")

    /** Defines the number of items in the current set of listitems or
      * treeitems. Not required if all elements in the set are present in the
      * DOM. See related aria-posinset.
      */
    lazy val setsize = aria("setsize")

    /** Indicates if items in a table or grid are sorted in ascending or
      * descending order.
      */
    lazy val sort = aria("sort")

    /** Defines the maximum allowed value for a range widget. */
    lazy val valuemax = aria("valuemax")

    /** Defines the minimum allowed value for a range widget. */
    lazy val valuemin = aria("valuemin")

    /** Defines the current value for a range widget. See related aria-valuetext. */
    lazy val valuenow = aria("valuenow")

    /** Defines the human readable text alternative of aria-valuenow for a range
      * widget.
      */
    lazy val valuetext = aria("valuetext")
  }
}
/** Attributes applicable only to the input element. This set is broken out
  * because it may be useful to identify the attributes of the input element
  * separately from other groupings. The attributes permitted by the input
  * element are likely the most complex of any element in HTML5.
  *
  * This group also contains some attributes that are applicable to the form
  * element itself.
  */
trait InputAttributes {
  /** If the value of the type attribute is file, this attribute indicates the
    * types of files that the server accepts; otherwise it is ignored.
    *
    * MDN
    */
  lazy val accept         = new AttributeBuilder[Any]("accept")

  /** A space- or comma-delimited list of character encodings that the server
    * accepts. The browser uses them in the order in which they are listed. The
    * default value, the reserved string "UNKNOWN", indicates the same encoding
    * as that of the document containing the form element.
    * In previous versions of HTML, the different character encodings could be
    * delimited by spaces or commas. In HTML5, only spaces are allowed as delimiters.
    *
    * MDN
    */
  lazy val acceptCharset  = new AttributeBuilder[Any]("accept-charset")

  /** The URI of a program that processes the information submitted via the form.
    * This value can be overridden by a formaction attribute on a button or
    * input element.
    *
    * MDN
    */
  lazy val action         = new AttributeBuilder[Any]("action")

  /** This attribute indicates whether the value of the control can be
    * automatically completed by the browser. This attribute is ignored if the
    * value of the type attribute is hidden, checkbox, radio, file, or a button
    * type (button, submit, reset, image).
    *
    * Possible values are "off" and "on"
    *
    * MDN
    */
  lazy val autocomplete   = new AttributeBuilder[Any]("autocomplete")

  /** This Boolean attribute lets you specify that a form control should have
    * input focus when the page loads, unless the user overrides it, for example
    * by typing in a different control. Only one form element in a document can
    * have the autofocus attribute, which is a Boolean. It cannot be applied if
    * the type attribute is set to hidden (that is, you cannot automatically set
    * focus to a hidden control).
    *
    * MDN
    */
  lazy val autofocus      = new BoolAttributeBuilder("autofocus")

  /** When the value of the type attribute is radio or checkbox, the presence of
    * this Boolean attribute indicates that the control is selected by default;
    * otherwise it is ignored.
    *
    * MDN
    */
  lazy val checked        = new BoolAttributeBuilder("checked")

  /** The visible width of the text control, in average character widths. If it
    * is specified, it must be a positive integer. If it is not specified, the
    * default value is 20 (HTML5).
    *
    * MDN
    */
  lazy val cols           = new AttributeBuilder[Double]("cols")

  /** The `enctype` attribute provides the encoding type of the form when it is
    * submitted (for forms with a method of "POST").
    */
  lazy val enctype        = new AttributeBuilder[Any]("enctype")

  /** Describes elements which belongs to this one. Used on labels and output
    * elements.
    *
    * MDN
    */
  lazy val `for`          = new AttributeBuilder[Any]("for")

  /** Shorthand for the `for` attribute. */
  lazy val forLabel       = `for`

  /** The form attribute specifies one or more forms an `&lt;input&gt;` element
    * belongs to.
    */
  lazy val form           = new AttributeBuilder[Any]("form")

  /** The `formaction` attribute provides the URL that will process the input control
    * when the form is submitted and overrides the default `action` attribute of the
    * `form` element. This should be used only with `input` elements of `type`
    * submit or image.
  */
  lazy val formaction     = new AttributeBuilder[Any]("formaction")

  /** The `formenctype` attribute provides the encoding type of the form when it is
    * submitted (for forms with a method of "POST") and overrides the default
    * `enctype` attribute of the `form` element. This should be used only with the
    * `input` elements of `type` "submit" or "image"
    */
  lazy val formenctype    = new AttributeBuilder[Any]("formenctype")

  /** The `formmethod` attribute specifies the HTTP Method the form should use when
    * it is submitted and overrides the default `method` attribute of the `form`
    * element. This should be used only with the `input` elements of `type` "submit"
    * or "image".
    */
  lazy val formmethod     = new AttributeBuilder[Any]("formmethod")

  /** The `formnovalidate` Boolean attribute specifies that the input of the form
    * should not be validated upon submit and overrides the default `novalidate`
    * attribute of the `form`. This should only be used with `input` elements of
    * of `type` "submit".
    */
  lazy val formnovalidate = new AttributeBuilder[Any]("formnovalidate")

  /** The `formtarget` provides a name or keyword that indicates where to display
    * the response that is received after submitting the form and overrides the
    * `target` attribute of them `form` element. This should only be used with
    * the `input` elements of `type` "submit" or "image"
    */
  lazy val formtarget     = new AttributeBuilder[Any]("formtarget")

  /** The type of control to display. The default type is text, if this
    * attribute is not specified.
    */
  lazy val inputType      = new AttributeBuilder[Any]("type")

  /** The list attribute refers to a `&lt;datalist&gt;` element that contains
    * the options for an input element the presents a select list.
    */
  lazy val list           = new AttributeBuilder[Any]("list")

  /** The max attribute specifies the maximum value for an `&lt;input&gt;`
    * element of type number, range, date, datetime, datetime-local, month,
    * time, or week.
    */
  lazy val max            = new AttributeBuilder[Double]("max")

  /** The maximum allowed length for the input field. This attribute forces the
    * input control to accept no more than the allowed number of characters.
    * It does not produce any feedback to the user; you must write Javascript to
    * make that happen.
    */
  lazy val maxlength      = new AttributeBuilder[Int]("maxlength")
  @deprecated("Deprecated, use 'maxlength' instead", "0.9.0")
  lazy val maxLength      = maxlength

  /** The HTTP method that the browser uses to submit the form. Possible values are:
    *
    *   post: Corresponds to the HTTP POST method ; form data are included in
    *         the body of the form and sent to the server.
    *
    *   get:  Corresponds to the HTTP GET method; form data are appended to the
    *         action attribute URI with a '?' as a separator, and the resulting
    *         URI is sent to the server. Use this method when the form has no
    *         side-effects and contains only ASCII characters.
    *
    * This value can be overridden by a formmethod attribute on a button or
    * input element.
    *
    * MDN
    */
  lazy val method         = new AttributeBuilder[Any]("method")

  /** The min attribute specifies the minimum value for an `&lt;input&gt;`
    * element of type number, range, date, datetime, datetime-local, month, time, or week.
    */
  lazy val min            = new AttributeBuilder[Double]("min")

  /** The minimum allowed length for the input field. This attribute forces the
    * input control to accept no less than the allowed number of characters.
    * It does not produce any feedback to the user; you must write Javascript to
    * make that happen.
    */
  lazy val minlength      = new AttributeBuilder[Int]("minlength")
  @deprecated("Deprecated, use 'minlength' instead", "0.9.0")
  lazy val minLength      = minlength

  /** This Boolean attribute specifies, when present/true, that the user is
    * allowed to enter more than one value for the `&lt;input&gt;` element for
    * types "email" or "file". It can also be provided to the `&lt;select&gt;`
    * element to allow selecting more than one option.
    */
  lazy val multiple       = new AttributeBuilder[Any]("multiple")

  /** On form elements (input etc.):
    *   Name of the element. For example used by the server to identify the fields
    *   in form submits.
    *
    * On the meta tag:
    *   This attribute defines the name of a document-level metadata.
    *   This document-level metadata name is associated with a value, contained by
    *   the content attribute.
    *
    * MDN
    */
  lazy val name           = new AttributeBuilder[String]("name")


  /** This Boolean attribute indicates that the form is not to be validated when
    * submitted. If this attribute is not specified (and therefore the form is
    * validated), this default setting can be overridden by a formnovalidate
    * attribute on a <button> or <input> element belonging to the form.
    *
    * MDN HTML5
    */
  lazy val novalidate     = new BoolAttributeBuilder("novalidate")

  /** Specifies a regular expression to validate the input. The pattern attribute
    * works with the following input types: text, search, url, tel, email, and
    * password. Use the `title` attribute to describe the pattern to the user.
    */
  lazy val pattern        = new AttributeBuilder[Any]("pattern")

  /** A hint to the user of what can be entered in the control. The placeholder
    * text must not contain carriage returns or line-feeds. This attribute
    * applies when the value of the type attribute is text, search, tel, url or
    * email; otherwise it is ignored.
    *
    * MDN
    */
  lazy val placeholder    = new AttributeBuilder[String]("placeholder")

  /** This Boolean attribute indicates that the user cannot modify the value of
    * the control. This attribute is ignored if the value of the type attribute
    * is hidden, range, color, checkbox, radio, file, or a button type.
    *
    * MDN
    */
  lazy val readonly       = new BoolAttributeBuilder("readonly")

  /** This attribute specifies that the user must fill in a value before
    * submitting a form. It cannot be used when the type attribute is hidden,
    * image, or a button type (submit, reset, or button). The :optional and
    * :required CSS pseudo-classes will be applied to the field as appropriate.
    *
    * MDN
    */
  lazy val required       = new BoolAttributeBuilder("required")

  /** The number of visible text lines for the control.
    *
    * MDN
    */
  lazy val rows           = new AttributeBuilder[Double]("rows")

  /** If present, this Boolean attribute indicates that the option is initially
    * selected. If the `&lt;option&gt;` element is the descendant of a
    * `&lt;select&gt;` element whose multiple attribute is not set, only one
    * single `&lt;option&gt;` of this `&lt;select&gt;` element may have the
    * `selected` attribute.
    *
    * MDN
    */
  lazy val selected       = new BoolAttributeBuilder("selected")

  /** The initial size of the control. This value is in pixels unless the value
    * of the type attribute is text or password, in which case, it is an integer
    * number of characters. Starting in HTML5, this attribute applies only when
    * the type attribute is set to text, search, tel, url, email, or password;
    * otherwise it is ignored. In addition, the size must be greater than zero.
    * If you don't specify a size, a default value of 20 is used.
    *
    * MDN
    */
  lazy val size           = new AttributeBuilder[Int]("size")

  /** The step attribute specifies the numeric intervals for an `&lt;input&gt;` element
    * that should be considered legal for the input. For example, if step is `2`
    * on a number typed `&lt;input&gt;` then the legal numbers could be `-2`, `0`,
    * `2`, `4`, `6` etc. The step attribute should be used in conjunction with
    * the `min` and `max` attributes to specify the full range and interval of
    * the legal values. The step attribute is applicable to `&lt;input&gt;` elements
    * of the following types: `number`, `range`, `date`, `datetime`,
    * `datetime-local`, `month`, `time` and `week`.
    */
  lazy val step           = new AttributeBuilder[Double]("step")

  /** A name or keyword indicating where to display the response that is received
    * after submitting the form. In HTML 4, this is the name of, or a keyword
    * for, a frame. In HTML5, it is a name of, or keyword for, a browsing context
    * (for example, tab, window, or inline frame). The following keywords have
    * special meanings:
    *
    *   _self:      Load the response into the same HTML 4 frame (or HTML5
    *               browsing context) as the current one. This value is the
    *               default if the attribute is not specified.
    *
    *   _blank:     Load the response into a new unnamed HTML 4 window or HTML5
    *               browsing context.
    *
    *   _parent:    Load the response into the HTML 4 frameset parent of the
    *               current frame or HTML5 parent browsing context of the current
    *               one. If there is no parent, this option behaves the same way
    *               as _self.
    *
    *   _top:       HTML 4: Load the response into the full, original window,
    *               canceling all other frames. HTML5: Load the response into the
    *               top-level browsing context (that is, the browsing context that
    *               is an ancestor of the current one, and has no parent). If there
    *               is no parent, this option behaves the same way as _self.
    *
    *  iframename:  The response is displayed in a named iframe.
    */
  lazy val target         = new AttributeBuilder[Any]("target")

  /** The initial value of the control. This attribute is optional except when
    * the value of the type attribute is radio or checkbox.
    *
    * MDN
    */
  lazy val value          = new AttributeBuilder[Any]("value")

  /** Indicates how the control wraps text. Possible values are:
    *   hard: The browser automatically inserts line breaks (CR+LF) so that each
    *         line has no more than the width of the control; the cols attribute
    *         must be specified.
    *
    *   soft: The browser ensures that all line breaks in the value consist of a
    *         CR+LF pair, but does not insert any additional line breaks.
    *
    *   off:  Like soft but changes appearance to white-space: pre so line
    *         segments exceeding cols are not wrapped and area becomes
    *         horizontally scrollable.
    *
    * If this attribute is not specified, soft is its default value.
    *
    * MDN
    */
  lazy val wrap           = new AttributeBuilder[Any]("wrap")

  /** The type of control to display. The default type is text, if this
    * attribute is not specified.
    *
    * MDN
    */
  object InputType {

    /** A push button with no default behavior.
      *
      * MDN
      */
    lazy val button         = inputType := "button"

    /** A check box. You must use the value attribute to define the value
      * submitted by this item. Use the checked attribute to indicate whether
      * this item is selected. You can also use the indeterminate attribute
      * (which can only be set programmatically) to indicate that the checkbox
      * is in an indeterminate state (on most platforms, this draws a horizontal
      * line across the checkbox).
      *
      * MDN
      */
    lazy val checkbox       = inputType := "checkbox"

    /** A control for specifying a color. A color picker's UI has no
      * required features other than accepting simple colors as text.
      *
      * MDN HTML5
      */
    lazy val color          = inputType := "color"

    /** A control for entering a date (year, month, and day, with no time).
      *
      * MDN HTML5
      */
    lazy val date           = inputType := "date"

    /** A control for entering a date and time (hour, minute, second, and
      * fraction of a second) based on UTC time zone. This feature has been
      * removed from WHATWG HTML.
      *
      * MDN HTML5
      */
    lazy val datetime       = inputType := "datetime"

    /** A control for entering a date and time, with no time zone.
      *
      * MDN HTML5
      */
    lazy val datetimeLocal  = inputType := "datetime-local"

    /** A field for editing an e-mail address. The input value is validated to
      * contain either the empty string or a single valid e-mail address before
      * submitting. The :valid and :invalid CSS pseudo-classes are applied as
      * appropriate.
      *
      * MDN HTML5
      */
    lazy val email          = inputType := "email"

    /** A control that lets the user select a file. Use the accept attribute to
      * define the types of files that the control can select.
      *
      * MDN
      */
    lazy val file           = inputType := "file"

    /** A control that is not displayed but whose value is submitted to the server.
      */
    lazy val hidden         = inputType := "hidden"

    /** A graphical submit button. You must use the src attribute to define the
      * source of the image and the alt attribute to define alternative text.
      * You can use the height and width attributes to define the size of the
      * image in pixels.
      *
      * MDN
      */
    lazy val image          = inputType := "image"

    /** A control for entering a month and year, with no time zone.
      *
      * MDN HTML5
      */
    lazy val month          = inputType := "month"

    /** A control for entering a floating point number.
      *
      * MDN HTML5
      */
    lazy val number         = inputType := "number"

    /** A single-line text field whose value is obscured. Use the maxlength
      * attribute to specify the maximum length of the value that can be entered.
      *
      * MDN
      */
    lazy val password       = inputType := "password"

    /** A radio button. You must use the value attribute to define the value
      * submitted by this item. Use the checked attribute to indicate whether
      * this item is selected by default. Radio buttons that have the same value
      * for the name attribute are in the same "radio button group". Only one
      * radio button in a group can be selected at a time.
      *
      * MDN
      */
    lazy val radio          = inputType := "radio"

    /** A control for entering a number whose exact value is not important.
      * This type control uses the following default values if the corresponding
      * attributes are not specified:
      *   min:    0
      *   max:    100
      *   value:  min + (max - min) / 2, or min if max is less than min
      *   step:   1
      *
      * MDN HTML5
      */
    lazy val range          = inputType := "range"

    /** A button that resets the contents of the form to default values.
      *
      * MDN
      */
    lazy val reset          = inputType := "reset"

    /** A single-line text field for entering search strings. Line-breaks are
      * automatically removed from the input value.
      *
      * MDN HTML5
      */
    lazy val search         = inputType := "search"

    /** A button that submits the form.
      *
      * MDN
      */
    lazy val submit         = inputType := "submit"

    /** A control for entering a telephone number. Line-breaks are automatically
      * removed from the input value, but no other syntax is enforced. You can
      * use attributes such as pattern and maxlength to restrict values entered
      * in the control. The :valid and :invalid CSS pseudo-classes are applied
      * as appropriate.
      *
      * MDN HTML5
      */
    lazy val tel            = inputType := "tel"

    /** A single-line text field. Line-breaks are automatically removed from the
      * input value.
      *
      * MDN
      */
    lazy val text           = inputType := "text"

    /** A control for entering a time value with no time zone.
      *
      * MDN HTML5
      */
    lazy val time           = inputType := "time"

    /** A field for editing a URL. The input value is validated to contain
      * either the empty string or a valid absolute URL before submitting.
      * You can use attributes such as pattern and maxlength to restrict values
      * entered in the control. The :valid and :invalid CSS pseudo-classes are
      * applied as appropriate.
      *
      * MDN HTML5
      */
    lazy val url            = inputType := "url"

    /** A control for entering a date consisting of a week-year number and a
      * week number with no time zone.
      *
      * MDN HTML5
      */
    lazy val week           = inputType := "week"
  }
}
/** Keyboard Events - triggered by user actions on the keyboard or similar user
  * actions.
  */
trait KeyboardEventAttributes {
  /** The keydown event is raised when the user presses a keyboard key.
    *
    * MDN
    */
  lazy val keydown  = EmitterBuilder[KeyboardEvent]("keydown")

  /** The keypress event should be raised when the user presses a key on the
    * keyboard. However, not all browsers fire keypress events for certain keys.
    *
    * Webkit-based browsers (Google Chrome and Safari, for example) do not fire
    * keypress events on the arrow keys. Firefox does not fire keypress events
    * on modifier keys like SHIFT.
    *
    * MDN
    */
  lazy val keypress = EmitterBuilder[KeyboardEvent]("keypress")

  /** The keyup event is raised when the user releases a key that's been pressed.
    *
    * MDN
    */
  lazy val keyup    = EmitterBuilder[KeyboardEvent]("keyup")
}

/** Attributes applicable to media elements like `&lt;audio&gt;`,
  * `&lt;video&gt;` etc.
  */
trait MediaAttributes extends SharedEventAttributes {
  /** A Boolean attribute; if specified, the video automatically begins to play
    * back as soon as it can do so without stopping to finish loading the data.
    *
    * MDN
    */
  lazy val autoplay     = new BoolAttributeBuilder("autoplay")

  /** An attribute you can read to determine the time ranges of the buffered media.
    * This attribute contains a TimeRanges object.
    *
    * MDN
    */
  lazy val buffered     = new BoolAttributeBuilder("buffered")

  /** This enumerated attribute indicates whether to use CORS to fetch the
    * related image. CORS-enabled resources can be reused in the `&lt;canvas&gt;`
    * element without being tainted. The allowed values are:
    *
    *   anonymous:        Sends a cross-origin request without a credential. In
    *                     other words, it sends the Origin: HTTP header without
    *                     a cookie, X.509 certificate, or performing HTTP Basic
    *                     authentication.
    *                     If the server does not give credentials to the origin
    *                     site (by not setting the Access-Control-Allow-Origin:
    *                     HTTP header), the image will be tainted, and its usage
    *                     restricted.
    *
    *   use-credentials:  Sends a cross-origin request with a credential. In
    *                     other words, it sends the Origin: HTTP header with a
    *                     cookie, a certificate, or performing HTTP Basic
    *                     authentication. If the server does not give credentials
    *                     to the origin site (through
    *                     Access-Control-Allow-Credentials: HTTP header), the
    *                     image will be tainted and its usage restricted.
    *
    *  When not present, the resource is fetched without a CORS request (i.e.
    *  without sending the Origin: HTTP header), preventing its non-tainted used
    *  in `&lt;canvas&gt;` elements. If invalid, it is handled as if the enumerated
    *  keyword anonymous was used. See CORS settings attributes for additional
    *  information.
    *
    * MDN
    */
  lazy val crossorigin  = new AttributeBuilder[String]("crossorigin")

  /** If this attribute is present, the browser will offer controls to allow the
    * user to control video playback, including volume, seeking, and pause/resume
    * playback.
    *
    * MDN
    */
  lazy val controls     = new BoolAttributeBuilder("controls")

  /** This attribute indicates that the track should be enabled unless the user's
    * preferences indicate that another track is more appropriate. This may only
    * be used on one track element per media element.
    *
    * MDN
    */
  lazy val default      = new BoolAttributeBuilder("default")

  /** How the text track is meant to be used. If omitted the default kind is
    * subtitles. If the attribute is not present, it will use the subtitles.
    * If the attribute contains an invalid value, it will use metadata.
    * (Versions of Chrome earlier than 52 treated an invalid value as subtitles.)
    *
    * The following keywords are allowed:
    *
    *   subtitles:    Subtitles provide translation of content that cannot be
    *                 understood by the viewer. For example dialogue or text
    *                 that is not English in an English language film.
    *                 Subtitles may contain additional content, usually extra
    *                 background information. For example the text at the
    *                 beginning of the Star Wars films, or the date, time, and
    *                 location of a scene.
    *
    *   captions:     Closed captions provide a transcription and possibly a
    *                 translation of audio.
    *                 It may include important non-verbal information such as
    *                 music cues or sound effects. It may indicate the cue's
    *                 source (e.g. music, text, character).
    *                 Suitable for users who are deaf or when the sound is muted.
    *
    *   descriptions: Textual description of the video content.
    *                 Suitable for users who are blind or where the video cannot
    *                 be seen.
    *
    *   chapters:     Chapter titles are intended to be used when the user is
    *                 navigating the media resource.
    *
    *   metadata:     Tracks used by scripts. Not visible to the user.
    *
    */
  lazy val kind         = new AttributeBuilder[Any]("kind")

  /** A user-readable title of the text track which is used by the browser when
    * listing available text tracks.
    *
    * MDN
    */
  lazy val label        = new AttributeBuilder[Any]("label")

  /** A Boolean attribute; if specified, we will, upon reaching the end of the
    * video, automatically seek back to the start.
    *
    * MDN
    */
  lazy val loop         = new BoolAttributeBuilder("loop")


  /** A Boolean attribute which indicates the default setting of the audio
    * contained in the video. If set, the audio will be initially silenced. Its
    * default value is false, meaning that the audio will be played when the
    * video is played.
    *
    * MDN
    */
  lazy val muted        = new BoolAttributeBuilder("muted")

  /** This enumerated attribute is intended to provide a hint to the browser
    * about what the author thinks will lead to the best user experience. It may
    * have one of the following values:
    *
    *   none:             indicates that the video should not be preloaded.
    *
    *   metadata:         indicates that only video metadata (e.g. length) is
    *                     fetched.
    *
    *   auto:             indicates that the whole video file could be downloaded,
    *                     even if the user is not expected to use it.
    *
    *   the empty string: synonym of the auto value.
    *
    * If not set, its default value is browser-defined (i.e. each browser may
    * have its default value). The spec advises it to be set to metadata.
    *
    * MDN
    */
  lazy val preload      = new AttributeBuilder[Any]("preload")

  /** A URL indicating a poster frame to show until the user plays or seeks. If
    * this attribute isn't specified, nothing is displayed until the first frame
    * is available; then the first frame is shown as the poster frame.
    *
    * MDN
    */
  lazy val poster       = new AttributeBuilder[Any]("poster")

  /** The playback volume, in the range `0.0` (silent) to `1.0` (loudest). */
  lazy val volume       = new AttributeBuilder[Float]("volume")
}

/** Media Events - triggered by media like videos, images and audio. These apply to
 * all HTML elements, but they are most common in media elements, like `&lt;audio&gt;`,
 * `&lt;embed&gt;`, `&lt;img&gt;`, `&lt;object&gt;`, and `&lt;video&gt;`.
 */
trait MediaEventAttributes {
  private def event(name: String) = EmitterBuilder[Event](name)

  /** Script to be run on abort. */
  lazy val abort = event("abort")

  /** Script to be run when a file is ready to start playing (when it has
    * buffered enough to begin).
    */
  lazy val canplay = event("canplay")

  /** Script to be run when a file can be played all the way to the end without
    * pausing for buffering.
    */
  lazy val canplaythrough = event("canplaythrough")

  /** Script to be run when the cue changes in a `&lt;track&gt;` element. */
  lazy val cuechange = event("cuechange")

  /** Script to be run when the length of the media changes. */
  lazy val durationchange = event("durationchange")

  /** Script to be run when something bad happens and the file is suddenly
    * unavailable (like unexpectedly disconnects).
    */
  lazy val emptied = event("emptied")

  /** Script to be run when the media has reach the end (a useful event for
    * messages like "thanks for listening").
    */
  lazy val ended = event("ended")

  /** Script to be run when media data is loaded. */
  lazy val loadeddata = event("loadeddata")

  /** Script to be run when meta data (like dimensions and duration) are loaded. */
  lazy val loadedmetadata = event("loadedmetadata")

  /** Script to be run just as the file begins to load before anything is
    * actually loaded.
    */
  lazy val loadstart = event("loadstart")

  /** Script to be run when the media is paused either by the user or
    * programmatically.
    */
  lazy val pause = event("pause")

  /** Script to be run when the media is ready to start playing.
    */
  lazy val play = event("play")

  /** Script to be run when the media actually has started playing.
    */
  lazy val playing = event("playing")

  /** Script to be run when the browser is in the process of getting the media
    * data.
    */
  lazy val progress = event("progress")

  /** Script to be run each time the playback rate changes (like when a user
    * switches to a slow motion or fast forward mode).
    */
  lazy val ratechange = event("ratechange")

  /** Script to be run when the seeking attribute is set to false indicating
    * that seeking has ended.
    */
  lazy val seeked = event("seeked")

  /** Script to be run when the seeking attribute is set to true indicating that
    * seeking is active.
    */
  lazy val seeking = event("seeking")

  /** Script to be run when the browser is unable to fetch the media data for
    * whatever reason.
    */
  lazy val stalled = event("stalled")

  /** Script to be run when fetching the media data is stopped before it is
    * completely loaded for whatever reason.
    */
  lazy val suspend = event("suspend")

  /** Script to be run when the playing position has changed (like when the user
    * fast forwards to a different point in the media).
    */
  lazy val timeupdate = event("timeupdate")

  /** Script to be run each time the volume is changed which (includes setting
    * the volume to "mute").
    */
  lazy val volumechange = event("volumechange")

  /** Script to be run when the media has paused but is expected to resume
    * (like when the media pauses to buffer more data).
    */
  lazy val waiting = event("waiting")
}

/** Miscellaneous attributes that are applicable to one or more elements.
 */
trait MiscellaneousAttributes {
  /** Specifies the horizontal alignment of the element.
    * Mostly obsolete since HTML5
    *
    * MDN
    */
  lazy val align        = new AttributeBuilder[Any]("align")

  /** This attribute defines the alternative text describing the image. Users
    * will see this displayed if the image URL is wrong, the image is not in one
    * of the supported formats, or until the image is downloaded.
    *
    * MDN
    */
  lazy val alt          = new AttributeBuilder[String]("alt")

  /** Declares the character encoding of the page or script. Used on meta and
    * script elements.
    *
    * MDN
    */
  lazy val charset      = new AttributeBuilder[Any]("charset")

  /** This attribute gives the value associated with the http-equiv or name
    * attribute, depending of the context.
    *
    * MDN
    */
  lazy val content      = new AttributeBuilder[String]("content")

  /** A set of values specifying the coordinates of the hot-spot region.
    * The number and meaning of the values depend upon the value specified for
    * the shape attribute. For a rect or rectangle shape, the coords value is
    * two x,y pairs: left, top, right, and bottom. For a circle shape, the value
    * is x,y,r where x,y is a pair specifying the center of the circle and r is
    * a value for the radius. For a poly or polygon shape, the value is a set of
    * x,y pairs for each point in the polygon: x1,y1,x2,y2,x3,y3, and so on. In
    * HTML4, the values are numbers of pixels or percentages, if a percent sign
    * (%) is appended; in HTML5, the values are numbers of CSS pixels.
    *
    * MDN
    */
  lazy val coords       = new AttributeBuilder[Any]("coords")


  /** This attribute indicates the time and date of the element and must be a
    * valid date with an optional time string. If the value cannot be parsed as
    * a date with an optional time string, the element does not have an
    * associated time stamp.
    *
    * MDN
    */
  lazy val datetime     = new AttributeBuilder[Any]("datetime")

  /** This Boolean attribute indicates that the form control is not available for
    * interaction. In particular, the click event will not be dispatched on
    * disabled controls. Also, a disabled control's value isn't submitted with
    * the form.
    *
    * This attribute is ignored if the value of the type attribute is hidden.
    *
    * MDN
    */
  lazy val disabled     = new BoolAttributeBuilder("disabled")

  /** This attribute instructs browsers to download a URL instead of navigating
    * to it, so the user will be prompted to save it as a local file. If the
    * attribute has a value, it is used as the pre-filled file name in the Save
    * prompt (the user can still change the file name if they want). There are
    * no restrictions on allowed values, though / and \ are converted to
    * underscores. Most file systems limit some punctuation in file names, and
    * browsers will adjust the suggested name accordingly.
    *
    * MDN HTML5
    */
  lazy val download     = new AttributeBuilder[Any]("download")

  /** The `height` attribute specifies the height of an `input` element of
    * `type` "image".
    */
  lazy val height       = new AttributeBuilder[Double]("height")

  /** For use in &lt;meter&gt; tags.
    *
    * @see https://css-tricks.com/html5-meter-element/
    */
  lazy val high         = new AttributeBuilder[Any]("high")

  /** This is the single required attribute for anchors defining a hypertext
    * source link. It indicates the link target, either a URL or a URL fragment.
    * A URL fragment is a name preceded by a hash mark (#), which specifies an
    * internal target location (an ID) within the current document. URLs are not
    * restricted to Web (HTTP)-based documents. URLs might use any protocol
    * supported by the browser. For example, file, ftp, and mailto work in most
    * user agents.
    *
    * MDN
    */
  lazy val href         = new AttributeBuilder[String]("href")

  /** This enumerated attribute defines the pragma that can alter servers and
    * user-agents behavior. The value of the pragma is defined using the content
    * attribute and can be one of the following:
    *
    *   content-language
    *   content-type
    *   default-style
    *   refresh
    *   set-cookie
    *
    * MDN
    */
  lazy val httpEquiv    = new AttributeBuilder[String]("http-equiv")

  /** Image URL, used to provide a picture to represent the command represented
    * by a `&lt;menuitem&gt;`.
    *
    * MDN
    */
  lazy val icon         = new AttributeBuilder[Any]("icon")


  /** Contains inline metadata, a base64-encoded cryptographic hash of a resource
    * (file) you’re telling the browser to fetch, that a user agent can use to
    * verify that a fetched resource has been delivered free of unexpected
    * manipulation.
    *
    * @see https://developer.mozilla.org/en-US/docs/Web/Security/Subresource_Integrity
    *
    * MDN
    */
  lazy val integrity    = new AttributeBuilder[Any]("integrity")

  /** This Boolean attribute indicates that the image is part of a server-side
    * map. If so, the precise coordinates of a click are sent to the server.
    *
    * Note: This attribute is allowed only if the `&lt;img&gt;` element is a descendant
    * of an `&lt;a&gt;` element with a valid href attribute.
    *
    * MDN
    */
  lazy val ismap        = new BoolAttributeBuilder("ismap")
  @deprecated("Deprecated, use 'ismap' instead", "0.9.0")
  lazy val isMap        = ismap

  /** For use in &lt;meter&gt; tags.
    *
    * @see https://css-tricks.com/html5-meter-element/
    */
  lazy val low          = new AttributeBuilder[Double]("low")


  /** This attribute specifies the media which the linked resource applies to.
    * Its value must be a media query. This attribute is mainly useful when
    * linking to external stylesheets by allowing the user agent to pick
    * the best adapted one for the device it runs on.
    *
    * @see https://developer.mozilla.org/en-US/docs/Web/HTML/Element/link#attr-media
    */
  lazy val media        = new AttributeBuilder[Any]("media")

  /** This Boolean attribute indicates whether the details will be shown to the
    * user on page load. Default is false and so details will be hidden.
    *
    * MDN
    */
  lazy val open         = new BoolAttributeBuilder("open")


  /** For use in &lt;meter&gt; tags.
    *
    * @see https://css-tricks.com/html5-meter-element/
    */
  lazy val optimum      = new AttributeBuilder[Double]("open")

  /** This attribute specifies the name of a group of commands to be toggled as
    * radio buttons when selected. May only be used where the type attribute is
    * radio.
    *
    * MDN
    */
  lazy val radiogroup   = new AttributeBuilder[Any]("radiogroup")

  /** This attribute names a relationship of the linked document to the current
    * document. The attribute must be a space-separated list of the link types
    * values. The most common use of this attribute is to specify a link to an
    * external style sheet: the rel attribute is set to stylesheet, and the href
    * attribute is set to the URL of an external style sheet to format the page.
    *
    * MDN
    */
  lazy val rel          = new AttributeBuilder[Any]("rel")

  /** This Boolean attribute specifies that the items of the list are specified
    * in reversed order.
    *
    * MDN HTML5
    */
  lazy val reversed     = new AttributeBuilder[Any]("reversed")

  /** The attribute describes the role(s) the current element plays in the
    * context of the document. This can be used, for example,
    * by applications and assistive technologies to determine the purpose of
    * an element. This could allow a user to make informed decisions on which
    * actions may be taken on an element and activate the selected action in a
    * device independent way. It could also be used as a mechanism for
    * annotating portions of a document in a domain specific way (e.g.,
    * a legal term taxonomy). Although the role attribute may be used to add
    * semantics to an element, authors should use elements with inherent
    * semantics, such as p, rather than layering semantics on semantically
    * neutral elements, such as div role="paragraph".
    *
    * See: [[http://www.w3.org/TR/role-attribute/#s_role_module_attributes]]
    */
  lazy val role         = new AttributeBuilder[String]("role")

  /** For use in &lt;style&gt; tags.
    *
    * If this attribute is present, then the style applies only to its parent element.
    * If absent, the style applies to the whole document.
    */
  lazy val scoped       = new AttributeBuilder[Any]("scoped")


  /** This attribute was used to define a region for hyperlinks to create an
    * image map. The values are circle, default, polygon, and rect. The format
    * of the coords attribute depends on the value of shape. For circle, the
    * value is x,y,r where x and y are the pixel coordinates for the center of
    * the circle and r is the radius value in pixels. For rect, the coords
    * attribute should be x,y,w,h. The x,y values define the upper-left-hand
    * corner of the rectangle, while w and h define the width and height
    * respectively. A value of polygon for shape requires x1,y1,x2,y2,... values
    * for coords. Each of the x,y pairs defines a point in the polygon, with
    * successive points being joined by straight lines and the last point joined
    * to the first. The value default for shape requires that the entire
    * enclosed area, typically an image, be used.
    *
    * MDN HTML4 ONLY
    */
  lazy val shape        = new AttributeBuilder[Any]("shape")


  /** This attribute defines the sizes of the icons for visual media contained
    * in the resource. It must be present only if the rel contains the icon link
    * types value. It may have the following values:
    *
    *   any, meaning that the icon can be scaled to any size as it is in a
    *   vectorial format, like image/svg+xml.
    *
    *   a white-space separated list of sizes, each in the format `&lt;width in
    *   pixels&gt;x&lt;height in pixels&gt;` or
    *   `&lt;width in pixels&gt;X&lt;height in pixels&gt;`.
    *   Each of these sizes must be contained in the resource.
    *
    * MDN
    */
  lazy val sizes        = new AttributeBuilder[Any]("sizes")

  /** If the value of the type attribute is image, this attribute specifies a URI
    * for the location of an image to display on the graphical submit button;
    * otherwise it is ignored.
    *
    * MDN
    */
  lazy val src          = new AttributeBuilder[Any]("src")

  /** A list of one or more strings separated by commas indicating a set of
    * possible image sources for the user agent to use.
    * Each string is composed of:
    *
    *   1. a URL to an image
    *
    *   2. optionally, whitespace followed by one of:
    *
    *     a width descriptor, or a positive integer directly followed by 'w'.
    *     The width descriptor is divided by the source size given in the sizes
    *     attribute to calculate the effective pixel density.
    *
    *     a pixel density descriptor, which is a positive floating point number
    *     directly followed by 'x'.
    *
    * If no descriptor is specified, the source is assigned the default
    * descriptor: 1x.
    *
    * It is incorrect to mix width descriptors and pixel density descriptors in
    * the same srcset attribute. Duplicate descriptors (for instance, two sources
    * in the same srcset which are both described with '2x') are invalid, too.
    *
    * The user agent selects any one of the available sources at its discretion.
    * This provides them with significant leeway to tailor their selection based
    * on things like user preferences or bandwidth conditions.
    *
    * MDN
    */
  lazy val srcset       = new AttributeBuilder[Any]("srcset")


  /** This integer attribute specifies the start value for numbering the
    * individual list items. Although the ordering type of list elements might
    * be Roman numerals, such as XXXI, or letters, the value of start is always
    * represented as a number. To start numbering elements from the letter "C",
    * use `&lt;ol start="3"&gt;`.
    *
    * Note: This attribute was deprecated in HTML4, but reintroduced in HTML5.
    *
    * MDN HTML5
    */
  lazy val start        = new AttributeBuilder[Int]("start")

  /** Defines the type of the element. */
  lazy val `type`       = new AttributeBuilder[Any]("type")

  /** Shorthand for the `type` attribute. */
  lazy val tpe          = `type`

  /** IE-specific property to prevent user selection. */
  lazy val unselectable = new AttributeBuilder[Double]("unselectable")

  /** The partial URL (starting with '#') of an image map associated with the
    * element.
    */
  lazy val usemap       = new AttributeBuilder[Any]("usemap")

  /** For the elements listed here, this establishes the element's width:
    *
    *   canvas
    *   embed
    *   iframe
    *   img
    *   input
    *   object
    *   video
    *
    * Note: For all other instances, such as `&lt;div&gt;`, this is a legacy
    * attribute, in which case the CSS width property should be used instead.
    *
    * MDN
    */
  lazy val width        = new AttributeBuilder[Double]("width")

  /** The XML namespace. */
  lazy val xmlns        = new AttributeBuilder[Any]("xmlns")

  @deprecated("The HTML keygen element, that this attribute belongs to has been deprecated in the HTML spec", "0.9.0")
  lazy val challenge = new AttributeBuilder[Any]("challenge")
  @deprecated("The HTML keygen element, that this attribute belongs to has been deprecated in the HTML spec", "0.9.0")
  lazy val keyType   = new AttributeBuilder[Any]("keytype")
}

/** Miscellaneous Events. */
trait MiscellaneousEventAttributes extends SharedEventAttributes {
  private def event(name: String) = EmitterBuilder[Event](name)

  /** Fires when a `&lt;menu&gt;` element is shown as a context menu. */
  lazy val show = event("show")

  /** Fires when the user opens or closes the `&lt;details&gt;` element. */
  lazy val toggle = event("toggle")
}

/** Mouse Events: triggered by a mouse, or similar user actions. */
trait MouseEventAttributes {
  /** The click event is raised when the user clicks on an element. The click
    * event will occur after the mousedown and mouseup events.
    *
    * MDN
    */
  lazy val click              = EmitterBuilder[MouseEvent]("click")

  /** Script to be run when a context menu is triggered
    */
  lazy val contextmenu        = EmitterBuilder[MouseEvent]("contextmenu")
  @deprecated("Deprecated, use 'contextmenu' instead", "0.9.0")
  lazy val contextMenu        = contextmenu

  /** The dblclick event is fired when a pointing device button (usually a
    * mouse button) is clicked twice on a single element.
    *
    * MDN
    */
  lazy val dblclick           = EmitterBuilder[MouseEvent]("dblclick")

  /** Script to be run when an element is dragged. */
  lazy val drag               = EmitterBuilder[DragEvent]("drag")

  /** Script to be run at the end of a drag operation. */
  lazy val dragend            = EmitterBuilder[DragEvent]("dragend")
  @deprecated("Deprecated, use 'dragend' instead", "0.9.0")
  lazy val dragEnd            = dragend

  /** Script to be run when an element has been dragged to a valid drop target. */
  lazy val dragenter          = EmitterBuilder[DragEvent]("dragenter")
  @deprecated("Deprecated, use 'dragenter' instead", "0.9.0")
  lazy val dragEnter          = dragenter

  /** Script to be run when an element leaves a valid drop target. */
  lazy val dragleave          = EmitterBuilder[DragEvent]("dragleave")
  @deprecated("Deprecated, use 'dragleave' instead", "0.9.0")
  lazy val dragLeave          = dragleave

  /** Script to be run when an element is being dragged over a valid drop target. */
  lazy val dragover           = EmitterBuilder[DragEvent]("dragover")
  @deprecated("Deprecated, use 'dragover' instead", "0.9.0")
  lazy val dragOver           = dragover


  /** Script to be run at the start of a drag operation. */
  lazy val dragstart          = EmitterBuilder[DragEvent]("dragstart")
  @deprecated("Deprecated, use 'dragstart' instead", "0.9.0")
  lazy val dragStart          = dragstart


  /** Script to be run when dragged element is being dropped. */
  lazy val drop               = EmitterBuilder[DragEvent]("drop")

  /** The mousedown event is raised when the user presses the mouse button.
    *
    * MDN
    */
  lazy val mousedown          = EmitterBuilder[MouseEvent]("mousedown")

  /** The mouseenter event is fired when a pointing device (usually a mouse) is
    * moved over the element that has the listener attached.
    *
    * MDN
    */
  lazy val mouseenter         = EmitterBuilder[MouseEvent]("mouseenter")

  /** The mouseleave event is fired when a pointing device (usually a mouse) is
    * moved off the element that has the listener attached.
    *
    * MDN
    */
  lazy val mouseleave         = EmitterBuilder[MouseEvent]("mouseleave")

  /** The mousemove event is raised when the user moves the mouse.
    *
    * MDN
    */
  lazy val mousemove          = EmitterBuilder[MouseEvent]("mousemove")

  /** The mouseover event is raised when the user moves the mouse over a
    * particular element.
    *
    * MDN
    */
  lazy val mouseover          = EmitterBuilder[MouseEvent]("mouseover")


  /** The mouseout event is fired when a pointing device (usually a mouse) is
    * moved off the element that has the listener attached or off one of its
    * children. Note that it is also triggered on the parent when you move onto
    * a child element, since you move out of the visible space of the parent.
    *
    * MDN
    */
  lazy val mouseout           = EmitterBuilder[MouseEvent]("mouseout")

  /** The mouseup event is raised when the user releases the mouse button.
    *
    * MDN
    */
  lazy val mouseup            = EmitterBuilder[MouseEvent]("mouseup")


  /** The pointerlockchange event is fired when the pointer is locked/unlocked.
    *
    * MDN
    */
  lazy val pointerlockchange  = EmitterBuilder[MouseEvent]("pointerlockchange")
  @deprecated("Deprecated, use 'pointerlockchange' instead", "0.9.0")
  lazy val pointerLockChange  = pointerlockchange


  /** The pointerlockerror event is fired when locking the pointer failed
    * (for technical reasons or because the permission was denied).
    *
    * MDN
    */
  lazy val pointerlockerror   = EmitterBuilder[MouseEvent]("pointerlockerror")
  @deprecated("Deprecated, use 'pointerlockerror' instead", "0.9.0")
  lazy val pointerLockError   = pointerlockerror


  /** Specifies the function to be called when the window is scrolled.
    *
    * MDN
    */
  lazy val scroll             = EmitterBuilder[MouseEvent]("scroll")

  /**
    * The select event only fires when text inside a text input or textarea is
    * selected. The event is fired after the text has been selected.
    *
    * MDN
    */
  lazy val select             = EmitterBuilder[MouseEvent]("select")

  /** The wheel event is fired when a wheel button of a pointing device (usually
    * a mouse) is rotated. This event replaces the non-standard deprecated
    * mousewheel event.
    *
    * MDN
    */
  lazy val wheel              = EmitterBuilder[WheelEvent]("wheel")
}

/** OutWatch specific attributes used to asign child nodes to a VNode. */
trait OutWatchChildAttributes {
  /** A special attribute that takes a stream of single child nodes. */
  lazy val child    = ChildStreamReceiverBuilder

  /** A special attribute that takes a stream of lists of child nodes. */
  lazy val children = ChildrenStreamReceiverBuilder
}

/** Outwatch component life cycle hooks. */
trait OutWatchLifeCycleAttributes {
  /** Lifecycle hook for component insertion. */
  lazy val insert   = InsertHookBuilder

  /** Lifecycle hook for component updates. */
  lazy val update   = UpdateHookBuilder

  /** Lifecycle hook for component destruction. */
  lazy val destroy  = DestroyHookBuilder
}

/** Snabbdom Key Attribute */
trait SnabbdomKeyAttributes {
  lazy val key = KeyBuilder

  def prop(name: String) = new PropertyBuilder[Any](name)
}

trait SharedEventAttributes {
  /** Script to be run when an error occurs when the file is being loaded. */
  lazy val error = EmitterBuilder[Event]("error")
}

/** Attributes applicable to the table element and its children. */
trait TableAttributes {

  /** This attribute contains a non-negative integer value that indicates for
    * how many columns the cell extends. Its default value is 1; if its value
    * is set to 0, it extends until the end of the `&lt;colgroup&gt;`, even if implicitly
    * defined, that the cell belongs to. Values higher than 1000 will be considered
    * as incorrect and will be set to the default value (1).
    *
    * MDN
    */
  lazy val colspan  = new AttributeBuilder[Any]("colspan")

  /** This attribute contains a list of space-separated strings, each
    * corresponding to the id attributes of `&lt;th&gt;` elements that relate to
    * this element.
    *
    * MDN
    */
  lazy val headers  = new AttributeBuilder[Any]("headers")

  /** This attribute contains a non-negative integer value that indicates for
    * how many rows the cell extends. Its default value is 1; if its value is
    * set to 0, it extends until the end of the table section (`&lt;thead&gt;`,
    * `&lt;tbody&gt;`, `&lt;tfoot&gt;`, even if implicitly defined, that the cell belongs to.
    * Values higher than 65534 are clipped down to 65534.
    *
    * MDN
    */
  lazy val rowspan  = new AttributeBuilder[Any]("rowspan")

  /** This enumerated attribute defines the cells that the header (defined in
    * the `&lt;th&gt;`) element relates to. It may have the following values:
    *
    *   row:      The header relates to all cells of the row it belongs to.
    *
    *   col:      The header relates to all cells of the column it belongs to.
    *
    *   rowgroup: The header belongs to a rowgroup and relates to all of its
    *             cells. These cells can be placed to the right or the left of
    *             the header, depending on the value of the dir attribute in the
    *             `&lt;table&gt;` element.
    *
    *   colgroup: The header belongs to a colgroup and relates to all of its cells.
    *
    *   auto
    *
    * MDN
    */
  lazy val scope    = new AttributeBuilder[Any]("scope")

  /** This attribute defines an alternative text that summarizes the content of
    * the table.
    *
    * This deprecated API should no longer be used, but will probably still work.
    *
    * MDN
    */
  lazy val summary  = new AttributeBuilder[Any]("summary")
}

/** Touch events. */
trait TouchEventAttributes {
  /** The touchcancel event is fired when a touch point has been disrupted in an
    * implementation-specific manner (for example, too many touch points are
    * created).
    *
    * MDN
    */
  lazy val touchcancel = EmitterBuilder[TouchEvent]("touchcancel")

  /** The touchend event is fired when a touch point is removed from the touch
    * surface.
    *
    * MDN
    */
  lazy val touchend    = EmitterBuilder[TouchEvent]("touchend")

  /** The touchmove event is fired when a touch point is moved along the touch
    * surface.
    *
    * MDN
    */
  lazy val touchmove   = EmitterBuilder[TouchEvent]("touchmove")

  /** The touchstart event is fired when a touch point is placed on the touch
    * surface.
    *
    * MDN
    */
  lazy val touchstart  = EmitterBuilder[TouchEvent]("touchstart")
}

/** Window events. */
trait WindowEventAttrs extends SharedEventAttributes {
  /** Firefox 3 introduces two new events: "online" and "offline". These two
    * events are fired on the `&lt;body&gt;` of each page when the browser switches
    * between online and offline mode. Additionally, the events bubble up from
    * document.body, to document, ending at window. Both events are
    * non-cancellable (you can't prevent the user from coming online, or going
    * offline).
    *
    * MDN
    */
  lazy val offline = EmitterBuilder[Event]("offline")

  /** Firefox 3 introduces two new events: "online" and "offline". These two
    * events are fired on the `&lt;body&gt;` of each page when the browser switches
    * between online and offline mode. Additionally, the events bubble up from
    * document.body, to document, ending at window. Both events are
    * non-cancellable (you can't prevent the user from coming online, or going
    * offline).
    *
    * MDN
    */
  lazy val online  = EmitterBuilder[Event]("online")

  /** The resize event is fired when the document view has been resized. */
  lazy val resize  = EmitterBuilder[Event]("resize")
}

object Attributes extends Attributes
// scalastyle:on
