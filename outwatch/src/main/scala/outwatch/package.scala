import outwatch.helpers.BasicStyleBuilder

package object outwatch extends ManagedSubscriptions {
  type EmitterBuilder[+O, +R] = EmitterBuilderExecution[O, R, EmitterBuilder.Execution]

  //TODO: invent typeclass CanBuildStyle[F[_]]
  @inline implicit def StyleIsBuilder[T](style: StyleProp[T]): BasicStyleBuilder[T] = new BasicStyleBuilder[T](style.name)


  class StyleProp[V](
    override val name: String,
    val prefixes: Seq[String] = Nil
  ) extends Key with StyleStringValueBuilder[StyleSetter[V]] with DerivedStylePropBuilder[DerivedStyleProp.Base] {

    @inline def apply(value: V | String): Setter[HtmlElement] = {
      this := value
    }
  }
}
