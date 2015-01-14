/**
 * StateTreeModel.scala
 *
 * Created on 11.08.2013
 */

package at.pria.koza.harmonic.viewer

import java.util.HashMap
import java.util.Map

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
  private val nodes = new HashMap[Long, StateNode]()
  private val _root = new StateNode()

  override def getRoot(): StateNode = {
    return _root;
  }

  def resolve(state: State): StateNode = {
    val id = state.getId()
    var result = nodes.get(id)
    if (result == null) {
      result = new StateNode(this, state)
      nodes.put(id, result)
    }
    result
  }

  override protected[viewer] def fireTreeNodesInserted(source: Object, path: Array[Object], childIndices: Array[Int], children: Array[Object]): Unit = {
    super.fireTreeNodesInserted(source, path, childIndices, children);
  }

  override protected[viewer] def fireTreeNodesChanged(source: Object, path: Array[Object], childIndices: Array[Int], children: Array[Object]): Unit = {
    super.fireTreeNodesChanged(source, path, childIndices, children);
  }
}
