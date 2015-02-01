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

  private def fire[T <: EventListener, U](listeners: Seq[T])(action: T => U): Unit =
    listeners.synchronized { listeners.reverseIterator.foreach(action) }

  val config: PolybufConfig = new PolybufConfig()
  def addIO[T <: PolybufSerializable](io: IOFactory[T]): Unit =
    config.add(io.getIO(this))
  def getIO(typeID: Int): Option[PolybufIO[_ <: PolybufSerializable]] =
    config.get(typeID)

  //States convenience members
  def states: immutable.Map[Long, State] = States.map

  object States {
    private[Engine] var map = immutable.Map[Long, State](0l -> RootState)

    def contains(id: Long): Boolean = map.contains(id)

    def get(id: Long): Option[State] = map.get(id)
    def apply(id: Long): State = map(id)
    private[harmonic] def +=(state: StateNode): State = {
      if (contains(state.id)) throw new IllegalArgumentException("can't redefine a state")
      get(state.parentId) match {
        case Some(parent) =>
          val newState = DerivedState(state, parent)
          map = map.updated(state.id, newState)
          fireStateAdded(newState)
          newState
        case None =>
          throw new IllegalArgumentException("parent state not known in engine")
      }
    }

    private var _nextStateId: Long = (id & 0xFFFFFFFFl) << 32

    /**
     * <p>
     * Returns the next ID to be assigned to a state created by this engine.
     * </p>
     *
     * @return the next ID to be used for a state created by this engine
     */
    private[harmonic] def nextStateId(): Long = {
      _nextStateId += 1
      _nextStateId
    }

    private val listeners = mutable.ListBuffer[StateListener]()

    def addListener(l: StateListener): Unit = listeners += l
    def removeListener(l: StateListener): Unit = listeners -= l

    private[harmonic] def fireStateAdded(state: State): Unit =
      fire(listeners) { _.stateAdded(state) }
  }

  object Entities {
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
    private[harmonic] def +=(entity: Entity): Unit = new RegisterEntity(entity)()

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

  //Head convenience members
  def head = Head()
  def head_=(head: State) = Head() = head

  object Head {
    private[Engine] var head: List[(State, Action)] = (states(0l), null) :: Nil
    def state = head.head._1

    def apply() = state

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

      val old = state

      //common predecessor
      val pred =
        commonTail(state.list, head.list) match {
          case StateNode(id, _, _) :: _ => id
          case Nil                      => 0l
        }

      //roll back to pred
      this.head = this.head.dropWhile {
        case (state, action) =>
          if (state.id == pred) {
            false
          } else {
            action.revert()
            true
          }
      }

      //move forward to new head
      def forward(head: List[(State, Action)], state: State): List[(State, Action)] =
        if (state.id == pred) {
          head
        } else {
          //get the tail first, otherwise deserializing won't work
          val tail = forward(head, state.parent)
          val action = state.state.deserializedAction(Engine.this)
          action.apply()
          (state, action) :: tail
        }
      this.head = forward(this.head, head)

      fireHeadMoved(old, state)
    }

    private val listeners = mutable.ListBuffer[HeadListener]()

    def addListener(l: HeadListener): Unit = listeners += l
    def removeListener(l: HeadListener): Unit = listeners -= l

    private[harmonic] def fireHeadMoved(prevHead: State, newHead: State): Unit =
      if (prevHead != newHead)
        fire(listeners) { _.headMoved(prevHead, newHead) }
  }

  //Branches convenience members
  def currentBranch = Branches.currentBranch
  def currentBranch_=(branch: Branches.Branch) = Branches.currentBranch = branch

  object Branches {
    class Branch private[Branches] (val name: String, private var _tip: State) {
      def tip: State = _tip
      def tip_=(newTip: State): State = {
        val oldTip = _tip
        _tip = newTip
        if (_currentBranch == this) Engine.this.head = newTip
        fireBranchMoved(Engine.this, name, oldTip, newTip)
        oldTip
      }

      override def toString(): String =
        "%s@%016X".format(name, head.id)
    }

    private val branches = mutable.Map[String, Branch]()
    def branchIterator: Iterator[Branch] = branches.values.iterator
    def branch(name: String): Option[Branch] = branches.get(name)

    //put the root
    private var _currentBranch: Branch = null
    def currentBranch = _currentBranch

    def currentBranch_=(branch: Branch): Unit = {
      head = branch.tip
      _currentBranch = branch
    }

    //listeners

    private val listeners = mutable.ListBuffer[BranchListener]()

    def addListener(l: BranchListener): Unit = listeners += l
    def removeListener(l: BranchListener): Unit = listeners -= l

    private[harmonic] def fireBranchCreated(engine: Engine, branch: String, tip: State): Unit =
      fire(listeners) { _.branchCreated(engine, branch, tip) }

    private[harmonic] def fireBranchMoved(engine: Engine, branch: String, prevTip: State, newTip: State): Unit =
      if (prevTip != newTip)
        fire(listeners) { _.branchMoved(engine, branch, prevTip, newTip) }

    private[harmonic] def fireBranchDeleted(engine: Engine, branch: String, tip: State): Unit =
      fire(listeners) { _.branchDeleted(engine, branch, tip) }

    //branch mgmt

    def createBranchHere(name: String): Branch =
      createBranch(name, head)

    def createBranch(name: String, tip: State): Branch = {
      if (branches.contains(name)) throw new IllegalArgumentException("branch already exists")
      val branch = new Branch(name, tip)
      branches(name) = branch
      fireBranchCreated(Engine.this, name, tip)
      branch
    }

    def deleteBranch(branch: Branch): Unit = {
      if (_currentBranch == branch) throw new IllegalArgumentException("can't delete curent branch")
      branches.remove(branch.name) match {
        case Some(_) =>
        case None    => assert(false)
      }
      fireBranchDeleted(Engine.this, branch.name, branch.tip)
    }
  }

  def execute[T <: Action](action: T): T = {
    head = States += new StateNode(States.nextStateId(), head.id, action)(this)
    if (Branches.currentBranch != null)
      Branches.currentBranch.tip = head
    Head.head.head._2.asInstanceOf[T]
  }

  override def toString(): String = "%s@%08X".format(getClass().getSimpleName(), id)
}
