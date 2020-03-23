package outwatch.libs.hammerjs

import scala.scalajs.js

import outwatch.libs.hammerjs.facade._
import outwatch._
import outwatch.dsl._

trait HammerJs {
  def onHammer(events: String, cssUserSelect: js.UndefOr[String] = "auto"): EmitterBuilder.Sync[facade.Event, VDomModifier] = {
    EmitterBuilder[Event, VDomModifier] { sink =>
      managedElement.asHtml { elem =>
        elem.asInstanceOf[js.Dynamic].hammer = js.undefined
        val hammertime = new Hammer[Event](elem, new Options { cssProps = new CssProps { userSelect = cssUserSelect } })
        propagating(hammertime).on(events, { e =>
          e.stopPropagation()
          sink.onNext(e)
        })

        cancelable { () =>
          hammertime.stop()
          hammertime.destroy()
          elem.asInstanceOf[js.Dynamic].hammer = js.undefined
        }
      }
    }
  }

  val onTap: EmitterBuilder.Sync[Event, VDomModifier] = onHammer("tap")
  val onPress: EmitterBuilder.Sync[Event, VDomModifier] = onHammer("press")
  val onSwipeRight: EmitterBuilder.Sync[Event, VDomModifier] = onHammer("swiperight")
  val onSwipeLeft: EmitterBuilder.Sync[Event, VDomModifier] = onHammer("swipeleft")
}

object HammerJs extends HammerJs
