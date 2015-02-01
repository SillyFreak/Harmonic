/**
 * State.scala
 *
 * Created on 31.01.2015
 */

package at.pria.koza.harmonic

/**
 * <p>
 * {@code State}
 * </p>
 *
 * @version V0.0 31.01.2015
 * @author SillyFreak
 */
case class State(val id: Long, val list: List[StateNode]) {
  def root = list.isEmpty
  def engineId: Int = (id >> 32).toInt
  //these all throw NSEEx's the state is root
  def state = list.head
  def parentId = state.parentId
  def action = state.action
  lazy val parent = new State(parentId, list.tail)

  def this() = this(0l, Nil)

  override def toString(): String =
    if (root) "Root"
    else state.toString()
}
