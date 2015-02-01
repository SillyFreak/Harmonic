/**
 * BranchListener.scala
 *
 * Created on 07.08.2013
 */

package at.pria.koza.harmonic

import java.util.EventListener

/**
 * <p>
 * `BranchListener`s listen to branches being created, moved, or added in an engine.
 * </p>
 *
 * @version V1.0 07.08.2013
 * @author SillyFreak
 */
trait BranchListener extends EventListener {
  /**
   * <p>
   * Called when a new branch is added to the engine.
   * </p>
   *
   * @param engine the Engine a branch is added to
   * @param branch the branch being created
   * @param tip the initial tip of the new branch
   */
  def branchCreated(engine: Engine, branch: String, tip: State): Unit = {}

  /**
   * <p>
   * Called when an existing branch is moved.
   * </p>
   *
   * @param engine the Engine containing the branch
   * @param branch the branch being moved
   * @param prevTip the previous tip of the branch
   * @param newTip the new tip of the branch
   */
  def branchMoved(engine: Engine, branch: String, prevTip: State, newTip: State): Unit = {}

  /**
   * <p>
   * Called when a branch is deleted.
   * </p>
   *
   * @param engine the Engine the branch is deleted from
   * @param branch the branch being deleted
   * @param tip the tip of the branch previous to being deleted
   */
  def branchDeleted(engine: Engine, branch: String, tip: State): Unit = {}
}
