package outwatch

// We recommend to use a sanitizer whenever injecting html-strings into your dom.
// Please use with caution!
final case class UnsafeHTML(html: String) extends AnyVal
