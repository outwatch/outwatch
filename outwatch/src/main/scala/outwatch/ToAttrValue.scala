package outwatch

trait ToAttrValue[T] {
  def convert(t: T): Attr.Value
}
object ToAttrValue {
  @inline def apply[T](implicit attrValue: ToAttrValue[T]): ToAttrValue[T] = attrValue

  implicit val ToAttrValueString: ToAttrValue[String]   = t => t
  implicit val ToAttrValueBoolean: ToAttrValue[Boolean] = t => t
  implicit val ToAttrValueInt: ToAttrValue[Int]         = t => t.toString
  implicit val ToAttrValueDouble: ToAttrValue[Double]   = t => t.toString
  implicit val ToAttrValueFloat: ToAttrValue[Float]     = t => t.toString
  implicit val ToAttrValueLong: ToAttrValue[Long]       = t => t.toString
  implicit val ToAttrValueByte: ToAttrValue[Byte]       = t => t.toString
  implicit val ToAttrValueShort: ToAttrValue[Short]     = t => t.toString
  implicit val ToAttrValueChar: ToAttrValue[Char]       = t => t.toString
}
