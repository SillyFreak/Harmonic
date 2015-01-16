/**
 * AbstractTreeNode.scala
 *
 * Created on 11.08.2013
 */

package at.pria.koza.harmonic.viewer

import scala.collection.JavaConversions

import java.util.Enumeration

import javax.swing.tree.TreeNode

/**
 * <p>
 * {@code AbstractTreeNode}
 * </p>
 *
 * @version V0.0 11.08.2013
 * @author SillyFreak
 */
abstract class AbstractTreeNode[E <: TreeNode](private[viewer] val _children: Seq[E], parent: TreeNode) extends TreeNode {
  override def getParent(): TreeNode = parent

  protected[viewer] def path: Array[Object] = {
    var path = List[Object]()
    var node: TreeNode = this
    while (node != null) {
      path = node :: path
      node = node.getParent()
    }
    path.toArray(implicitly)
  }

  override def getChildAt(childIndex: Int): TreeNode =
    if (_children == null) throw new IndexOutOfBoundsException()
    else _children(childIndex)

  override def getChildCount(): Int =
    if (_children == null) 0 else _children.size

  override def getIndex(node: TreeNode): Int =
    if (_children == null) -1 else _children.indexOf(node)

  override def getAllowsChildren(): Boolean =
    _children != null

  override def isLeaf(): Boolean =
    _children == null || _children.isEmpty

  override def children(): Enumeration[E] =
    JavaConversions.asJavaEnumeration((if (_children == null) Seq.empty else _children).iterator)
}
