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
  object states {
    private var states = immutable.Map[Long, State]()
    def map: immutable.Map[Long, State] = states

    def contains(id: Long): Boolean = states.contains(id)

    def get(id: Long): Option[State] = states.get(id)
    def apply(id: Long): State = states(id)
    private def update(id: Long, state: State) = {
      if (contains(id)) throw new IllegalArgumentException("can't redefine a state")
      states = states.updated(id, state)
      fireStateAdded(state)
    }
    def +=(state: State): Unit = this(state.id) = state

    private var _nextStateId: Long = (id & 0xFFFFFFFFl) << 32

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

    private val listeners = mutable.ListBuffer[StateListener]()

    def addListener(l: StateListener): Unit = listeners += l
    def removeListener(l: StateListener): Unit = listeners -= l

    private[harmonic] def fireStateAdded(state: State): Unit =
      fire(listeners) { _.stateAdded(state) }
  }

  object entities {
    private var nextEntityId: Int = 0;

    private val entities = mutable.Map[Int, Entity]()

    def contains(id: Int): Boolean = entities.contains(id)

    def get(id: Int): Option[Entity] = entities.get(id)
    def apply(id: Int): Entity = entities(id)

    /**
     * <p>
     * Adds an entity to this engine, assigning it a unique id.
     * </p>
     *
     * @param entity the entity to register in this engine
     */
    def +=(entity: Entity): Unit = new RegisterEntity(entity)()

    private class RegisterEntity(entity: Entity) extends Modification {
      protected[this] override def apply0(): Unit = {
        if (contains(nextEntityId)) throw new IllegalArgumentException("can't redefine an entity")
        entities(nextEntityId) = entity
        entity.engine(Engine.this, nextEntityId)
        nextEntityId += 1
      }

      override def revert(): Unit = {
        entities -= entity.id
        entity.engine(null, -1)
        nextEntityId -= 1
      }
    }
  }

  object head extends Ref {
    private var head: State = new RootState(Engine.this)
    override def state = head

    /**
     * <p>
     * Moves this engine's head to the given state.
     * </p>
     *
     * @param head the engine's new head state
     */
    def update(head: State): Unit = {
      if (head == null)
        throw new IllegalArgumentException()

      //common predecessor
      val pred = this.head.commonPredecessor(head)

      //roll back to pred
      {
        var current = this.head
        while (current != pred) {
          current.asInstanceOf[DerivedState].revert()
          current = current.parent
        }
      }

      //move forward to new head
      {
        var states = immutable.List[State]()
        var current = head
        while (current != pred) {
          states = current :: states
          current = current.parent
        }
        states.foreach {
          _.asInstanceOf[DerivedState].apply()
        }
      }

      //set new head
      val old = this.head
      this.head = head
      fireHeadMoved(old, this.head)
    }

    private val listeners = mutable.ListBuffer[HeadListener]()

    def addListener(l: HeadListener): Unit = listeners += l
    def removeListener(l: HeadListener): Unit = listeners -= l

    private[harmonic] def fireHeadMoved(prevHead: State, newHead: State): Unit =
      fire(listeners) { _.headMoved(prevHead, newHead) }
  }

  val config: PolybufConfig = new PolybufConfig()
  def addIO[T <: PolybufSerializable](io: IOFactory[T]): Unit =
    config.add(io.getIO(this))
  def getIO(typeID: Int): Option[PolybufIO[_ <: PolybufSerializable]] =
    config.get(typeID)

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

  private[harmonic] def fire[T <: EventListener, U](listeners: Seq[T])(action: T => U): Unit =
    listeners.synchronized { listeners.reverseIterator.foreach(action) }

  override def toString(): String = "%s@%08X".format(getClass().getSimpleName(), id)
}
