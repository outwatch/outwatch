package outwatch.repairdom

import org.scalajs.dom._
import scala.scalajs.js

import outwatch.helpers.JSDefined
import outwatch.helpers.NativeHelpers._
import snabbdom.VNodeProxy

object RepairDom {

  @inline def setDirty(elem: Node): Unit = {
    elem.asInstanceOf[js.Dynamic].__dirty = 1
  }

  @inline def removeDirty(elem: Node): Unit = {
    elem.asInstanceOf[js.Dynamic].__dirty = 0
  }

  @inline def isDirty(elem: Node): Boolean = {
    elem.asInstanceOf[js.Dynamic].__dirty.asInstanceOf[js.UndefOr[Int]] == js.defined(1)
  }

  val patchHook: outwatch.Modifier = {
    new outwatch.PrePatchHook((beforeProxy, _) => if(beforeProxy.elm.exists(isDirty)) repairNode(beforeProxy))
  }

  def repairNode(proxy: VNodeProxy, level: Int = 0): Unit = {
    proxy.elm.foreach { elm =>
      proxy.text match {
        case JSDefined(text) =>
          elm.textContent = text
        case _               =>
          repairAttributes(proxy, elm)
          repairStyles(proxy, elm)

          proxy.children match {
            case JSDefined(childProxies) =>
              repairProxyNodes(childProxies, elm, level)
              removeAppendedNodes(childProxies, elm)
            case _                       =>
              removeAllDomChildren(elm)
          }
          repairProps(proxy, elm) // props could insert children with innerHTML
      }
    }

    def repairAttributes(proxy: VNodeProxy, elem: Element): Unit = {
      proxy.data match {
        case JSDefined(data) =>
          data.attrs match {
            case JSDefined(proxyAttributes) =>
              // fix or remove existing attributes
              var i = elem.attributes.length - 1
              while(i >= 0) {
                val currentAttribute = elem.attributes(i)
                val name = currentAttribute.name
                proxyAttributes.raw(name) match {
                  case JSDefined(value) =>
                    elem.setAttribute(name, value.toString)
                  case _                =>
                    elem.removeAttribute(name)
                }
                i -= 1
              }
              // add remaining attributes
              proxyAttributes.keys.foreach { name =>
                val value = proxyAttributes(name)
                elem.setAttribute(name, value.toString)
              }
            case _                          => removeAllAttributes(elem)
          }
        case _               => removeAllAttributes(elem)
      }
    }

    def removeAllAttributes(elem: Element): Unit = {
      var i = elem.attributes.length - 1
      while(i >= 0) {
        val name = elem.attributes(i).name
        if(name != "style")
          elem.removeAttribute(name)
        i -= 1
      }
    }


    def repairStyles(proxy: VNodeProxy, elem: Element): Unit = {
      proxy.data match {
        case JSDefined(data) =>
          data.style match {
            case JSDefined(proxyStyles) =>
              // fix or remove existing styles
              var i = elem.style.length - 1
              while(i >= 0) {
                val name = elem.style(i)
                proxyStyles.raw(name) match {
                  case JSDefined(value) => elem.style.setProperty(name, value.asInstanceOf[String])
                  case _                => elem.style.removeProperty(name)
                }
                i -= 1
              }
              // add remaining attributes
              proxyStyles.keys.foreach { name =>
                val value = proxyStyles(name)
                elem.style.setProperty(name, value.asInstanceOf[String])
              }
            case _                      => removeAllStyles(elem)
          }
        case _               => removeAllStyles(elem)
      }
    }

    def removeAllStyles(elem: Element): Unit = {
      elem.removeAttribute("style")
    }

    def repairProps(proxy: VNodeProxy, elem: Element): Unit = {
      proxy.data match {
        case JSDefined(data) =>
          data.props match {
            case JSDefined(proxyProps) =>
              val domProps = elem.asInstanceOf[js.Dictionary[js.Any]]
              // fix or remove existing props
              domProps.keys.foreach { key =>
                proxyProps.raw(key) match {
                  case JSDefined(value) => domProps(key) = value.asInstanceOf[js.Any]
                  case _                => domProps -= key
                }
              }

              // add remaining props
              proxyProps.keys.foreach { key =>
                val value = proxyProps(key)
                domProps(key) = value.asInstanceOf[js.Any]
              }
            case _                     => removeAllProps(elem)
          }
        case _               => removeAllProps(elem)
      }
    }

    def removeAllProps(elem: Element): Unit = {
      js.Object.keys(elem).foreach { key =>
        // use Reflect.deleteProperty, because it does not crash like delete on non-configurable props
        // see: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Reflect/deleteProperty
        window.asInstanceOf[js.Dynamic].Reflect.deleteProperty(elem, key)
      }
    }

    def removeAllDomChildren(parentNode: Element) = {
      while(parentNode.firstChild != null) {
        parentNode.removeChild(parentNode.firstChild)
      }
    }

    def repairProxyNodes(childProxies: js.Array[VNodeProxy], parentNode: Element, level: Int) = {
      var i = 0
      val childProxyCount = childProxies.length
      while(i < childProxyCount) {
        val childProxy = childProxies(i)
        childProxy.elm.foreach { originalDomChild =>
          if(i < parentNode.childNodes.length) {
            val currentDomChild = parentNode.childNodes(i)
            if(currentDomChild != originalDomChild) {
              parentNode.replaceChild(originalDomChild, currentDomChild)
            }
          }
          else
            parentNode.appendChild(originalDomChild)
        }
        repairNode(childProxy, level + 1)
        i += 1
      }
    }

    def removeAppendedNodes(childProxies: js.Array[VNodeProxy], parentNode: Element) = {
      var i = childProxies.length
      val domChildrenCount = parentNode.childNodes.length
      while(i < domChildrenCount) {
        parentNode.removeChild(parentNode.childNodes(i))
        i += 1
      }
    }
  }
}
