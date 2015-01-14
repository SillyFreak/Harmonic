/**
 * StateNode.scala
 *
 * Created on 11.08.2013
 */

package at.pria.koza.harmonic.viewer

import java.util.Collections._

import java.util.ArrayList
import java.util.Iterator
import java.util.List
import java.util.Set
import java.util.TreeSet

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
class StateNode(state: State, parent: StateNode, source: StateTreeModel) extends AbstractTreeNode[StateNode] {

  private val childIndices = if (parent == null) null else Array(parent._children.size() - 1)
  private val childObjects = if (parent == null) null else Array[Object](this)
  private val path = if (parent == null) null else parent.getPath()

  private val _children = new ArrayList[StateNode]()
  private val childrenView = unmodifiableList(_children)
  private val labels = new TreeSet[String]()

  private[viewer] def this() = this(null, null, null)

  private[viewer] def this(model: StateTreeModel, state: State) = {
    this(
      state,
      if (state.getId() == 0l) model.getRoot() else model.resolve(state.getParent()),
      model)
    model.fireTreeNodesInserted(source, path, childIndices, childObjects)
  }

  def getState(): State = state

  def fireChanged(): Unit = source.fireTreeNodesChanged(source, path, childIndices, childObjects)

  def getLabels(): Set[String] = labels

  override def getParent(): TreeNode = parent

  override protected def getChildren(): List[StateNode] = childrenView

  override def toString(): String = {
    if (state == null) return "<root>"
    val sb = new StringBuilder()
    val it = labels.iterator()
    while (it.hasNext()) {
      val lbl = it.next();
      if (lbl.startsWith("branch:")) sb.append('[').append(lbl.substring(7)).append(']')
      else sb.append(lbl)
      sb.append(' ')
    }
    if (state.getId() == 0l) sb.append(state.getEngine().toString())
    else sb.append(state.toString())

    sb.toString()
  }
}
