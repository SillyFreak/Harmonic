/**
 * State.scala
 *
 * Created on 31.01.2015
 */

package at.pria.koza.harmonic

/**
 * <p>
 * `State` wraps a list of `StateNode`s. While a StateNode only stores information about one state, the State also
 * contains information about the state's ancestors. It is therefore the main abstraction for dealing with engines'
 * states. Informally, "state" can refer to both a `State` and a `StateNode`.
 * </p>
 *
 * @version V0.0 31.01.2015
 * @author SillyFreak
 */
case class State private[State] (val id: Long, val list: List[StateNode]) {
  def root = list.isEmpty
  def engineId: Int = (id >> 32).toInt
  //these all throw NSEEx's the state is root
  def state = list.head
  def parentId = state.parentId
  def action = state.action
  lazy val parent = new State(parentId, list.tail)

  def this() = this(0l, Nil)

  def this(list: List[StateNode]) = this(list.head.id, list)

  override def toString(): String =
    if (root) "Root"
    else state.toString()
}
