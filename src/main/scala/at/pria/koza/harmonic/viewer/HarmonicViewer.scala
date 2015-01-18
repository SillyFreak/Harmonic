/**
 * HarmonicViewer.scala
 *
 * Created on 11.08.2013
 */

package at.pria.koza.harmonic.viewer

import java.awt.BorderLayout
import java.awt.Dimension

import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTree
import javax.swing.tree.TreePath

import at.pria.koza.harmonic.BranchListener
import at.pria.koza.harmonic.BranchManager
import at.pria.koza.harmonic.Engine
import at.pria.koza.harmonic.HeadListener
import at.pria.koza.harmonic.State
import at.pria.koza.harmonic.StateListener

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
  private val listener = new Listener()
  private val states = new StateTreeModel()
  private val statesTree = new JTree(states)

  statesTree.setMinimumSize(new Dimension(100, 0))
  statesTree.setRootVisible(false)

  {
    val statesTreePanel = new JPanel(new BorderLayout())
    statesTreePanel.add(new JScrollPane(statesTree))
    add(statesTreePanel)
  }

  def listenTo(engine: Engine): Unit = {
    engine.addStateListener(listener)
    engine.addHeadListener(listener)
    val it = engine.states.values.iterator
    while (it.hasNext)
      states.resolve(it.next())

    val head = states.resolve(engine.head)
    head.labels.add("<HEAD>")
    head.fireChanged()
    makeVisible(head)
  }

  def listenTo(mgr: BranchManager): Unit = {
    listenTo(mgr.engine)
    mgr.addBranchListener(listener)

    val it = mgr.branches.iterator()
    while (it.hasNext()) {
      val branch = it.next()
      val head = states.resolve(mgr.branchTip(branch))
      head.labels.add("branch:" + branch)
      head.fireChanged()
    }
  }

  private def makeVisible(node: StateNode): Unit =
    statesTree.makeVisible(new TreePath(node.path))

  private class Listener extends HeadListener with StateListener with BranchListener {
    override def headMoved(prevHead: State, newHead: State): Unit = {
      val prevNode = states.resolve(prevHead)
      prevNode.labels.remove("<HEAD>")
      prevNode.fireChanged()
      val newNode = states.resolve(newHead)
      newNode.labels.add("<HEAD>")
      newNode.fireChanged()
    }

    override def stateAdded(state: State): Unit = {
      val node = states.resolve(state)
      makeVisible(node)
    }

    override def branchCreated(mgr: BranchManager, branch: String, head: State): Unit = {
      val headNode = states.resolve(head)
      headNode.labels.add("branch:" + branch)
      headNode.fireChanged()
    }

    override def branchMoved(mgr: BranchManager, branch: String, prevHead: State, newHead: State): Unit = {
      val prevNode = states.resolve(prevHead)
      prevNode.labels.remove("branch:" + branch)
      prevNode.fireChanged()
      val newNode = states.resolve(newHead)
      newNode.labels.add("branch:" + branch)
      newNode.fireChanged()
    }

    override def branchDeleted(mgr: BranchManager, branch: String, prevHead: State): Unit = {
      val prevNode = states.resolve(prevHead)
      prevNode.labels.remove("branch:" + branch)
      prevNode.fireChanged()
    }
  }
}
