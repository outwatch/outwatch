package outwatch

import colibri.Observable

package object util {

  /** A Function that applies an Action onto the Stores current state.
    * @param reducer
    *   The reducing function
    * @tparam A
    *   The Action Type
    * @tparam M
    *   The Model Type
    */
  @deprecated("Use colibri.Subject and pattern matching to build your own Store", "")
  type Reducer[A, M] = (M, A) => (M, Observable[A])
}
