package outwatch

import cats.Functor

object implicits {

  @inline implicit class VNodeFunctorOps[F[_]: Functor](self: F[VNode]) {
    def apply(args: VMod*): F[VNode]   = Functor[F].map(self)(_.apply(args: _*))
    def append(args: VMod*): F[VNode]  = Functor[F].map(self)(_.append(args: _*))
    def prepend(args: VMod*): F[VNode] = Functor[F].map(self)(_.prepend(args: _*))
  }

  @inline implicit class VNodeFunctor2Ops[F[_]: Functor, G[_]: Functor](self: F[G[VNode]]) {
    def apply(args: VMod*): F[G[VNode]]   = Functor[F].map(self)(g => Functor[G].map(g)(_.apply(args: _*)))
    def append(args: VMod*): F[G[VNode]]  = Functor[F].map(self)(g => Functor[G].map(g)(_.append(args: _*)))
    def prepend(args: VMod*): F[G[VNode]] = Functor[F].map(self)(g => Functor[G].map(g)(_.prepend(args: _*)))
  }
}
