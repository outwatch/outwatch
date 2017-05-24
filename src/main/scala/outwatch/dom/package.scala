package outwatch

package object dom extends Handlers
{
  object all extends Tags with Attributes

  object <^ {
    object < extends Tags
    object ^ extends Attributes
  }

  object Handlers extends Handlers
}
