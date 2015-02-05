/**
 * RemoteEngineListener.scala
 *
 * Created on 05.02.2015
 */
package at.pria.koza.harmonic

import java.util.EventListener

/**
 * <p>
 * {@code RemoteEngineListener}
 * </p>
 *
 * @version V0.0 05.02.2015
 * @author SillyFreak
 */
trait RemoteEngineListener extends EventListener {
  /**
   * <p>
   * Called when a new branch is added to the engine.
   * </p>
   *
   * @param remote the RemoteEngine of which the heads were updated
   * @param oldHeads the previous head map of the remote
   * @param newHeads the new head map of the remote
   */
  def headsUpdated(remote: RemoteEngine, oldHeads: Map[String, Long], newHeads: Map[String, Long]): Unit = {}
}
