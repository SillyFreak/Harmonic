/**
 * StateTreeModel.scala
 *
 * Created on 11.08.2013
 */

package at.pria.koza.harmonic.viewer

import scala.collection.mutable

import javax.swing.tree.DefaultTreeModel

import at.pria.koza.harmonic.State

/**
 * <p>
 * {@code StateTreeModel}
 * </p>
 *
 * @version V0.0 11.08.2013
 * @author SillyFreak
 */
class StateTreeModel extends DefaultTreeModel(null) {
  private val nodes = mutable.Map[Long, StateNode]()
  private val _root = new StateNode()

  override def getRoot(): StateNode = _root

  def resolve(state: State): StateNode =
    nodes.getOrElseUpdate(state.id, new StateNode(this, state))

  override protected[viewer] def fireTreeNodesInserted(source: Object, path: Array[Object], childIndices: Array[Int], children: Array[Object]): Unit =
    super.fireTreeNodesInserted(source, path, childIndices, children)

  override protected[viewer] def fireTreeNodesChanged(source: Object, path: Array[Object], childIndices: Array[Int], children: Array[Object]): Unit =
    super.fireTreeNodesChanged(source, path, childIndices, children)
}
