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
    val it = engine.getStates().values().iterator()
    while (it.hasNext())
      states.resolve(it.next())

    val head = states.resolve(engine.getHead())
    head.getLabels().add("<HEAD>")
    head.fireChanged()
    makeVisible(head)
  }

  def listenTo(mgr: BranchManager): Unit = {
    listenTo(mgr.getEngine())
    mgr.addBranchListener(listener)

    val it = mgr.getBranches().iterator()
    while (it.hasNext()) {
      val branch = it.next()
      val head = states.resolve(mgr.getBranchTip(branch))
      head.getLabels().add("branch:" + branch)
      head.fireChanged()
    }
  }

  private def makeVisible(node: StateNode): Unit = {
    statesTree.makeVisible(new TreePath(node.getPath()))
  }

  private class Listener extends HeadListener with StateListener with BranchListener {
    def headMoved(prevHead: State, newHead: State): Unit = {
      val prevNode = states.resolve(prevHead)
      prevNode.getLabels().remove("<HEAD>")
      prevNode.fireChanged()
      val newNode = states.resolve(newHead)
      newNode.getLabels().add("<HEAD>")
      newNode.fireChanged()
    }

    def stateAdded(state: State): Unit = {
      val node = states.resolve(state)
      makeVisible(node)
    }

    def branchCreated(mgr: BranchManager, branch: String, head: State): Unit = {
      val headNode = states.resolve(head)
      headNode.getLabels().add("branch:" + branch)
      headNode.fireChanged()
    }

    def branchMoved(mgr: BranchManager, branch: String, prevHead: State, newHead: State): Unit = {
      val prevNode = states.resolve(prevHead)
      prevNode.getLabels().remove("branch:" + branch)
      prevNode.fireChanged()
      val newNode = states.resolve(newHead)
      newNode.getLabels().add("branch:" + branch)
      newNode.fireChanged()
    }

    def branchDeleted(mgr: BranchManager, branch: String, prevHead: State): Unit = {
      val prevNode = states.resolve(prevHead)
      prevNode.getLabels().remove("branch:" + branch)
      prevNode.fireChanged()
    }
  }
}
