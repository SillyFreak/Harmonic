/**
 * BranchManager.java
 * 
 * Created on 29.07.2013
 */

package at.pria.koza.harmonic;


import static java.util.Collections.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import at.pria.koza.harmonic.proto.HarmonicP.StateP;
import at.pria.koza.polybuf.PolybufConfig;
import at.pria.koza.polybuf.PolybufException;
import at.pria.koza.polybuf.PolybufIO;
import at.pria.koza.polybuf.PolybufInput;
import at.pria.koza.polybuf.PolybufOutput;
import at.pria.koza.polybuf.PolybufSerializable;
import at.pria.koza.polybuf.proto.Polybuf.Obj;

import com.google.protobuf.GeneratedMessage.GeneratedExtension;


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
public class BranchManager {
    public static final String             BRANCH_DEFAULT  = "default";
    
    private final Engine                   engine;
    private final Map<String, MetaState[]> branches        = new HashMap<>();
    private final Map<String, MetaState[]> branchesView    = unmodifiableMap(branches);
    private final Map<Long, MetaState>     states          = new HashMap<>();
    private final List<BranchListener>     branchListeners = new ArrayList<>();
    private String                         currentBranch;
    
    //ctors & misc
    
    /**
     * <p>
     * Creates a new branch manager
     * </p>
     * 
     * @see Engine#Engine()
     */
    public BranchManager() {
        this(new Engine());
    }
    
    /**
     * <p>
     * Creates a new branch manager.
     * </p>
     * 
     * @param spectating whether the engine will only spectate or also execute actions
     * 
     * @see Engine#Engine(boolean)
     */
    public BranchManager(boolean spectating) {
        this(new Engine(spectating));
    }
    
    /**
     * <p>
     * Creates a new branch manager.
     * </p>
     * 
     * @param id the ID to be used for the engine
     * 
     * @see Engine#Engine(int)
     */
    public BranchManager(int id) {
        this(new Engine(id));
    }
    
    /**
     * <p>
     * Creates a new branch manager.
     * </p>
     */
    private BranchManager(Engine engine) {
        this.engine = engine;
        
        //put the root
        currentBranch = BRANCH_DEFAULT;
        createBranch(currentBranch, engine.getState(0l));
    }
    
    /**
     * <p>
     * Returns the engine underlying this BranchManager. The engine must not be modified mannually.
     * </p>
     * 
     * @return
     */
    public Engine getEngine() {
        return engine;
    }
    
    //listeners
    
    public void addBranchListener(BranchListener l) {
        branchListeners.add(l);
    }
    
    public void removeBranchListener(BranchListener l) {
        branchListeners.remove(l);
    }
    
    protected void fireBranchCreated(BranchManager mgr, String branch, State head) {
        synchronized(branchListeners) {
            for(ListIterator<BranchListener> it = branchListeners.listIterator(branchListeners.size()); it.hasPrevious();) {
                it.previous().branchCreated(mgr, branch, head);
            }
        }
    }
    
    protected void fireBranchMoved(BranchManager mgr, String branch, State prevHead, State newHead) {
        synchronized(branchListeners) {
            for(ListIterator<BranchListener> it = branchListeners.listIterator(branchListeners.size()); it.hasPrevious();) {
                it.previous().branchMoved(mgr, branch, prevHead, newHead);
            }
        }
    }
    
    protected void fireBranchDeleted(BranchManager mgr, String branch, State prevHead) {
        synchronized(branchListeners) {
            for(ListIterator<BranchListener> it = branchListeners.listIterator(branchListeners.size()); it.hasPrevious();) {
                it.previous().branchDeleted(mgr, branch, prevHead);
            }
        }
    }
    
    //branch mgmt
    
    public void createBranchHere(String branch) {
        createBranch(branch, getBranchTip(currentBranch));
    }
    
    public void createBranch(String branch, State state) {
        if(state.getEngine() != engine) throw new IllegalArgumentException();
        if(branches.containsKey(branch)) throw new IllegalArgumentException();
        createOrMoveBranch(branch, put(state));
    }
    
    public void deleteBranch(String branch) {
        if(currentBranch.equals(branch)) throw new IllegalArgumentException();
        MetaState[] head = branches.remove(branch);
        if(head == null) throw new IllegalArgumentException();
        fireBranchDeleted(this, branch, head[0].state);
    }
    
    public State getBranchTip(String branch) {
        MetaState[] tip = branches.get(branch);
        if(tip == null) throw new IllegalArgumentException();
        return tip[0].state;
    }
    
    public State setBranchTip(String branch, State newHead) {
        if(newHead.getEngine() != engine) throw new IllegalArgumentException();
        if(!branches.containsKey(branch)) throw new IllegalArgumentException();
        return createOrMoveBranch(branch, put(newHead)).state;
    }
    
    private MetaState createOrMoveBranch(String branch, MetaState newHead) {
        MetaState[] tip = branches.get(branch);
        if(tip == null) branches.put(branch, tip = new MetaState[] {newHead});
        MetaState oldHead = tip[0];
        tip[0] = newHead;
        
        if(currentBranch.equals(branch)) this.engine.setHead(tip[0].state);
        if(oldHead == null) fireBranchCreated(this, branch, newHead.state);
        else fireBranchMoved(this, branch, oldHead.state, newHead.state);
        
        return oldHead;
    }
    
    public String getCurrentBranch() {
        return currentBranch;
    }
    
    public void setCurrentBranch(String branch) {
        State tip = getBranchTip(branch);
        engine.setHead(tip);
        currentBranch = branch;
    }
    
    public <T extends Action> T execute(T action) {
        MetaState[] tip = branches.get(currentBranch);
        State state = new State(tip[0].state, action);
        engine.setHead(state);
        tip[0] = put(state);
        return action;
    }
    
    public Set<String> getBranches() {
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
    public void receiveUpdate(int engine, String branch, Obj state, long[] ancestors, SyncCallback callback) {
        MetaState newHead = deserialize(state);
        put(newHead);
        if(newHead.resolve()) {
            //we have all we need
            newHead.addEngine(engine);
            
            createOrMoveBranch(branch, newHead);
            
        } else {
            //we need additional states
            for(long l:ancestors)
                if(states.containsKey(l)) {
                    callback.receiveUpdateCallback(this.engine.getId(), branch, l);
                    return;
                }
            
            //we know none of the given ancestors
            callback.receiveUpdateCallback(this.engine.getId(), branch, 0l);
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
     * @param branch the branch this update belongs to
     * @param state the id of the state being the tip of this update
     * @param ancestors a list of ancestor states that is missing from the local branch, in chronological order
     */
    public void receiveMissing(int engine, String branch, long state, Obj[] ancestors) {
        for(Obj obj:ancestors) {
            MetaState s = deserialize(obj);
            put(s);
            if(!s.resolve()) throw new AssertionError();
        }
        
        MetaState newHead = states.get(state);
        if(!newHead.resolve()) throw new AssertionError();
        newHead.addEngine(engine);
        
        createOrMoveBranch(branch, newHead);
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
    public void sendUpdate(int engine, String branch, SyncCallback callback) {
        MetaState[] head = branches.get(branch);
        if(head == null || head[0] == null) throw new IllegalArgumentException();
        
        MetaState state;
        if(engine == 0) {
            state = null;
        } else {
            state = head[0];
            Integer id = engine;
            while(state != null && !state.engines.contains(id))
                state = state.parent;
            if(state == head[0]) return;
            
            head[0].addEngine(engine);
        }
        
        long[] ancestors = state == null? new long[0]:new long[] {state.stateId};
        callback.sendUpdateCallback(this.engine.getId(), branch, serialize(head[0]), ancestors);
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
    public void sendMissing(int engine, String branch, long ancestor, SyncCallback callback) {
        MetaState[] head = branches.get(branch);
        if(head == null || head[0] == null) throw new IllegalArgumentException();
        
        head[0].addEngine(engine);
        long headId = head[0].stateId;
        if(headId == ancestor) return;
        
        LinkedList<Obj> ancestors = new LinkedList<>();
        for(MetaState state = head[0].parent; state.stateId != ancestor; state = state.parent) {
            ancestors.addFirst(serialize(state));
        }
        
        callback.sendMissingCallback(this.engine.getId(), branch, headId,
                ancestors.toArray(new Obj[ancestors.size()]));
    }
    
    public static interface SyncCallback {
        /**
         * <p>
         * Reports the data needed to call {@link BranchManager#receiveUpdate(int, String, Obj, long...)
         * receiveUpdate()} on the receiving BranchManager.
         * </p>
         */
        public void sendUpdateCallback(int engine, String branch, Obj state, long... ancestors);
        
        /**
         * <p>
         * Reports the data needed to call {@link BranchManager#sendMissing(int, String, long, SyncCallback)
         * sendMissing()} on the sending BranchManager.
         * </p>
         */
        public void receiveUpdateCallback(int engine, String branch, long ancestor);
        
        /**
         * <p>
         * Reports the data needed to call {@link BranchManager#receiveMissing(int, String, long, Obj...)
         * receiveMissing()} on the receiving BranchManager.
         * </p>
         */
        public void sendMissingCallback(int engine, String branch, long state, Obj... ancestors);
    }
    
    //state mgmt
    
    private MetaState deserialize(Obj state) {
        try {
            PolybufInput in = new PolybufInput(engine.getConfig());
            return (MetaState) in.readObject(state);
        } catch(PolybufException | ClassCastException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
    
    private Obj serialize(MetaState state) {
        try {
            PolybufOutput out = new PolybufOutput(engine.getConfig());
            return out.writeObject(state);
        } catch(PolybufException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
    
    private void put(MetaState state) {
        states.put(state.stateId, state);
    }
    
    private MetaState put(State state) {
        Long id = state.getId();
        MetaState result = states.get(id);
        if(result == null) {
            states.put(id, result = new MetaState(state));
        }
        return result;
    }
    
    private class MetaState implements PolybufSerializable {
        private final long         stateId, parentId;
        
        private Obj                action;
        
        private MetaState          parent;
        private State              state;
        
        //set of engines known to know this meta state
        private final Set<Integer> engines = new HashSet<>();
        
        /**
         * <p>
         * Used to add states created by the engine managed by this branch manager.
         * </p>
         * 
         * @param state the state to be added
         */
        public MetaState(State state) {
            this.state = state;
            stateId = state.getId();
            if(stateId != 0) {
                State parentState = state.getParent();
                parentId = parentState.getId();
                parent = put(parentState);
            } else {
                parentId = 0;
            }
            
            addEngine(engine.getId());
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
        public MetaState(Obj state) {
            StateP p = state.getExtension(State.EXTENSION);
            stateId = p.getId();
            parentId = p.getParent();
            this.action = p.getAction();
        }
        
        @Override
        public int getTypeId() {
            return State.FIELD;
        }
        
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
        public boolean resolve() {
            if(state != null) return true;
            assert stateId != 0;
            if(parent == null) parent = states.get(parentId);
            if(parent == null || !parent.resolve()) return false;
            
            state = new State(engine, parent.state, stateId, action);
            addEngine(engine.getId());
            addEngine(state.getEngineId());
            
            return true;
        }
        
        public void addEngine(int id) {
            //an assuption here is that if this state has an engine marked, all its parents will have it marked too
            //if the state is not resolved, i.e. not attached to its parent, then this assumption could be broken
            //when it is subsequently resolved, so don't allow that
            if(!resolve()) throw new IllegalStateException();
            boolean added = engines.add(id);
            if(added && parent != null) parent.addEngine(id);
        }
    }
    
    //polybuf
    
    public PolybufIO<MetaState> getIO() {
        return new IO();
    }
    
    public void configure(PolybufConfig config) {
        config.add(getIO());
    }
    
    private class IO implements PolybufIO<MetaState> {
        private final PolybufIO<State> delegate;
        
        public IO() {
            delegate = State.getIO(engine);
        }
        
        @Override
        public int getType() {
            return delegate.getType();
        }
        
        @Override
        public GeneratedExtension<Obj, ?> getExtension() {
            return delegate.getExtension();
        }
        
        @Override
        public void serialize(PolybufOutput out, MetaState object, Obj.Builder obj) throws PolybufException {
            delegate.serialize(out, object.state, obj);
        }
        
        @Override
        public MetaState initialize(PolybufInput in, Obj obj) throws PolybufException {
            return new MetaState(obj);
        }
        
        @Override
        public void deserialize(PolybufInput in, Obj obj, MetaState object) throws PolybufException {}
    }
}
