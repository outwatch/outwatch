package outwatch

import cats.effect.IO
import monix.reactive.Observable

package object util {
  /**
   * A Function that applies an Action onto the Stores current state.
   * @param reducer The reducing function
   * @tparam A The Action Type
   * @tparam M The Model Type
   */
  type Reducer[A, M] = (M, A) => (M, Observable[A])

  object io extends StoreOps[IO]
}
