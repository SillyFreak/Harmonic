/**
 * HarmonicViewer.scala
 *
 * Created on 11.08.2013
 */

package at.pria.koza.harmonic.viewer

import scala.collection.mutable
import scala.language.implicitConversions

import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

import at.pria.koza.harmonic.BranchListener
import at.pria.koza.harmonic.Engine
import at.pria.koza.harmonic.HeadListener
import at.pria.koza.harmonic.State
import at.pria.koza.harmonic.StateListener
import at.pria.koza.harmonic.viewer.HarmonicViewer._

private object HarmonicViewer {
  @inline implicit def payload(x: DefaultMutableTreeNode): Payload = x.getUserObject().asInstanceOf[Payload]

  class Payload(val state: State) {
    val labels = mutable.SortedSet[String]()
    override def toString(): String =
      state.toString() + (if (labels.isEmpty) "" else labels.mkString(": ", ", ", ""))
  }
}

/**
 * <p>
 * {@code HarmonicViewer}
 * </p>
 *
 * @version V0.0 11.08.2013
 * @author SillyFreak
 */
@SerialVersionUID(1)
class HarmonicViewer extends JPanel(new BorderLayout()) {
  private val root = new DefaultMutableTreeNode()
  private val states = new DefaultTreeModel(root)
  private val statesTree = new JTree(states)

  //TODO when automatically showing new states works, this can be set to false again
  statesTree.setRootVisible(true)
  add(new JScrollPane(statesTree))

  private val nodes = mutable.Map[Long, DefaultMutableTreeNode]()
  def resolve(state: State): DefaultMutableTreeNode =
    nodes.getOrElseUpdate(state.id, {
      val parent = if (state.root) root else resolve(state.parent)
      val node = new DefaultMutableTreeNode(new Payload(state))
      states.insertNodeInto(node, parent, parent.getChildCount())
      node
    })

  def listenTo(engine: Engine): Unit = {
    engine.States.addListener(Listener)
    engine.Head.addListener(Listener)
    engine.Branches.addListener(Listener)

    for (state <- engine.states.values)
      resolve(state)

    val head = resolve(engine.head)
    head.labels += "<HEAD>"
    states.nodeChanged(head)
    makeVisible(head)

    for (branch <- engine.Branches.branchIterator) {
      val tip = resolve(branch.tip)
      head.labels += "branch:" + branch.name
      states.nodeChanged(tip)
    }
  }

  //TODO
  private def makeVisible(node: DefaultMutableTreeNode): Unit = ()

  private object Listener extends HeadListener with StateListener with BranchListener {
    override def headMoved(prevHead: State, newHead: State): Unit = {
      val prevNode = resolve(prevHead)
      prevNode.labels -= "<HEAD>"
      states.nodeChanged(prevNode)
      val newNode = resolve(newHead)
      newNode.labels += "<HEAD>"
      states.nodeChanged(newNode)
    }

    override def stateAdded(state: State): Unit = {
      val node = resolve(state)
      makeVisible(node)
    }

    override def branchCreated(engine: Engine, branch: String, tip: State): Unit = {
      val node = resolve(tip)
      node.labels += "branch:" + branch
      states.nodeChanged(node)
    }

    override def branchMoved(engine: Engine, branch: String, prevTip: State, newTip: State): Unit = {
      val prevNode = resolve(prevTip)
      prevNode.labels -= "branch:" + branch
      states.nodeChanged(prevNode)
      val newNode = resolve(newTip)
      newNode.labels += "branch:" + branch
      states.nodeChanged(newNode)
    }

    override def branchDeleted(engine: Engine, branch: String, tip: State): Unit = {
      val node = resolve(tip)
      node.labels -= "branch:" + branch
      states.nodeChanged(node)
    }
  }
}
