/**
 * StateTreeModel.scala
 *
 * Created on 11.08.2013
 */

package at.pria.koza.harmonic.viewer

import scala.collection.mutable

import at.pria.koza.harmonic.State

import javax.swing.tree.DefaultTreeModel

/**
 * <p>
 * {@code StateTreeModel}
 * </p>
 *
 * @version V0.0 11.08.2013
 * @author SillyFreak
 */
@SerialVersionUID(1)
class StateTreeModel extends DefaultTreeModel(new StateTreeNode()) {
  override def getRoot(): StateTreeNode = root.asInstanceOf[StateTreeNode]

  private val nodes = mutable.Map[Long, StateTreeNode]()

  def resolve(state: State): StateTreeNode =
    nodes.getOrElseUpdate(state.id, new StateTreeNode(this, state))
}
