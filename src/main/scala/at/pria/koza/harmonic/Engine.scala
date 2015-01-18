/**
 * Engine.scala
 *
 * Created on 14.05.2013
 */

package at.pria.koza.harmonic

import scala.collection.{ immutable, mutable }

import java.lang.String._
import java.util.EventListener
import java.util.Random

import at.pria.koza.polybuf.PolybufConfig
import at.pria.koza.polybuf.PolybufIO
import at.pria.koza.polybuf.PolybufSerializable

/**
 * <p>
 * The class {@code Engine} represents a graph of objects that describes the application. The Engine's State is
 * modified by Actions.
 * </p>
 *
 * @version V1.0 26.07.2013
 * @author SillyFreak
 */
object Engine {
  private lazy val random = new Random()

  private def nextNonZeroInt(): Int = {
    var result: Int = 0
    do {
      result = random.nextInt()
    } while (result == 0)
    result
  }
}

/**
 * <p>
 * Creates an engine with the given ID. Spectating engines have an ID equal to zero.
 * </p>
 * <p>
 * Returns this engine's ID. An engine that is only spectating (i.e. receiving actions, but not sending any)
 * may have an ID of 0. Other engines have a non-zero random 32 bit ID.
 * </p>
 * <p>
 * This ID is used to prevent conflicts in IDs of states created by this engine: Instead of assigning random
 * IDs to states and hoping that no state IDs in an engine's execution ever clash, random IDs are only assigned
 * to engines, and states get IDs based on these. As the set of engines is relatively stable during the
 * execution of an application, as opposed to the set of states, this scheme is safer.
 * </p>
 *
 * @param id the engine's ID.
 */
class Engine(val id: Int) {
  private var _entities = immutable.Map[Int, Entity]()
  def entities: immutable.Map[Int, Entity] = _entities
  def entity(id: Int): Entity =
    //TODO change interface
    _entities.get(id) match {
      case Some(e) => e
      case None    => null
    }

  private var _states = immutable.Map[Long, State]()
  def states: immutable.Map[Long, State] = _states
  def state(id: Long): State =
    //TODO change interface
    _states.get(id) match {
      case Some(e) => e
      case None    => null
    }

  val config: PolybufConfig = new PolybufConfig()
  def addIO[T <: PolybufSerializable](io: IOFactory[T]): Unit =
    config.add(io.getIO(this))
  def getIO(typeID: Int): Option[PolybufIO[_ <: PolybufSerializable]] =
    config.get(typeID)

  private val stateListeners = mutable.ListBuffer[StateListener]()
  private val headListeners = mutable.ListBuffer[HeadListener]()

  private var _nextStateId: Long = (id & 0xFFFFFFFFl) << 32
  private var _nextEntityId: Int = 0;

  private var _head: State = new RootState(this)
  def head = _head

  /**
   * <p>
   * Creates an engine.
   * </p>
   *
   * @param spectating whether this engine will only spectate or also execute actions
   */
  def this(spectating: Boolean) =
    this(if (spectating) 0 else Engine.nextNonZeroInt())

  /**
   * <p>
   * Creates a non-spectating engine.
   * </p>
   */
  def this() =
    this(false)

  def addStateListener(l: StateListener): Unit = stateListeners += l
  def removeStateListener(l: StateListener): Unit = stateListeners -= l

  def addHeadListener(l: HeadListener): Unit = headListeners += l
  def removeHeadListener(l: HeadListener): Unit = headListeners -= l

  private[harmonic] def fire[T <: EventListener, U](listeners: Seq[T])(action: T => U): Unit =
    listeners.synchronized { listeners.reverseIterator.foreach(action) }

  private[harmonic] def fireStateAdded(state: State): Unit =
    fire(stateListeners) { _.stateAdded(state) }

  private[harmonic] def fireHeadMoved(prevHead: State, newHead: State): Unit =
    fire(headListeners) { _.headMoved(prevHead, newHead) }

  /**
   * <p>
   * Returns the next ID to be assigned to a state created by this engine.
   * </p>
   *
   * @return the next ID to be used for a state created by this engine
   */
  def nextStateId(): Long = {
    _nextStateId += 1
    _nextStateId
  }

  /**
   * <p>
   * Moves this engine's head to the given state.
   * </p>
   *
   * @param head the engine's new head state
   */
  def setHead(head: State): Unit = {
    if (head == null)
      throw new IllegalArgumentException()

    //common predecessor
    val pred = _head.commonPredecessor(head)

    //roll back to pred
    var current = _head
    while (current != pred) {
      current.asInstanceOf[DerivedState].revert()
      current = current.asInstanceOf[DerivedState].parent
    }

    //move forward to new head
    var states = immutable.List[State]()
    current = head
    while (current != pred) {
      states = current :: states
      current = current.asInstanceOf[DerivedState].parent
    }
    states.foreach {
      _.asInstanceOf[DerivedState].apply()
    }

    //set new head
    val old = _head
    _head = head
    fireHeadMoved(old, _head)
  }

  /**
   * <p>
   * Adds an entity to this engine, assigning it a unique id.
   * </p>
   *
   * @param entity the entity to register in this engine
   */
  def putEntity(entity: Entity): Unit = new RegisterEntity(entity)()

  /**
   * <p>
   * Adds a state to this engine.
   * </p>
   *
   * @param state the state to be added
   */
  def putState(state: State): Unit = {
    val id = state.id
    if (_states.contains(id)) throw new IllegalStateException()
    _states = _states.updated(id, state)
    fireStateAdded(state)
  }

  override def toString(): String = "%s@%08X".format(getClass().getSimpleName(), id)

  private class RegisterEntity(entity: Entity) extends Modification {
    protected[this] override def apply0(): Unit = {
      val id = _nextEntityId
      _nextEntityId += 1
      if (_entities.contains(id)) throw new IllegalStateException()
      entity.engine(Engine.this, id)
      _entities = _entities.updated(id, entity)
    }

    override def revert(): Unit = {
      _entities = _entities.filterKeys { _ != entity.id }
      entity.engine(null, -1)
      _nextEntityId -= 1
    }
  }
}
