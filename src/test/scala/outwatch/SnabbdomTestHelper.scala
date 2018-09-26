package outwatch

import org.scalajs.dom.Event
import snabbdom.{DataObject, Hooks}
import snabbdom.DataObject.{AttrValue, PropValue, StyleValue}

import scala.scalajs.js

trait SnabbdomTestHelper {
  def createDataObject(attrs: js.UndefOr[js.Dictionary[AttrValue]] = js.Dictionary[AttrValue](),
            on: js.UndefOr[js.Dictionary[js.Function1[Event, Unit]]] = js.Dictionary[js.Function1[Event, Unit]](),
            hooks : Hooks = Hooks()
           ): DataObject = DataObject(attrs, js.Dictionary[PropValue](), js.Dictionary[StyleValue](), on, hooks, js.undefined)

}
