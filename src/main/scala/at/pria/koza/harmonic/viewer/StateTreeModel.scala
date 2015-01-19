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
class StateTreeModel extends DefaultTreeModel(new StateNode()) {
  override def getRoot(): StateNode = root.asInstanceOf[StateNode]

  private val nodes = mutable.Map[Long, StateNode]()

  def resolve(state: State): StateNode =
    nodes.getOrElseUpdate(state.id, new StateNode(this, state))
}
