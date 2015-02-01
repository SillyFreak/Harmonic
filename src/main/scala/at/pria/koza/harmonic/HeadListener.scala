/**
 * HeadListener.scala
 *
 * Created on 07.08.2013
 */

package at.pria.koza.harmonic

import java.util.EventListener

/**
 * <p>
 * `HeadListener`s listen to an engine's head being moved.
 * </p>
 *
 * @version V1.0 07.08.2013
 * @author SillyFreak
 */
trait HeadListener extends EventListener {
  /**
   * <p>
   * Called when an engine's head is moved.
   * </p>
   *
   * @param prevHead the engine's head previous to the move
   * @param newHead the engine's new head state
   */
  def headMoved(prevHead: State, newHead: State): Unit = {}
}
