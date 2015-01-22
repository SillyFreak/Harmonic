/**
 * BranchManager.scala
 *
 * Created on 29.07.2013
 */

package at.pria.koza.harmonic

import scala.collection.{ mutable, immutable }

import at.pria.koza.harmonic.BranchManager.SyncCallback
import at.pria.koza.harmonic.BranchManager.MetaState
import at.pria.koza.polybuf.PolybufException
import at.pria.koza.polybuf.PolybufIO
import at.pria.koza.polybuf.PolybufInput
import at.pria.koza.polybuf.PolybufOutput
import at.pria.koza.polybuf.PolybufSerializable
import at.pria.koza.polybuf.proto.Polybuf.Obj

import com.google.protobuf.GeneratedMessage.GeneratedExtension

import java.util.EventListener

/**
 * <p>
 * A {@code BranchManager} is a wrapper for an {@link Engine}. It enables easy and efficient synchronization
 * between multiple BranchManagers by managing metadata for the states in the engine, and named branches for which
 * updates may be published between engines. At the moment, a branch manager does not operate on existing and
 * possibly already modified engines. Instead, it creates a new engine and controls all access to it. This may
 * change in the future.
 * </p>
 * <p>
 * This class does not provide network protocols for achieving this ends. It is the backend containing logic to
 * create and process the information necessary for such messages, but does not mandate any specific protocol
 * formats to be used to transport that information.
 * </p>
 *
 * @version V1.0 29.07.2013
 * @author SillyFreak
 */
object BranchManager {
  val BRANCH_DEFAULT = "default"

  trait SyncCallback {
    /**
     * <p>
     * Reports the data needed to call {@link BranchManager#receiveUpdate(int, String, Obj, long...)
     * receiveUpdate()} on the receiving BranchManager.
     * </p>
     */
    def sendUpdateCallback(engine: Int, branch: String, state: Obj, ancestors: Long*): Unit = {}

    /**
     * <p>
     * Reports the data needed to call {@link BranchManager#sendMissing(int, String, long, SyncCallback)
     * sendMissing()} on the sending BranchManager.
     * </p>
     */
    def receiveUpdateCallback(engine: Int, branch: String, ancestor: Long): Unit = {}

    /**
     * <p>
     * Reports the data needed to call {@link BranchManager#receiveMissing(int, String, long, Obj...)
     * receiveMissing()} on the receiving BranchManager.
     * </p>
     */
    def sendMissingCallback(engine: Int, branch: String, state: Long, ancestors: Obj*): Unit = {}
  }

  private[harmonic] class MetaState(mgr: BranchManager, val stateId: Long, val parentId: Long) extends PolybufSerializable with Ref {
    private var _action: Obj = _

    private var _parent: MetaState = _
    def parent = _parent

    private var _state: State = _
    override def state = _state

    //set of engines known to know this meta state
    private[BranchManager] val engines = new mutable.HashSet[Int]()

    /**
     * <p>
     * Used to add states created by the engine managed by this branch manager.
     * </p>
     *
     * @param state the state to be added
     */
    def this(mgr: BranchManager, state: State) = {
      this(
        mgr,
        state.id,
        state match {
          case root: RootState    => 0
          case node: DerivedState => node.parent.id
        })

      _state = state;
      _parent = state match {
        case root: RootState    => null
        case node: DerivedState => mgr.put(node.parent)
      }
      addEngine(mgr.engine.id)
    }

    /**
     * <p>
     * Used to add states received from an engine other than managed by this branch manager.
     * {@linkplain #resolve() Resolving} will be necessary before this MetaState can be used.
     * </p>
     *
     * @param state the protobuf serialized form of the state to be added
     * @param action the action extracted from that protobuf extension
     */
    def this(mgr: BranchManager, state: Obj) = {
      this(
        mgr,
        state.getExtension(State.EXTENSION).getId(),
        state.getExtension(State.EXTENSION).getParent())
      _action = state.getExtension(State.EXTENSION).getAction()
    }

    override def typeId: Int = State.FIELD

    /**
     * <p>
     * Resolves this state. Returns true when the MetaState is now fully initialized, false if it is still not.
     * This method must be called for states received from another engine, as the parent state may not be
     * present at the time it is received. After all necessary ancestor states were received, then resolving
     * will be successful and the state will be added to the underlying engine.
     * </p>
     *
     * @return {@code true} if the MetaState was resolved, so that there is now a corresponding {@link State}
     *         in the underlying engine; {@code false} otherwise
     */
    def resolve(): Boolean = {
      if (_state != null) true
      else {
        assert(stateId != 0)
        if (_parent == null) {
          _parent = mgr.states.get(parentId) match {
            case Some(state) => state
            case None        => null
          }
        }
        if (_parent == null || !_parent.resolve()) false
        else {
          _state = new DerivedState(parent.state, stateId, _action)
          addEngine(mgr.engine.id)
          addEngine(state.engineId)
          true
        }
      }
    }

    def addEngine(id: Int): Unit = {
      //an assuption here is that if this state has an engine marked, all its parents will have it marked too
      //if the state is not resolved, i.e. not attached to its parent, then this assumption could be broken
      //when it is subsequently resolved, so don't allow that
      if (!resolve()) throw new IllegalStateException()
      if (engines.add(id) && _parent != null) _parent.addEngine(id)
    }
  }
}

/**
 * <p>
 * Creates a new branch manager.
 * </p>
 */
class BranchManager(val engine: Engine) extends IOFactory[MetaState] {
  class Branch private[BranchManager] (val name: String) extends Ref {
    private var _head: MetaState = _
    def head: MetaState = _head
    def head(head: MetaState): Unit = _head = head

    override def state = head.state

    override def toString(): String = "%s@%016X".format(name, head.stateId)
  }

  private val branches = mutable.Map[String, Branch]()
  def branchIterator: Iterator[Branch] = branches.values.iterator
  def branch(name: String): Option[Branch] = branches.get(name)

  private val states = mutable.Map[Long, MetaState]()

  private val branchListeners = mutable.ListBuffer[BranchListener]()

  private var _currentBranch = BranchManager.BRANCH_DEFAULT
  def currentBranch = _currentBranch

  def currentBranch(name: String): Unit = {
    branchTip(name) match {
      case Some(tip) =>
        engine.head.update(tip)
        _currentBranch = name
      case None => throw new IllegalArgumentException("can't switch to nonexistant branch")
    }
  }

  //put the root
  createBranch(_currentBranch, engine.states(0l))

  //ctors & misc

  /**
   * <p>
   * Creates a new branch manager
   * </p>
   *
   * @see Engine#Engine()
   */
  def this() = this(new Engine())

  /**
   * <p>
   * Creates a new branch manager.
   * </p>
   *
   * @param spectating whether the engine will only spectate or also execute actions
   *
   * @see Engine#Engine(boolean)
   */
  def this(spectating: Boolean) = this(new Engine(spectating))

  /**
   * <p>
   * Creates a new branch manager.
   * </p>
   *
   * @param id the ID to be used for the engine
   *
   * @see Engine#Engine(int)
   */
  def this(id: Int) = this(new Engine(id))

  //listeners

  def addBranchListener(l: BranchListener): Unit = branchListeners += l
  def removeBranchListener(l: BranchListener): Unit = branchListeners -= l

  private[harmonic] def fire[T <: EventListener, U](listeners: Seq[T])(action: T => U): Unit =
    listeners.synchronized { listeners.reverseIterator.foreach(action) }

  private[harmonic] def fireBranchCreated(mgr: BranchManager, branch: String, head: State): Unit =
    fire(branchListeners) { _.branchCreated(mgr, branch, head) }

  private[harmonic] def fireBranchMoved(mgr: BranchManager, branch: String, prevHead: State, newHead: State): Unit =
    if (prevHead != newHead)
      fire(branchListeners) { _.branchMoved(mgr, branch, prevHead, newHead) }

  private[harmonic] def fireBranchDeleted(mgr: BranchManager, branch: String, prevHead: State): Unit =
    fire(branchListeners) { _.branchDeleted(mgr, branch, prevHead) }

  //branch mgmt

  def createBranchHere(name: String): Unit =
    createBranch(name, branchTip(currentBranch).get)

  def createBranch(name: String, state: State): Unit = {
    if (state.engine != engine) throw new IllegalArgumentException("state is from another engine")
    if (branches.contains(name)) throw new IllegalArgumentException("branch already exists")
    createOrMoveBranch(name, put(state))
  }

  def deleteBranch(name: String): Unit = {
    if (_currentBranch == name) throw new IllegalArgumentException("can't delete curent branch")
    branches.remove(name) match {
      case Some(head) => fireBranchDeleted(this, name, head.head.state)
      case None       => throw new IllegalArgumentException("branch does not exist")
    }
  }

  def branchTip(name: String): Option[State] = branches.get(name).map { _.head.state }

  def branchTip(name: String, newHead: State): State = {
    if (newHead.engine != engine) throw new IllegalArgumentException("newHead is from another engine")
    if (!branches.contains(name)) throw new IllegalArgumentException("branch does not exist")
    createOrMoveBranch(name, put(newHead)).state
  }

  private def createOrMoveBranch(name: String, newHead: MetaState): MetaState = {
    val tip = branches.getOrElseUpdate(name, new Branch(name))
    val oldHead = tip.head
    tip.head(newHead)

    if (_currentBranch.equals(name)) engine.head.update(newHead.state)
    if (oldHead == null) fireBranchCreated(this, name, newHead.state)
    else fireBranchMoved(this, name, oldHead.state, newHead.state)

    oldHead
  }

  def execute[T <: Action](action: T): T = {
    engine.execute(action);
    createOrMoveBranch(_currentBranch, put(engine.head.state))
    action
  }

  //receive branch sync

  /**
   * <p>
   * Receives an update offer from a remote branch manager. The branch to be updated consists of the branch
   * owner's ID in hex (16 digits), a slash and a branch name. The {@code state} contains the full information
   * about the branch's tip as the remote BranchManager knows it, and the {@code ancestors} array contains state
   * IDs that the remote BranchManager thought this BranchManager already knew, as to allow to communicate deltas
   * as small as possible. In the case that this BranchManager was already up to date, or had the parent of the
   * new state, and could therefore update immediately, the return value will be the new state's id. Otherwise,
   * it will be the latest state's id of which the manager knows it's on the branch; likely either an element of
   * the {@code ancestors} array, or {@code 0}.
   * </p>
   *
   * @param engine the id of the offering BranchManager's engine
   * @param name the branch this update belongs to
   * @param state the state being the tip of this update
   * @param ancestors a list of ancestor state IDs the remote branch manager thought this branch manager might
   *            already be aware of; most recent first
   * @return the most recent state id that this BranchManager knows for the given branch; {@code 0} if the branch
   *         is unknown; the {@code state}'s id if the full branch is known
   */
  def receiveUpdate(engine: Int, name: String, state: Obj, ancestors: Seq[Long], callback: SyncCallback): Unit = {
    val newHead = deserialize(state)
    if (newHead.resolve()) {
      //we have all we need
      newHead.addEngine(engine)

      createOrMoveBranch(name, newHead);

    } else {
      //we need additional states
      val l = ancestors.find { states.contains(_) } match {
        case Some(l) => l
        case None    => 0l
      }

      callback.receiveUpdateCallback(this.engine.id, name, 0l)
    }
  }

  /**
   * <p>
   * Receives the missing states for a previous {@link #receiveUpdate(int, String, Obj, long...) receiveUpdate()}
   * call. The BranchManager does not save any transient state between {@code receiveUpdate()} and
   * {@code receiveMissing()}, so some information must be added to the parameters again: the source of the
   * update; and the branch being updated. To find again the state which was already received, the id of the head
   * state of the update must be transmitted again. In addition, a list of states containing the delta between
   * the remote and this BranchManager's branch is transmitted.
   * </p>
   *
   * @param engine the id of the offering BranchManager's engine
   * @param name the branch this update belongs to
   * @param state the id of the state being the tip of this update
   * @param ancestors a list of ancestor states that is missing from the local branch, in chronological order
   */
  def receiveMissing(engine: Int, name: String, state: Long, ancestors: Seq[Obj]): Unit = {
    ancestors.foreach { obj =>
      if (!deserialize(obj).resolve())
        throw new AssertionError()
    }

    val newHead = states.get(state).get
    if (!newHead.resolve()) throw new AssertionError()
    newHead.addEngine(engine)

    createOrMoveBranch(name, newHead)
  }

  //send branch sync

  /**
   * <p>
   * Determines which data has to be sent to the {@link BranchManager} identified by {@code engine} to update the
   * given branch. If there is anything to update, this method provides this data to the caller through
   * {@link SyncCallback#sendUpdateCallback(int, String, Obj, long...) callback.sendUpdateCallback()}.
   * </p>
   *
   * @param engine the engine which should be updated
   * @param name the branch for which updates should be provided
   * @param callback a callback to provide the data to the caller
   */
  def sendUpdate(engine: Int, name: String, callback: SyncCallback): Unit = {
    val head =
      branches.get(name) match {
        case Some(branch) =>
          if (branch.head == null) throw new IllegalArgumentException() //TODO can this even happen?
          else branch.head
        case None => throw new IllegalArgumentException("branch does not exist")
      }
    val state =
      if (engine == 0) {
        null
      } else {
        var _state = head
        while (_state != null && !_state.engines.contains(engine))
          _state = _state.parent
        if (_state == head) return

        head.addEngine(engine)
        _state
      }

    val ancestors = if (state == null) Seq[Long](0) else Seq[Long](state.stateId)
    callback.sendUpdateCallback(this.engine.id, name, serialize(head), ancestors: _*)
  }

  /**
   * <p>
   * Determines which states are missing at the {@link BranchManager} identified by {@code engine} provided the
   * known ancestor. If there is anything to update, this method provides this data to the caller through
   * {@link SyncCallback#sendMissingCallback(int, String, long, Obj...) callback.sendMissingCallback()}.
   * </p>
   *
   * @param engine the engine which should be updated
   * @param name the branch for which updates should be provided
   * @param ancestor the ancestor the remote branch manager reported it knew
   * @param callback a callback to provide the data to the caller
   */
  def sendMissing(engine: Int, name: String, ancestor: Long, callback: SyncCallback): Unit = {
    val head =
      branches.get(name) match {
        case Some(branch) =>
          if (branch.head == null) throw new IllegalArgumentException() //TODO can this even happen?
          else branch.head
        case None => throw new IllegalArgumentException("branch does not exist")
      }

    head.addEngine(engine)
    val headId = head.stateId
    if (headId == ancestor) return

    var ancestors = List[Obj]()
    var state = head.parent
    while (state.stateId != ancestor) {
      ancestors = serialize(state) :: ancestors
      state = state.parent
    }

    callback.sendMissingCallback(this.engine.id, name, headId, ancestors: _*)
  }

  //state mgmt

  private def deserialize(state: Obj): MetaState = {
    try {
      new PolybufInput(engine.config).readObject(state).asInstanceOf[MetaState]
    } catch {
      case ex: PolybufException   => throw new IllegalArgumentException(ex)
      case ex: ClassCastException => throw new IllegalArgumentException(ex)
    }
  }

  private def serialize(state: MetaState): Obj = {
    try {
      new PolybufOutput(engine.config).writeObject(state)
    } catch {
      case ex: PolybufException => throw new IllegalArgumentException(ex)
    }
  }

  private def put(state: State): MetaState =
    states.getOrElseUpdate(state.id, new MetaState(this, state))

  //polybuf

  override def getIO(implicit engine: Engine): PolybufIO[MetaState] = new IO()

  private class IO extends PolybufIO[MetaState] {
    private val delegate = State.getIO(engine)

    override def extension: GeneratedExtension[Obj, _] = delegate.extension

    @throws[PolybufException]
    override def serialize(out: PolybufOutput, instance: MetaState, obj: Obj.Builder): Unit =
      delegate.serialize(out, instance.state, obj)

    @throws[PolybufException]
    override def initialize(in: PolybufInput, obj: Obj): MetaState = {
      val p = obj.getExtension(State.EXTENSION)
      val id = p.getId()
      //handle states already present properly
      states.getOrElseUpdate(id, new MetaState(BranchManager.this, obj))
    }

    @throws[PolybufException]
    override def deserialize(in: PolybufInput, obj: Obj, instance: MetaState): Unit =
      delegate.deserialize(in, obj, instance.state)
  }
}
