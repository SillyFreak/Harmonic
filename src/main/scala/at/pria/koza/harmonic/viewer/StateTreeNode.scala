/**
 * StateTreeNode.scala
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
 * {@code StateTreeNode}
 * </p>
 *
 * @version V0.0 11.08.2013
 * @author SillyFreak
 */
class StateTreeNode(val state: State, parent: StateTreeNode, source: StateTreeModel) extends AbstractTreeNode[StateTreeNode](parent) {
  val labels = mutable.SortedSet[String]()

  //AbstractTreeNode

  override protected val childSeq = mutable.ListBuffer[StateTreeNode]()

  //used for fireTreeNodes*
  private val childIndices = if (parent == null) null else Array(parent.childCount)

  private[viewer] def this() = this(null, null, null)

  private[viewer] def this(model: StateTreeModel, state: State) = {
    this(
      state,
      if (state.root) model.getRoot()
      else model.resolve(state.parent),
      model)
    parent.childSeq += this
    source.nodesWereInserted(parent, childIndices)
  }

  def fireChanged(): Unit =
    source.nodesChanged(parent, childIndices)

  override def toString(): String = {
    if (state == null) return "<root>"
    val sb = new StringBuilder
    sb ++=
      labels.map { lbl =>
        if (lbl.startsWith("branch:")) '[' + lbl.substring(7) + ']'
        else lbl
      }.mkString("{", ", ", "} ")

    sb ++= state.toString()

    sb.toString()
  }
}
