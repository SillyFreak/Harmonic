/**
 * BranchListener.scala
 *
 * Created on 07.08.2013
 */

package at.pria.koza.harmonic

import java.util.EventListener

/**
 * <p>
 * `BranchListener` provides a listener interface that can be used to detect when a `BranchManager`'s branch is
 * created or moved.
 * </p>
 *
 * @version V1.0 07.08.2013
 * @author SillyFreak
 */
trait BranchListener extends EventListener {
  /**
   * <p>
   * Called when a new branch is created, either manually or by receiving a branch update.
   * </p>
   *
   * @param mgr the BranchManager a branch is added to
   * @param branch the branch being created
   * @param head the initial tip of the new branch
   */
  def branchCreated(engine: Engine, branch: String, head: State): Unit = {}

  /**
   * <p>
   * Called when an existing branch is moved, either manually or by receiving a branch update.
   * </p>
   *
   * @param mgr the BranchManager the branch belongs to
   * @param branch the branch being moved
   * @param prevHead the previous tip of the branch
   * @param newHead the new tip of the branch
   */
  def branchMoved(engine: Engine, branch: String, prevHead: State, newHead: State): Unit = {}

  /**
   * <p>
   * Called when a branch is deleted.
   * </p>
   *
   * @param mgr the BranchManager a branch is deleted from
   * @param branch the branch being deleted
   * @param prevHead the tip of the new branch previous to deleting it
   */
  def branchDeleted(engine: Engine, branch: String, prevHead: State): Unit = {}
}
