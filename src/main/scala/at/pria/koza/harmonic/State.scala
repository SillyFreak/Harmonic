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
abstract sealed class State(val id: Long, val list: List[StateNode]) {
  def root = id == 0l
  def engineId = (id >> 32).toInt
  def state: StateNode
  def parent: State
  def parentId = state.parentId
  def action = state.action
  override def toString(): String = "State@%016X".format(id)
}

case object RootState extends State(0l, Nil) {
  override def state = throw new NoSuchElementException
  override def parent = throw new NoSuchElementException
}

case class DerivedState(override val state: StateNode, override val parent: State) extends State(state.id, state :: parent.list)
