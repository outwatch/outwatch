package outwatch



package object dom extends Attributes with Tags with Handlers {
  type VNode = VNodeIO[VDom]
}
