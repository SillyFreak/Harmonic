/**
 * StateListener.scala
 *
 * Created on 07.08.2013
 */

package at.pria.koza.harmonic

import java.util.EventListener

/**
 * <p>
 * `StateListener` listen to states being added to an engine.
 * </p>
 *
 * @version V1.0 07.08.2013
 * @author SillyFreak
 */
trait StateListener extends EventListener {
  /**
   * <p>
   * Called when a `State` is added. All relevant information can be retrieved from the state: The `engine` the
   * state was added to; the engine originating the state via `engineId` etc. Note that at this moment the state
   * was definitely not executed yet.
   * </p>
   *
   * @param state the state that was added
   */
  def stateAdded(state: State): Unit = {}
}
