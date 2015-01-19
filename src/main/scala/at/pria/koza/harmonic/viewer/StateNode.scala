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
import at.pria.koza.harmonic.RootState
import at.pria.koza.harmonic.DerivedState

/**
 * <p>
 * {@code StateNode}
 * </p>
 *
 * @version V0.0 11.08.2013
 * @author SillyFreak
 */
class StateNode(val state: State, parent: StateNode, source: StateTreeModel) extends AbstractTreeNode[StateNode](parent) {
  val labels = mutable.SortedSet[String]()

  //AbstractTreeNode

  override protected val childSeq = mutable.ListBuffer[StateNode]()

  //used for fireTreeNodes*
  private val childIndices = if (parent == null) null else Array(parent.childCount)

  private[viewer] def this() = this(null, null, null)

  private[viewer] def this(model: StateTreeModel, state: State) = {
    this(
      state,
      state match {
        case root: RootState    => model.getRoot()
        case node: DerivedState => model.resolve(node.parent)
      },
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

    sb ++= {
      if (state.id == 0l) state.engine.toString()
      else state.toString()
    }

    sb.toString()
  }
}
