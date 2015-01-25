/**
 * BranchManager.scala
 *
 * Created on 29.07.2013
 */

package at.pria.koza.harmonic

import scala.collection.{ mutable, immutable }

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

  //trait SyncCallback {
  //  /**
  //   * <p>
  //   * Reports the data needed to call {@link BranchManager#receiveUpdate(int, String, Obj, long...)
  //   * receiveUpdate()} on the receiving BranchManager.
  //   * </p>
  //   */
  //  def sendUpdateCallback(engine: Int, branch: String, state: Obj, ancestors: Long*): Unit = {}
  //
  //  /**
  //   * <p>
  //   * Reports the data needed to call {@link BranchManager#sendMissing(int, String, long, SyncCallback)
  //   * sendMissing()} on the sending BranchManager.
  //   * </p>
  //   */
  //  def receiveUpdateCallback(engine: Int, branch: String, ancestor: Long): Unit = {}
  //
  //  /**
  //   * <p>
  //   * Reports the data needed to call {@link BranchManager#receiveMissing(int, String, long, Obj...)
  //   * receiveMissing()} on the receiving BranchManager.
  //   * </p>
  //   */
  //  def sendMissingCallback(engine: Int, branch: String, state: Long, ancestors: Obj*): Unit = {}
  //}
}

/**
 * <p>
 * Creates a new branch manager.
 * </p>
 */
class BranchManager(val engine: Engine) {
  class Branch private[BranchManager] (val name: String, private var _head: StateWrapper) extends Ref {
    def head: StateWrapper = _head
    def head(newHead: State): State = head(engine.wrappers(newHead.id)).state
    private[BranchManager] def head(newHead: StateWrapper): StateWrapper = {
      val oldHead = _head
      _head = newHead
      if (_currentBranch == this) engine.head() = newHead.state
      fireBranchMoved(BranchManager.this, name, oldHead.state, newHead.state)
      oldHead
    }

    override def state = head.state

    override def toString(): String = "%s@%016X".format(name, head.stateId)
  }

  private val branches = mutable.Map[String, Branch]()
  def branchIterator: Iterator[Branch] = branches.values.iterator
  def branch(name: String): Option[Branch] = branches.get(name)

  private val branchListeners = mutable.ListBuffer[BranchListener]()

  //put the root
  private var _currentBranch = createBranch(BranchManager.BRANCH_DEFAULT, engine.head())
  def currentBranch = _currentBranch

  def currentBranch_=(branch: Branch): Unit = {
    if (branch.state.engine != engine) throw new IllegalArgumentException("branch is from another engine")
    engine.head() = branch.state
    _currentBranch = branch
  }

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

  def createBranchHere(name: String): Branch =
    createBranch(name, currentBranch.state)

  def createBranch(name: String, state: State): Branch = {
    if (branches.contains(name)) throw new IllegalArgumentException("branch already exists")
    val branch = new Branch(name, engine.wrappers(state.id))
    branches(name) = branch
    fireBranchCreated(this, name, state)
    branch
  }

  def deleteBranch(branch: Branch): Unit = {
    if (branch.state.engine != engine) throw new IllegalArgumentException("branch is from another engine")
    if (_currentBranch == branch) throw new IllegalArgumentException("can't delete curent branch")
    branches.remove(branch.name) match {
      case Some(_) =>
      case None    => assert(false)
    }
    fireBranchDeleted(this, branch.name, branch.state)
  }

  def execute[T <: Action](action: T): T = {
    engine.execute(action);
    currentBranch.head(engine.wrappers.head)
    action
  }

  ////receive branch sync
  //
  ///**
  // * <p>
  // * Receives an update offer from a remote branch manager. The branch to be updated consists of the branch
  // * owner's ID in hex (16 digits), a slash and a branch name. The {@code state} contains the full information
  // * about the branch's tip as the remote BranchManager knows it, and the {@code ancestors} array contains state
  // * IDs that the remote BranchManager thought this BranchManager already knew, as to allow to communicate deltas
  // * as small as possible. In the case that this BranchManager was already up to date, or had the parent of the
  // * new state, and could therefore update immediately, the return value will be the new state's id. Otherwise,
  // * it will be the latest state's id of which the manager knows it's on the branch; likely either an element of
  // * the {@code ancestors} array, or {@code 0}.
  // * </p>
  // *
  // * @param engine the id of the offering BranchManager's engine
  // * @param name the branch this update belongs to
  // * @param state the state being the tip of this update
  // * @param ancestors a list of ancestor state IDs the remote branch manager thought this branch manager might
  // *            already be aware of; most recent first
  // * @return the most recent state id that this BranchManager knows for the given branch; {@code 0} if the branch
  // *         is unknown; the {@code state}'s id if the full branch is known
  // */
  //def receiveUpdate(engine: Int, name: String, state: Obj, ancestors: Seq[Long], callback: SyncCallback): Unit = {
  //  val newHead = deserialize(state)
  //  if (newHead.resolve()) {
  //    //we have all we need
  //    newHead.addEngine(engine)
  //
  //    branches.get(name) match {
  //      case Some(branch) => branch.head(newHead)
  //      case None         => createBranch(name, newHead.state)
  //    }
  //
  //  } else {
  //    //we need additional states
  //    val l = ancestors.find { states.contains(_) } match {
  //      case Some(l) => l
  //      case None    => 0l
  //    }
  //
  //    callback.receiveUpdateCallback(this.engine.id, name, 0l)
  //  }
  //}
  //
  ///**
  // * <p>
  // * Receives the missing states for a previous {@link #receiveUpdate(int, String, Obj, long...) receiveUpdate()}
  // * call. The BranchManager does not save any transient state between {@code receiveUpdate()} and
  // * {@code receiveMissing()}, so some information must be added to the parameters again: the source of the
  // * update; and the branch being updated. To find again the state which was already received, the id of the head
  // * state of the update must be transmitted again. In addition, a list of states containing the delta between
  // * the remote and this BranchManager's branch is transmitted.
  // * </p>
  // *
  // * @param engine the id of the offering BranchManager's engine
  // * @param name the branch this update belongs to
  // * @param state the id of the state being the tip of this update
  // * @param ancestors a list of ancestor states that is missing from the local branch, in chronological order
  // */
  //def receiveMissing(engine: Int, name: String, state: Long, ancestors: Seq[Obj]): Unit = {
  //  ancestors.foreach { obj =>
  //    if (!deserialize(obj).resolve())
  //      throw new AssertionError()
  //  }
  //
  //  val newHead = states.get(state).get
  //  if (!newHead.resolve()) throw new AssertionError()
  //  newHead.addEngine(engine)
  //
  //  branches.get(name) match {
  //    case Some(branch) => branch.head(newHead)
  //    case None         => createBranch(name, newHead.state)
  //  }
  //}
  //
  ////send branch sync
  //
  ///**
  // * <p>
  // * Determines which data has to be sent to the {@link BranchManager} identified by {@code engine} to update the
  // * given branch. If there is anything to update, this method provides this data to the caller through
  // * {@link SyncCallback#sendUpdateCallback(int, String, Obj, long...) callback.sendUpdateCallback()}.
  // * </p>
  // *
  // * @param engine the engine which should be updated
  // * @param name the branch for which updates should be provided
  // * @param callback a callback to provide the data to the caller
  // */
  //def sendUpdate(engine: Int, name: String, callback: SyncCallback): Unit = {
  //  val head =
  //    branches.get(name) match {
  //      case Some(branch) =>
  //        if (branch.head == null) throw new IllegalArgumentException() //TODO can this even happen?
  //        else branch.head
  //      case None => throw new IllegalArgumentException("branch does not exist")
  //    }
  //  val state =
  //    if (engine == 0) {
  //      null
  //    } else {
  //      var _state = head
  //      while (_state != null && !_state.engines.contains(engine))
  //        _state = _state.parent
  //      if (_state == head) return
  //
  //      head.addEngine(engine)
  //      _state
  //    }
  //
  //  val ancestors = if (state == null) Seq[Long](0) else Seq[Long](state.stateId)
  //  callback.sendUpdateCallback(this.engine.id, name, serialize(head), ancestors: _*)
  //}
  //
  ///**
  // * <p>
  // * Determines which states are missing at the {@link BranchManager} identified by {@code engine} provided the
  // * known ancestor. If there is anything to update, this method provides this data to the caller through
  // * {@link SyncCallback#sendMissingCallback(int, String, long, Obj...) callback.sendMissingCallback()}.
  // * </p>
  // *
  // * @param engine the engine which should be updated
  // * @param name the branch for which updates should be provided
  // * @param ancestor the ancestor the remote branch manager reported it knew
  // * @param callback a callback to provide the data to the caller
  // */
  //def sendMissing(engine: Int, name: String, ancestor: Long, callback: SyncCallback): Unit = {
  //  val head =
  //    branches.get(name) match {
  //      case Some(branch) =>
  //        if (branch.head == null) throw new IllegalArgumentException() //TODO can this even happen?
  //        else branch.head
  //      case None => throw new IllegalArgumentException("branch does not exist")
  //    }
  //
  //  head.addEngine(engine)
  //  val headId = head.stateId
  //  if (headId == ancestor) return
  //
  //  var ancestors = List[Obj]()
  //  var state = head.parent
  //  while (state.stateId != ancestor) {
  //    ancestors = serialize(state) :: ancestors
  //    state = state.parent
  //  }
  //
  //  callback.sendMissingCallback(this.engine.id, name, headId, ancestors: _*)
  //}
  //
  ////state mgmt
  //
  //private def deserialize(state: Obj): StateWrapper = {
  //  try {
  //    new PolybufInput(engine.config).readObject(state).asInstanceOf[StateWrapper]
  //  } catch {
  //    case ex: PolybufException   => throw new IllegalArgumentException(ex)
  //    case ex: ClassCastException => throw new IllegalArgumentException(ex)
  //  }
  //}
  //
  //private def serialize(state: StateWrapper): Obj = {
  //  try {
  //    new PolybufOutput(engine.config).writeObject(state)
  //  } catch {
  //    case ex: PolybufException => throw new IllegalArgumentException(ex)
  //  }
  //}
}
