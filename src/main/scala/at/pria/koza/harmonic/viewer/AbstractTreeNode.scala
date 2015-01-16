/**
 * AbstractTreeNode.scala
 *
 * Created on 11.08.2013
 */

package at.pria.koza.harmonic.viewer

import scala.collection.{ immutable, mutable }
import scala.collection.JavaConversions._

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
abstract class AbstractTreeNode[E <: TreeNode](val parent: TreeNode) extends TreeNode with Seq[E] {
  //abstract members

  protected def childSeq: Seq[E]

  //Seq

  def iterator: Iterator[E] = childSeq.iterator

  def apply(idx: Int): E = childSeq(idx)
  def length: Int = childSeq.length

  //TreeNode

  override def getParent(): TreeNode = parent

  def allowsChildren: Boolean = childSeq != null
  override def getAllowsChildren(): Boolean = allowsChildren

  def childAt(index: Int): E =
    if (!allowsChildren) throw new IndexOutOfBoundsException()
    else childSeq(index)
  override def getChildAt(childIndex: Int): E = childAt(childIndex)

  def childCount: Int = if (!allowsChildren) 0 else childSeq.length
  override def getChildCount(): Int = childCount

  def indexOf(node: TreeNode): Int = if (!allowsChildren) -1 else childSeq.indexOf(node)
  override def getIndex(node: TreeNode): Int = indexOf(node)

  def leaf: Boolean = !allowsChildren || childCount == 0
  override def isLeaf(): Boolean = leaf

  override def children(): Enumeration[E] = (if (!allowsChildren) Seq.empty else childSeq).iterator

  //additional members

  protected[viewer] def path: immutable.Seq[Object] = {
    var path = List[Object]()
    var node: TreeNode = this
    while (node != null) {
      path = node :: path
      node = node.getParent()
    }
    path
  }
  protected[viewer] def pathArray: Array[Object] = {
    val p = path
    val result = new Array[Object](p.length)
    p.copyToArray(result)
    result
  }
}
