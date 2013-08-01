/**
 * BranchManager.java
 * 
 * Created on 29.07.2013
 */

package at.pria.koza.harmonic;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import at.pria.koza.harmonic.proto.HarmonicP.StateP;
import at.pria.koza.polybuf.PolybufConfig;
import at.pria.koza.polybuf.PolybufException;
import at.pria.koza.polybuf.PolybufIO;
import at.pria.koza.polybuf.PolybufInput;
import at.pria.koza.polybuf.PolybufOutput;
import at.pria.koza.polybuf.proto.Polybuf.Obj;


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
    private final Engine                   engine;
    private final Map<String, MetaState[]> branches = new HashMap<>();
    private final Map<Long, MetaState>     states   = new HashMap<>();
    
    //ctors
    
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
        put(engine.getState(0l));
    }
    
    //branch sync
    
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
     *            already be aware of
     * @return the most recent state id that this BranchManager knows for the given branch; {@code 0} if the branch
     *         is unknown; the {@code state}'s id if the full branch is known
     */
    public long receiveUpdate(int engine, String branch, Obj state, long... ancestors) {
        return 0;
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
     * @param ancestors a list of ancestor states that is missing from the local branch
     */
    public void receiveMissing(int engine, String branch, long state, Obj... ancestors) {}
    
    //state mgmt
    
    private MetaState put(State state) {
        Long id = state.getId();
        MetaState result = states.get(id);
        if(result == null) {
            states.put(id, result = new MetaState(state));
        }
        return result;
    }
    
    private class MetaState {
        private final long         stateId, parentId;
        
        private Action             action;
        
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
            engines.add(engine.getId());
            
            stateId = state.getId();
            if(stateId != 0) {
                State parentState = state.getParent();
                parentId = parentState.getId();
                parent = put(parentState);
            } else {
                parentId = 0;
            }
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
        public MetaState(StateP state, Action action) {
            stateId = state.getId();
            parentId = state.getParent();
            this.action = action;
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
            
            state = new State(parent.state, action);
            engines.add(engine.getId());
            engines.add(state.getEngineId());
            
            return true;
        }
    }
    
    //polybuf
    
    public PolybufIO<MetaState> getIO(Engine engine) {
        return new IO();
    }
    
    public void configure(PolybufConfig config, Engine engine) {
        config.put(State.FIELD, getIO(engine));
    }
    
    private class IO implements PolybufIO<MetaState> {
        private final PolybufIO<State> delegate;
        
        public IO() {
            delegate = State.getIO(engine);
        }
        
        @Override
        public void serialize(PolybufOutput out, MetaState object, Obj.Builder obj) throws PolybufException {
            delegate.serialize(out, object.state, obj);
        }
        
        @Override
        public MetaState initialize(PolybufInput in, Obj obj) throws PolybufException {
            StateP p = obj.getExtension(State.EXTENSION);
            Action action = (Action) in.readObject(p.getAction());
            return new MetaState(p, action);
        }
        
        @Override
        public void deserialize(PolybufInput in, Obj obj, MetaState object) throws PolybufException {}
    }
}
