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
        get(engine.getState(0l));
    }
    
    public PolybufIO<MetaState> getIO(Engine engine) {
        return new IO();
    }
    
    public void configure(PolybufConfig config, Engine engine) {
        config.put(State.FIELD, getIO(engine));
    }
    
    private MetaState get(State state) {
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
                parent = get(parentState);
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
