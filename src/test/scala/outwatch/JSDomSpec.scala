package outwatch


import org.scalajs.dom._
import org.scalatest.BeforeAndAfterEach


abstract class JSDomSpec extends UnitSpec with BeforeAndAfterEach {
  override def beforeEach(): Unit = {
    document.body.innerHTML = ""

    val root = document.createElement("div")
    root.id = "app"
    document.body.appendChild(root)
  }
}
