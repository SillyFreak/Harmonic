/**
 * StateListener.scala
 *
 * Created on 07.08.2013
 */

package at.pria.koza.harmonic

import java.util.EventListener

/**
 * <p>
 * `StateListener` provides a listener interface that can be used to detect when new states are added to an engine.
 * </p>
 *
 * @version V1.0 07.08.2013
 * @author SillyFreak
 */
trait StateListener extends EventListener {
  /**
   * <p>
   * Called when a `State` is added. All relevant information can be retrieved from the state: The engine the state
   * was added to via `getEngine()`; the engine originating the state via `getEngineId()` etc. Note that, because
   * at this moment the state was definitely not executed yet, `getAction()` will return `null`.
   * </p>
   *
   * @param state the state that was added
   */
  def stateAdded(state: State): Unit = {}
}
