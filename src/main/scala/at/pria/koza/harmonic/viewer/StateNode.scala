/**
 * StateNode.scala
 *
 * Created on 11.08.2013
 */

package at.pria.koza.harmonic.viewer

import scala.collection.mutable
import scala.collection.JavaConversions._

import java.util.ArrayList
import java.util.List

import javax.swing.tree.TreeNode

import at.pria.koza.harmonic.State

/**
 * <p>
 * {@code StateNode}
 * </p>
 *
 * @version V0.0 11.08.2013
 * @author SillyFreak
 */
class StateNode(val state: State, val parent: StateNode, source: StateTreeModel) extends AbstractTreeNode[StateNode](mutable.ListBuffer(), parent) {

  private val childIndices = if (parent == null) null else Array(parent._children.size - 1)
  private val childObjects = if (parent == null) null else Array[Object](this)
  private val _path = if (parent == null) null else parent.path
  val labels = mutable.SortedSet[String]()

  private[viewer] def this() = this(null, null, null)

  private[viewer] def this(model: StateTreeModel, state: State) = {
    this(
      state,
      if (state.id == 0l) model.getRoot() else model.resolve(state.parent),
      model)
    model.fireTreeNodesInserted(source, path, childIndices, childObjects)
  }

  def fireChanged(): Unit = source.fireTreeNodesChanged(source, path, childIndices, childObjects)

  override def getParent(): TreeNode = parent

  override def toString(): String = {
    if (state == null) return "<root>"
    val sb = new StringBuilder
    val it = labels.iterator
    while (it.hasNext) {
      val lbl = it.next();
      if (lbl.startsWith("branch:")) sb += '[' ++= lbl.substring(7) += ']'
      else sb ++= lbl
      sb += ' '
    }
    if (state.id == 0l) sb ++= state.engine.toString()
    else sb ++= state.toString()

    sb.toString()
  }
}
