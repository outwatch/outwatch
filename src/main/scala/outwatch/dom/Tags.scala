// scalastyle:off number.of.methods

/** Documentation marked "MDN" is thanks to Mozilla Contributors
  * at https://developer.mozilla.org/en-US/docs/Web/API and available
  * under the Creative Commons Attribution-ShareAlike v2.5 or later.
  * http://creativecommons.org/licenses/by-sa/2.5/
  *
  * Other tag documentation is thanks to Li Haoyi's scalatags and is under
  * the MIT License.
  * http://opensource.org/licenses/MIT
  */

package outwatch.dom

import cats.effect.IO


/** Trait that contains all tags, so they can be mixed in to other objects if needed.
  */
trait Tags {
  private def tag(nodeType: String)(args: Seq[VDomModifier]): VNode = IO.pure(VTree(nodeType, args))

  /** Represents a hyperlink, linking to another resource.
    *
    *  MDN
    */
  def a          (args: VDomModifier*): VNode = tag("a")(args)

  /** An abbreviation or acronym; the expansion of the abbreviation can be
    * represented in the title attribute.
    *
    *  MDN
    */
  def abbr       (args: VDomModifier*): VNode = tag("abbr")(args)

  /** Defines a section containing contact information.
    *
    *  MDN
    */
  def address    (args: VDomModifier*): VNode = tag("address")(args)

  /** In conjunction with map, defines an image map
    *
    *  MDN
    */
  def area       (args: VDomModifier*): VNode = tag("area")(args)

  /** Defines self-contained content that could exist independently of the rest
    * of the content.
    *
    *  MDN
    */
  def article    (args: VDomModifier*): VNode = tag("article")(args)

  /** Defines some content loosely related to the page content. If it is removed,
    * the remaining content still makes sense.
    *
    *  MDN
    */
  def aside      (args: VDomModifier*): VNode = tag("aside")(args)

  /** Represents a sound or an audio stream.
    *
    *  MDN
    */
  def audio      (args: VDomModifier*): VNode = tag("audio")(args)

  /** Bold text.
    *
    *  MDN
    */
  def b          (args: VDomModifier*): VNode = tag("b")(args)

  /** Defines the base URL for relative URLs in the page.
    *
    *  MDN
    */
  def base       (args: VDomModifier*): VNode = tag("base")(args)

  /** Represents text that must be isolated from its surrounding for bidirectional
    * text formatting. It allows embedding a span of text with a different, or
    * unknown, directionality.
    *
    *  MDN
    */
  def bdi        (args: VDomModifier*): VNode = tag("bdi")(args)

  /** Represents the directionality of its children, in order to explicitly
    * override the Unicode bidirectional algorithm.
    *
    *  MDN
    */
  def bdo        (args: VDomModifier*): VNode = tag("bdo")(args)

  /** Represents a content that is quoted from another source.
    *
    *  MDN
    */
  def blockquote (args: VDomModifier*): VNode = tag("blockquote")(args)

  /** Represents the content of an HTML document. There is only one body
    *   element in a document.
    *
    *  MDN
    */
  def body       (args: VDomModifier*): VNode = tag("body")(args)

  /** Represents a line break.
    *
    *  MDN
    */
  def br         (args: VDomModifier*): VNode = tag("br")(args)

  /** A button
    *
    *  MDN
    */
  def button     (args: VDomModifier*): VNode = tag("button")(args)

  /** Represents a bitmap area that scripts can use to render graphics like graphs,
    * games or any visual images on the fly.
    *
    *  MDN
    */
  def canvas     (args: VDomModifier*): VNode = tag("canvas")(args)

  /** The title of a table.
    *
    *  MDN
    */
  def caption    (args: VDomModifier*): VNode = tag("caption")(args)

  /** Represents the title of a work being cited.
    *
    *  MDN
    */
  def cite       (args: VDomModifier*): VNode = tag("cite")(args)

  /** Represents computer code.
    *
    *  MDN
    */
  def code       (args: VDomModifier*): VNode = tag("code")(args)

  /** A single column.
    *
    *  MDN
    */
  def col        (args: VDomModifier*): VNode = tag("col")(args)

  /** A set of columns.
    *
    *  MDN
    */
  def colgroup   (args: VDomModifier*): VNode = tag("colgroup")(args)

  /** Links a given content with a machine-readable translation.
    * If the content is time- or date-related, the `<time>` element must be used.
    *
    * MDN
    */
  def dataElement(args: VDomModifier*): VNode = tag("data")(args)

  /** A set of predefined options for other controls.
    *
    *  MDN
    */
  def datalist   (args: VDomModifier*): VNode = tag("datalist")(args)

  /** Represents the definition of the terms immediately listed before it.
    *
    * MDN
    */
  def dd         (args: VDomModifier*): VNode = tag("dd")(args)

  /** Defines a removal from the document.
    *
    * MDN
    */
  def del        (args: VDomModifier*): VNode = tag("del")(args)

  /** A widget from which the user can obtain additional information
    * or controls.
    *
    * MDN
    */
  def details    (args: VDomModifier*): VNode = tag("details")(args)

  /** Represents a term whose definition is contained in its nearest ancestor
    * content.
    *
    * MDN
    */
  def dfn        (args: VDomModifier*): VNode = tag("dfn")(args)

  /** Represents a dialog box or other interactive component, such as an inspector
    * or window.
    *
    * MDN
    */
  def dialog     (args: VDomModifier*): VNode = tag("dialog")(args)

  /** Represents a generic container with no special meaning.
    *
    * MDN
    */
  def div        (args: VDomModifier*): VNode = tag("div")(args)

  /** Defines a definition list; a list of terms and their associated definitions.
    *
    * MDN
    */
  def dl         (args: VDomModifier*): VNode = tag("dl")(args)

  /** Represents a term defined by the next dd
    *
    * MDN
    */
  def dt         (args: VDomModifier*): VNode = tag("dt")(args)

  /** Represents emphasized text.
    *
    * MDN
    */
  def em         (args: VDomModifier*): VNode = tag("em")(args)

  /** Represents a integration point for an external, often non-HTML, application
    * or interactive content.
    *
    * MDN
    */
  def embed      (args: VDomModifier*): VNode = tag("embed")(args)

  /** A set of fields.
    *
    * MDN
    */
  def fieldset   (args: VDomModifier*): VNode = tag("fieldset")(args)

  /** Represents the legend of a figure.
    *
    * MDN
    */
  def figcaption (args: VDomModifier*): VNode = tag("figcaption")(args)

  /** Represents a figure illustrated as part of the document.
    *
    * MDN
    */
  def figure     (args: VDomModifier*): VNode = tag("figure")(args)

  /** Defines the footer for a page or section. It often contains a copyright
    * notice, some links to legal information, or addresses to give feedback.
    *
    * MDN
    */
  def footer     (args: VDomModifier*): VNode = tag("footer")(args)

  /** Represents a form, consisting of controls, that can be submitted to a
    * server for processing.
    *
    * MDN
    */
  def form       (args: VDomModifier*): VNode = tag("form")(args)

  /** Heading level 1
    *
    * MDN
    */
  def h1         (args: VDomModifier*): VNode = tag("h1")(args)

  /** Heading level 2
    *
    * MDN
    */
  def h2         (args: VDomModifier*): VNode = tag("h2")(args)

  /** Heading level 3
    *
    * MDN
    */
  def h3         (args: VDomModifier*): VNode = tag("h3")(args)

  /** Heading level 4
    *
    * MDN
    */
  def h4         (args: VDomModifier*): VNode = tag("h4")(args)

  /** Heading level 5
    *
    * MDN
    */
  def h5         (args: VDomModifier*): VNode = tag("h5")(args)

  /** Heading level 6
    *
    * MDN
    */
  def h6         (args: VDomModifier*): VNode = tag("h6")(args)

  /** Represents a collection of metadata about the document, including links to,
    * or definitions of, scripts and style sheets.
    *
    * MDN
    */
  def head       (args: VDomModifier*): VNode = tag("head")(args)

  /** Defines the header of a page or section. It often contains a logo, the
    * title of the Web site, and a navigational table of content.
    *
    * MDN
    */
  def header     (args: VDomModifier*): VNode = tag("header")(args)

  /** Represents a thematic break between paragraphs of a section or article or
    * any longer content.
    *
    * MDN
    */
  def hr         (args: VDomModifier*): VNode = tag("hr")(args)

  /** Italicized text.
    *
    * MDN
    */
  def i          (args: VDomModifier*): VNode = tag("i")(args)

  /** Represents a nested browsing context, that is an embedded HTML document.
    *
    * MDN
    */
  def iframe     (args: VDomModifier*): VNode = tag("iframe")(args)

  /** Represents an image.
    *
    * MDN
    */
  def img        (args: VDomModifier*): VNode = tag("img")(args)

  /** A typed data field allowing the user to input data.
    *
    * MDN
    */
  def input      (args: VDomModifier*): VNode = tag("input")(args)

  /** Defines an addition to the document.
    *
    * MDN
    */
  def ins        (args: VDomModifier*): VNode = tag("ins")(args)

  /** Represents user input, often from a keyboard, but not necessarily.
    *
    * MDN
    */
  def kbd        (args: VDomModifier*): VNode = tag("kbd")(args)

  /** A key-pair generator control.
    *
    * MDN
    */
  @deprecated(
    """
      |This feature has been removed from the Web standards. Though some
      |browsers may still support it, it is in the process of being dropped.
      |Avoid using it and update existing code if possible; see the
      |compatibility table at the bottom of this page to guide your decision.
      |Be aware that this feature may cease to work at any time.
    """.stripMargin, "0.9.0")
  def keygen     (args: VDomModifier*): VNode = tag("keygen")(args)

  /** The caption of a single field
    *
    * MDN
    */
  def label      (args: VDomModifier*): VNode = tag("label")(args)

  /** The caption for a fieldset.
    *
    * MDN
    */
  def legend     (args: VDomModifier*): VNode = tag("legend")(args)

  /** Defines an item of an list.
    *
    * MDN
    */
  def li         (args: VDomModifier*): VNode = tag("li")(args)

  /** Used to link JavaScript and external CSS with the current HTML document.
    *
    * MDN
    */
  def link       (args: VDomModifier*): VNode = tag("link")(args)

  /** Defines the main or important content in the document. There is only one
    * main element in the document.
    *
    * MDN
    */
  def main       (args: VDomModifier*): VNode = tag("main")(args)

  /** In conjunction with area, defines an image map.
    *
    * MDN
    */
  def map        (args: VDomModifier*): VNode = tag("map")(args)

  /** Represents text highlighted for reference purposes, that is for its
    * relevance in another context.
    *
    * MDN
    */
  def mark       (args: VDomModifier*): VNode = tag("mark")(args)

  /** Defines a mathematical formula.
    *
    * MDN
    */
  def math       (args: VDomModifier*): VNode = tag("math")(args)

  /** Represents a group of commands that a user can perform or activate.
    * This includes both list menus, which might appear across the top of
    * a screen, as well as context menus, such as those that might appear
    * underneath a button after it has been clicked.
    *
    * MDN
    */
  def menu       (args: VDomModifier*): VNode = tag("menu")(args)

  /** Represents a command that a user is able to invoke through a popup menu.
    * This includes context menus, as well as menus that might be attached to
    * a menu button.
    *
    * A command can either be defined explicitly, with a textual label and
    * optional icon to describe its appearance, or alternatively as an indirect
    * command whose behavior is defined by a separate element. Commands can
    * also optionally include a checkbox or be grouped to share radio buttons.
    * (Menu items for indirect commands gain checkboxes or radio buttons when
    * defined against elements <input type="checkbox"> and
    * <input type="radio">.)
    *
    * MDN
    */
  def menuitem   (args: VDomModifier*): VNode = tag("menuitem")(args)

  /** Defines metadata that can't be defined using another HTML element.
    *
    * MDN
    */
  def meta       (args: VDomModifier*): VNode = tag("meta")(args)

  /** A scalar measurement within a known range.
    *
    * MDN
    */
  def meter      (args: VDomModifier*): VNode = tag("meter")(args)

  /** Represents a section of a page that links to other pages or to parts within
    * the page: a section with navigation links.
    *
    * MDN
    */
  def nav        (args: VDomModifier*): VNode = tag("nav")(args)

  /** Defines alternative content to display when the browser doesn't support
    * scripting.
    *
    * MDN
    */
  def noscript   (args: VDomModifier*): VNode = tag("noscript")(args)

  /** Represents an external resource, which is treated as an image, an HTML
    * sub-document, or an external resource to be processed by a plug-in.
    *
    * MDN
    */
  def `object`   (args: VDomModifier*): VNode = tag("object")(args)

  /** Defines an ordered list of items.
    *
    * MDN
    */
  def ol         (args: VDomModifier*): VNode = tag("ol")(args)

  /** A set of options, logically grouped.
    *
    * MDN
    */
  def optgroup   (args: VDomModifier*): VNode = tag("optgroup")(args)

  /**
    * An option in a select element.
    *
    *  MDN
    */
  def option     (args: VDomModifier*): VNode = tag("option")(args)

  /** The result of a calculation
    *
    * MDN
    */
  def output     (args: VDomModifier*): VNode = tag("output")(args)

  /** Defines a portion that should be displayed as a paragraph.
    *
    * MDN
    */
  def p          (args: VDomModifier*): VNode = tag("p")(args)

  /** Defines parameters for use by plug-ins invoked by object elements.
    *
    * MDN
    */
  def param      (args: VDomModifier*): VNode = tag("param")(args)

  /** Indicates that its content is preformatted and that this format must be
    * preserved.
    *
    * MDN
    */
  def pre        (args: VDomModifier*): VNode = tag("pre")(args)

  /** A progress completion bar
    *
    * MDN
    */
  def progress   (args: VDomModifier*): VNode = tag("progress")(args)

  /** An inline quotation.
    *
    * MDN
    */
  def q          (args: VDomModifier*): VNode = tag("q")(args)

  /** Represents parenthesis around a ruby annotation, used to display the
    * annotation in an alternate way by browsers not supporting the standard
    * display for annotations.
    *
    * MDN
    */
  def rp         (args: VDomModifier*): VNode = tag("rp")(args)

  /** Represents content to be marked with ruby annotations, short runs of text
    * presented alongside the text. This is often used in conjunction with East
    * Asian language where the annotations act as a guide for pronunciation, like
    * the Japanese furigana .
    *
    * MDN
    */
  def ruby         (args: VDomModifier*): VNode = tag("ruby")(args)

  /** Represents the text of a ruby annotation.
    *
    * MDN
    */
  def rt         (args: VDomModifier*): VNode = tag("rt")(args)

  /** Strikethrough element, used for that is no longer accurate or relevant.
    *
    * MDN
    */
  def s          (args: VDomModifier*): VNode = tag("s")(args)

  /** Represents sample output of a program or a computer.
    *
    * MDN
    */
  def samp       (args: VDomModifier*): VNode = tag("samp")(args)

  /** Defines either an internal script or a link to an external script. The
    * script language is JavaScript.
    *
    * MDN
    */
  def script     (args: VDomModifier*): VNode = tag("script")(args)

  /** Represents a generic section of a document, i.e., a thematic grouping of
    * content, typically with a heading.
    *
    * MDN
    */
  def section    (args: VDomModifier*): VNode = tag("section")(args)

  /** A control that allows the user to select one of a set of options.
    *
    * MDN
    */
  def select     (args: VDomModifier*): VNode = tag("select")(args)

  /** Represents a placeholder inside a web component that you can fill with
    * your own markup, with the effect of composing different DOM trees together.
    *
    * MDN
    */
  def slot       (args: VDomModifier*): VNode = tag("slot")(args)

  /** Represents a side comment; text like a disclaimer or copyright, which is not
    * essential to the comprehension of the document.
    *
    * MDN
    */
  def small      (args: VDomModifier*): VNode = tag("small")(args)

  /** Allows the authors to specify alternate media resources for media elements
    * like video or audio
    *
    * MDN
    */
  def source     (args: VDomModifier*): VNode = tag("source")(args)

  /** Represents text with no specific meaning. This has to be used when no other
    * text-semantic element conveys an adequate meaning, which, in this case, is
    * often brought by global attributes like class, lang, or dir.
    *
    * MDN
    */
  def span       (args: VDomModifier*): VNode = tag("span")(args)

  /** Represents especially important text.
    *
    * MDN
    */
  def strong     (args: VDomModifier*): VNode = tag("strong")(args)

  /** Used to write inline CSS.
    *
    * MDN
    */
  def style      (args: VDomModifier*): VNode = tag("style")(args)

  /** Subscript tag
    *
    * MDN
    */
  def sub        (args: VDomModifier*): VNode = tag("sub")(args)

  /** A summary, caption, or legend for a given details.
    *
    * MDN
    */
  def summary    (args: VDomModifier*): VNode = tag("summary")(args)

  /** Superscript tag.
    *
    * MDN
    */
  def sup        (args: VDomModifier*): VNode = tag("sup")(args)

  /** Represents data with more than one dimension.
    *
    * MDN
    */
  def table      (args: VDomModifier*): VNode = tag("table")(args)

  /** The table body.
    *
    * MDN
    */
  def tbody      (args: VDomModifier*): VNode = tag("tbody")(args)

  /** A single cell in a table.
    *
    * MDN
    */
  def td         (args: VDomModifier*): VNode = tag("td")(args)

  /** A multiline text edit control.
    *
    * MDN
    */
  def textarea   (args: VDomModifier*): VNode = tag("textarea")(args)

  /** The table footer.
    *
    * MDN
    */
  def tfoot      (args: VDomModifier*): VNode = tag("tfoot")(args)

  /** A header cell in a table.
    *
    * MDN
    */
  def th         (args: VDomModifier*): VNode = tag("th")(args)

  /** The table headers.
    *
    * MDN
    */
  def thead      (args: VDomModifier*): VNode = tag("thead")(args)

  /** Represents a date and time value; the machine-readable equivalent can be
    * represented in the datetime attribetu
    *
    * MDN
    */
  def time       (args: VDomModifier*): VNode = tag("time")(args)

  /** Defines the title of the document, shown in a browser's title bar or on the
    * page's tab. It can only contain text and any contained tags are not
    * interpreted.
    *
    * MDN
    */
  def title      (args: VDomModifier*): VNode = tag("title")(args)

  /** A single row in a table.
    *
    * MDN
    */
  def tr         (args: VDomModifier*): VNode = tag("tr")(args)

  /** Allows authors to specify timed text track for media elements like video or
    * audio
    *
    * MDN
    */
  def track      (args: VDomModifier*): VNode = tag("track")(args)

  /** Underlined text.
    *
    * MDN
    */
  def u          (args: VDomModifier*): VNode = tag("u")(args)

  /** Defines an unordered list of items.
    *
    * MDN
    */
  def ul         (args: VDomModifier*): VNode = tag("ul")(args)

  /** Represents a video, and its associated audio files and captions, with the
    * necessary interface to play it.
    *
    * MDN
    */
  def video      (args: VDomModifier*): VNode = tag("video")(args)

  /** Represents a line break opportunity, that is a suggested point for wrapping
    * text in order to improve readability of text split on several lines.
    *
    * MDN
    */
  def wbr        (args: VDomModifier*): VNode = tag("wbr")(args)
}

object Tags extends Tags
// scalastyle:on
