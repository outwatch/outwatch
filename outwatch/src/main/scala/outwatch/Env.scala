package outwatch

import hummingbird.syntax.{RxOps, TxOps}
import hummingbird.{Cereal, syntax}

class Env(implicit cereal: Cereal) {
  implicit private val self: Env = this

  type Rx[-T] = cereal.Rx[T]
  implicit def iRx: syntax.Rx = cereal.iRx
  implicit def rxOps[T](source: Rx[T]): RxOps[cereal.G, T] = cereal.rxOps(source)

  type Tx[+T] = cereal.Tx[T]
  implicit def iTx: syntax.Tx = cereal.iTx
  implicit def txOps[T](source: Tx[T]): TxOps[cereal.H, T] = cereal.txOps(source)

  type Cancelable = cereal.Cancelable

  val CEmitterBuilder = new CEmitterBuilder
  type EmitterBuilder[+O, +R] = CEmitterBuilder.EmitterBuilder[O, R]

  val CVDomModifier = new CVDomModifier
  type VDomModifier = CVDomModifier.VDomModifier
  val VDomModifier: CVDomModifier.VDomModifier.type = CVDomModifier.VDomModifier
  type VNode = CVDomModifier.VNode
  val VNode: CVDomModifier.VNode.type = CVDomModifier.VNode
  type VNodeProxyNode = CVDomModifier.VNodeProxyNode
  val VNodeProxyNode: CVDomModifier.VNodeProxyNode.type = CVDomModifier.VNodeProxyNode
  type CompositeModifier = CVDomModifier.CompositeModifier
  val CompositeModifier: CVDomModifier.CompositeModifier.type = CVDomModifier.CompositeModifier
  type StringVNode = CVDomModifier.StringVNode
  val StringVNode: CVDomModifier.StringVNode.type = CVDomModifier.StringVNode
  type SyncEffectModifier = CVDomModifier.SyncEffectModifier
  val SyncEffectModifier: CVDomModifier.SyncEffectModifier.type = CVDomModifier.SyncEffectModifier
  type StreamModifier = CVDomModifier.StreamModifier
  val StreamModifier: CVDomModifier.StreamModifier.type = CVDomModifier.StreamModifier
  type ChildCommandsModifier = CVDomModifier.ChildCommandsModifier
  val ChildCommandsModifier: CVDomModifier.ChildCommandsModifier.type = CVDomModifier.ChildCommandsModifier

  val CChildCommand = new CChildCommand
  type ChildCommand = CChildCommand.ChildCommand
  val ChildCommand: CChildCommand.ChildCommand.type = CChildCommand.ChildCommand
}
