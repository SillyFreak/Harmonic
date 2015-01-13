/**
 * BranchManager.java
 *
 * Created on 29.07.2013
 */

package at.pria.koza.harmonic

import java.util.Collections._

import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import java.util.LinkedList
import java.util.List
import java.util.ListIterator
import java.util.Map
import java.util.Set

import at.pria.koza.harmonic.proto.HarmonicP.StateP
import at.pria.koza.polybuf.PolybufConfig
import at.pria.koza.polybuf.PolybufException
import at.pria.koza.polybuf.PolybufIO
import at.pria.koza.polybuf.PolybufInput
import at.pria.koza.polybuf.PolybufOutput
import at.pria.koza.polybuf.PolybufSerializable
import at.pria.koza.polybuf.proto.Polybuf.Obj

import com.google.protobuf.GeneratedMessage.GeneratedExtension

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
}

/**
 * <p>
 * Creates a new branch manager.
 * </p>
 */
class BranchManager(engine: Engine) {

  private val branches = new HashMap[String, Array[MetaState]]()
  private val branchesView = unmodifiableMap[String, Array[MetaState]](branches)
  private val states = new HashMap[Long, MetaState]()
  private val branchListeners = new ArrayList[BranchListener]()
  private var _currentBranch = BranchManager.BRANCH_DEFAULT
  //put the root
  createBranch(_currentBranch, engine.getState(0l))

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

  /**
   * <p>
   * Returns the engine underlying this BranchManager. The engine must not be modified mannually.
   * </p>
   *
   * @return
   */
  def getEngine(): Engine = engine

  //listeners

  def addBranchListener(l: BranchListener): Unit = branchListeners.add(l)

  def removeBranchListener(l: BranchListener): Unit = branchListeners.remove(l)

  private[harmonic] def fireBranchCreated(mgr: BranchManager, branch: String, head: State): Unit = {
    branchListeners.synchronized {
      val it = branchListeners.listIterator(branchListeners.size())
      while (it.hasPrevious()) {
        it.previous().branchCreated(mgr, branch, head)
      }
    }
  }

  private[harmonic] def fireBranchMoved(mgr: BranchManager, branch: String, prevHead: State, newHead: State): Unit = {
    branchListeners.synchronized {
      val it = branchListeners.listIterator(branchListeners.size())
      while (it.hasPrevious()) {
        it.previous().branchMoved(mgr, branch, prevHead, newHead)
      }
    }
  }

  private[harmonic] def fireBranchDeleted(mgr: BranchManager, branch: String, prevHead: State): Unit = {
    branchListeners.synchronized {
      val it = branchListeners.listIterator(branchListeners.size())
      while (it.hasPrevious()) {
        it.previous().branchDeleted(mgr, branch, prevHead)
      }
    }
  }

  //branch mgmt

  def createBranchHere(branch: String): Unit =
    createBranch(branch, getBranchTip(_currentBranch))

  def createBranch(branch: String, state: State): Unit = {
    if (state.getEngine() != engine) throw new IllegalArgumentException()
    if (branches.containsKey(branch)) throw new IllegalArgumentException()
    createOrMoveBranch(branch, put(state))
  }

  def deleteBranch(branch: String): Unit = {
    if (_currentBranch.equals(branch)) throw new IllegalArgumentException()
    val head = branches.remove(branch)
    if (head == null) throw new IllegalArgumentException()
    fireBranchDeleted(this, branch, head(0).state)
  }

  def getBranchTip(branch: String): State = {
    val tip = branches.get(branch)
    if (tip == null) throw new IllegalArgumentException()
    tip(0).state
  }

  def setBranchTip(branch: String, newHead: State): State = {
    if (newHead.getEngine() != engine) throw new IllegalArgumentException()
    if (!branches.containsKey(branch)) throw new IllegalArgumentException()
    createOrMoveBranch(branch, put(newHead)).state
  }

  private def createOrMoveBranch(branch: String, newHead: MetaState): MetaState = {
    var tip = branches.get(branch)
    if (tip == null) {
      tip = new Array[MetaState](1)
      tip(0) = newHead
      branches.put(branch, tip)
    }
    val oldHead = tip(0)
    tip(0) = newHead

    if (_currentBranch.equals(branch)) engine.setHead(tip(0).state)
    if (oldHead == null) fireBranchCreated(this, branch, newHead.state)
    else fireBranchMoved(this, branch, oldHead.state, newHead.state)

    oldHead
  }

  def getCurrentBranch(): String = _currentBranch

  def setCurrentBranch(branch: String): Unit = {
    val tip = getBranchTip(branch);
    engine.setHead(tip);
    _currentBranch = branch;
  }

  def execute[T <: Action](action: T): T = {
    val state = new State(getBranchTip(_currentBranch), action)
    engine.setHead(state)
    createOrMoveBranch(_currentBranch, put(state))
    action
  }

  def getBranches(): Set[String] = {
    return branchesView.keySet();
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
   * @param branch the branch this update belongs to
   * @param state the state being the tip of this update
   * @param ancestors a list of ancestor state IDs the remote branch manager thought this branch manager might
   *            already be aware of; most recent first
   * @return the most recent state id that this BranchManager knows for the given branch; {@code 0} if the branch
   *         is unknown; the {@code state}'s id if the full branch is known
   */
  def receiveUpdate(engine: Int, branch: String, state: Obj, ancestors: Array[Long], callback: SyncCallback): Unit = {
    //MetaState newHead = deserialize(state);
    //if(newHead.resolve()) {
    //    //we have all we need
    //    newHead.addEngine(engine);
    //
    //    createOrMoveBranch(branch, newHead);
    //
    //} else {
    //    //we need additional states
    //    for(long l:ancestors)
    //        if(states.containsKey(l)) {
    //            callback.receiveUpdateCallback(this.engine.getId(), branch, l);
    //            return;
    //        }
    //
    //    //we know none of the given ancestors
    //    callback.receiveUpdateCallback(this.engine.getId(), branch, 0l);
    //}
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
   * @param branch the branch this update belongs to
   * @param state the id of the state being the tip of this update
   * @param ancestors a list of ancestor states that is missing from the local branch, in chronological order
   */
  def receiveMissing(engine: Int, branch: String, state: Long, ancestors: Array[Obj]): Unit = {
    //for(Obj obj:ancestors) {
    //    MetaState s = deserialize(obj);
    //    if(!s.resolve()) throw new AssertionError();
    //}
    //
    //MetaState newHead = states.get(state);
    //if(!newHead.resolve()) throw new AssertionError();
    //newHead.addEngine(engine);
    //
    //createOrMoveBranch(branch, newHead);
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
   * @param branch the branch for which updates should be provided
   * @param callback a callback to provide the data to the caller
   */
  def sendUpdate(engine: Int, branch: String, callback: SyncCallback): Unit = {
    //MetaState[] head = branches.get(branch);
    //if(head == null || head[0] == null) throw new IllegalArgumentException();
    //
    //MetaState state;
    //if(engine == 0) {
    //    state = null;
    //} else {
    //    state = head[0];
    //    Integer id = engine;
    //    while(state != null && !state.engines.contains(id))
    //        state = state.parent;
    //    if(state == head[0]) return;
    //
    //    head[0].addEngine(engine);
    //}
    //
    //long[] ancestors = state == null? new long[0]:new long[] {state.stateId};
    //callback.sendUpdateCallback(this.engine.getId(), branch, serialize(head[0]), ancestors);
  }

  /**
   * <p>
   * Determines which states are missing at the {@link BranchManager} identified by {@code engine} provided the
   * known ancestor. If there is anything to update, this method provides this data to the caller through
   * {@link SyncCallback#sendMissingCallback(int, String, long, Obj...) callback.sendMissingCallback()}.
   * </p>
   *
   * @param engine the engine which should be updated
   * @param branch the branch for which updates should be provided
   * @param ancestor the ancestor the remote branch manager reported it knew
   * @param callback a callback to provide the data to the caller
   */
  def sendMissing(engine: Int, branch: String, ancestor: Long, callback: SyncCallback): Unit = {
    //MetaState[] head = branches.get(branch);
    //if(head == null || head[0] == null) throw new IllegalArgumentException();
    //
    //head[0].addEngine(engine);
    //long headId = head[0].stateId;
    //if(headId == ancestor) return;
    //
    //LinkedList<Obj> ancestors = new LinkedList<>();
    //for(MetaState state = head[0].parent; state.stateId != ancestor; state = state.parent) {
    //    ancestors.addFirst(serialize(state));
    //}
    //
    //callback.sendMissingCallback(this.engine.getId(), branch, headId,
    //        ancestors.toArray(new Obj[ancestors.size()]));
  }

  trait SyncCallback {
    /**
     * <p>
     * Reports the data needed to call {@link BranchManager#receiveUpdate(int, String, Obj, long...)
     * receiveUpdate()} on the receiving BranchManager.
     * </p>
     */
    def sendUpdateCallback(engine: Int, branch: String, state: Obj, ancestors: Long*): Unit;

    /**
     * <p>
     * Reports the data needed to call {@link BranchManager#sendMissing(int, String, long, SyncCallback)
     * sendMissing()} on the sending BranchManager.
     * </p>
     */
    def receiveUpdateCallback(engine: Int, branch: String, ancestor: Long): Unit;

    /**
     * <p>
     * Reports the data needed to call {@link BranchManager#receiveMissing(int, String, long, Obj...)
     * receiveMissing()} on the receiving BranchManager.
     * </p>
     */
    def sendMissingCallback(engine: Int, branch: String, state: Long, ancestors: Obj*): Unit;
  }

  //state mgmt

  private def deserialize(state: Obj): MetaState = {
    //try {
    //    PolybufInput in = new PolybufInput(engine.getConfig());
    //    return (MetaState) in.readObject(state);
    //} catch(PolybufException | ClassCastException ex) {
    //    throw new IllegalArgumentException(ex);
    //}
    null
  }

  private def serialize(state: MetaState): Obj = {
    //try {
    //    PolybufOutput out = new PolybufOutput(engine.getConfig());
    //    return out.writeObject(state);
    //} catch(PolybufException ex) {
    //    throw new IllegalArgumentException(ex);
    //}
    null
  }

  private def put(state: State): MetaState = {
    //Long id = state.getId();
    //MetaState result = states.get(id);
    //if(result == null) {
    //    states.put(id, result = new MetaState(state));
    //}
    //return result;
    null
  }

  private class MetaState(stateId: Long, parentId: Long) extends PolybufSerializable {
    private var _action: Obj = _

    private var _parent: MetaState = _
    private var _state: State = _
    def state = _state

    //set of engines known to know this meta state
    private val engines = new HashSet[Integer]();

    /**
     * <p>
     * Used to add states created by the engine managed by this branch manager.
     * </p>
     *
     * @param state the state to be added
     */
    def this(state: State) = {
      this(
        state.getId(),
        if (state.getId() != 0) state.getParent().getId() else 0)

      _state = state;
      if (stateId != 0)
        _parent = put(state.getParent())
      addEngine(engine.getId())
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
    def this(state: Obj) = {
      this(
        state.getExtension(State.EXTENSION).getId(),
        state.getExtension(State.EXTENSION).getParent())
      _action = state.getExtension(State.EXTENSION).getAction;
    }

    override def getTypeId(): Int = State.FIELD

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
      //if(state != null) return true;
      //assert stateId != 0;
      //if(parent == null) parent = states.get(parentId);
      //if(parent == null || !parent.resolve()) return false;
      //
      //state = new State(engine, parent.state, stateId, action);
      //addEngine(engine.getId());
      //addEngine(state.getEngineId());
      //
      //return true;
      true
    }

    def addEngine(id: Int): Unit = {
      ////an assuption here is that if this state has an engine marked, all its parents will have it marked too
      ////if the state is not resolved, i.e. not attached to its parent, then this assumption could be broken
      ////when it is subsequently resolved, so don't allow that
      //if(!resolve()) throw new IllegalStateException();
      //boolean added = engines.add(id);
      //if(added && parent != null) parent.addEngine(id);
    }
  }

  //polybuf

  private def getIO(): PolybufIO[MetaState] = new IO();

  def configure(config: PolybufConfig): Unit = config.add(getIO())

  private class IO extends PolybufIO[MetaState] {
    val delegate: PolybufIO[State] = State.getIO(engine)

    override def getType(): Int = {
      return delegate.getType();
    }

    override def getExtension(): GeneratedExtension[Obj, _] = {
      return delegate.getExtension();
    }

    @throws[PolybufException]
    override def serialize(out: PolybufOutput, instance: MetaState, obj: Obj.Builder): Unit = {
      delegate.serialize(out, instance.state, obj);
    }

    @throws[PolybufException]
    override def initialize(in: PolybufInput, obj: Obj): MetaState = {
      val p = obj.getExtension(State.EXTENSION)
      val id = p.getId()
      //handle states already present properly
      var result = states.get(id)
      if (result == null) {
        result = new MetaState(obj)
        states.put(id, result)
      }
      result
    }

    @throws[PolybufException]
    override def deserialize(in: PolybufInput, obj: Obj, instance: MetaState): Unit = {
      delegate.deserialize(in, obj, instance.state)
    }
  }
}
