/**
 * AbstractTreeNode.scala
 *
 * Created on 11.08.2013
 */

package at.pria.koza.harmonic.viewer

import java.util.Collections._

import java.util.Enumeration
import java.util.LinkedList
import java.util.List

import javax.swing.tree.TreeNode

/**
 * <p>
 * {@code AbstractTreeNode}
 * </p>
 *
 * @version V0.0 11.08.2013
 * @author SillyFreak
 */
abstract class AbstractTreeNode[E <: TreeNode] extends TreeNode {
  protected def getChildren(): List[E]

  override def getParent(): TreeNode

  protected def getPath(): Array[Object] = {
    val path = new LinkedList[Object]()
    var node: TreeNode = this
    while (node != null) {
      path.addFirst(node)
      node = node.getParent()
    }
    path.toArray()
  }

  override def getChildAt(childIndex: Int): TreeNode = {
    val list = getChildren()
    if (list == null) throw new IndexOutOfBoundsException()
    list.get(childIndex)
  }

  override def getChildCount(): Int = {
    val list = getChildren()
    if (list == null) 0 else list.size()
  }

  override def getIndex(node: TreeNode): Int = {
    val list = getChildren()
    if (list == null) return -1
    return list.indexOf(node)
  }

  override def getAllowsChildren(): Boolean = {
    return getChildren() != null
  }

  override def isLeaf(): Boolean = {
    val list = getChildren()
    return list == null || list.isEmpty()
  }

  override def children(): Enumeration[E] = {
    val list = getChildren()
    enumeration(if (list == null) emptyList() else list)
  }
}
