/**
 * Engine.scala
 *
 * Created on 14.05.2013
 */

package at.pria.koza.harmonic

import java.lang.String._
import java.util.Collections._

import java.util.ArrayList
import java.util.Deque
import java.util.HashMap
import java.util.LinkedList
import java.util.List
import java.util.ListIterator
import java.util.Map
import java.util.Random

import at.pria.koza.polybuf.PolybufConfig

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

  private def getRandom(): Random = random

  private def nextNonZeroInt(): Int = {
    val r = getRandom()
    var result: Int = 0
    do {
      result = r.nextInt()
    } while (result != 0)
    result
  }
}

/**
 * <p>
 * Creates an engine with the given ID. Spectating engines have an ID equal to zero.
 * </p>
 *
 * @param id the engine's ID.
 */
class Engine(id: Int) {
  private val entities = new HashMap[Integer, Entity]()
  private val entitiesView = unmodifiableMap[Integer, Entity](entities)
  private val states = new HashMap[Long, State]()
  private val statesView = unmodifiableMap[Long, State](states)
  private val config = new PolybufConfig()
  private val stateListeners = new ArrayList[StateListener]()
  private val headListeners = new ArrayList[HeadListener]()

  private var _nextStateId: Long = (id & 0xFFFFFFFFl) << 32
  private var _nextEntityId: Int = 0;

  private var _head: State = new State(this)

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

  def getConfig(): PolybufConfig = config

  def addStateListener(l: StateListener): Unit =
    stateListeners.add(l)

  def removeStateListener(l: StateListener): Unit =
    stateListeners.remove(l)

  def addHeadListener(l: HeadListener): Unit =
    headListeners.add(l)

  def removeHeadListener(l: HeadListener): Unit =
    headListeners.remove(l)

  private[harmonic] def fireStateAdded(state: State): Unit = {
    stateListeners.synchronized {
      val it = stateListeners.listIterator(stateListeners.size())
      while (it.hasPrevious()) {
        it.previous().stateAdded(state)
      }
    }
  }

  private[harmonic] def fireHeadMoved(prevHead: State, newHead: State): Unit = {
    headListeners.synchronized {
      val it = headListeners.listIterator(headListeners.size())
      while (it.hasPrevious()) {
        it.previous().headMoved(prevHead, newHead)
      }
    }
  }

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
   * @return this engine's ID
   */
  def getId(): Int = id

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
    val pred = _head.getCommonPredecessor(head)

    //roll back to pred
    var current = _head
    while (current != pred) {
      current.revert()
      current = current.getParent()
    }

    //move forward to new head
    val states = new LinkedList[State]();
    current = head
    while (current != pred) {
      states.addFirst(current)
      current = current.getParent()
    }
    for (state <- states.asInstanceOf[Iterable[State]]) { //TODO I think this fails at runtime
      state.apply()
    }

    //set new head
    val old = _head
    _head = head
    fireHeadMoved(old, _head)
  }

  /**
   * <p>
   * Returns this engine's head state.
   * </p>
   *
   * @return this engine's head state
   */
  def getHead(): State = _head

  /**
   * <p>
   * Adds an entity to this engine, assigning it a unique id.
   * </p>
   *
   * @param entity the entity to register in this engine
   */
  def putEntity(entity: Entity): Unit =
    new RegisterEntity(entity).apply()

  /**
   * <p>
   * Returns the entity associated with the given ID.
   * </p>
   *
   * @param id the ID to resolve
   * @return the entity that is associated with the ID, or {@code null}
   */
  def getEntity(id: Int): Entity = entities.get(id)

  def getEntities(): Map[Integer, Entity] = entitiesView

  /**
   * <p>
   * Adds a state to this engine.
   * </p>
   *
   * @param state the state to be added
   */
  def putState(state: State): Unit = {
    val id = state.getId()
    if (states.containsKey(id)) throw new IllegalStateException()
    states.put(id, state)
    fireStateAdded(state)
  }

  /**
   * <p>
   * Returns the state associated with the given ID.
   * </p>
   *
   * @param id the ID to resolve
   * @return the state that is associated with the ID, or {@code null}
   */
  def getState(id: Long): State = states.get(id);

  def getStates(): Map[Long, State] = statesView

  override def toString(): String = format("%s@%08X", (getClass().getSimpleName(), id))

  private class RegisterEntity(entity: Entity) extends Modification {
    private[harmonic] override def apply0(): Unit = {
      val id = _nextEntityId
      _nextEntityId += 1
      entity.setEngine(Engine.this, id);
      entities.put(id, entity);
    }

    private[harmonic] override def revert(): Unit = {
      entities.remove(entity.getId());
      entity.setEngine(null, -1);
      _nextEntityId -= 1;
    }
  }
}
