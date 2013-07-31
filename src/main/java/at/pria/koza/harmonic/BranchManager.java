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
 * {@code BranchManager}
 * </p>
 * 
 * @version V0.0 29.07.2013
 * @author SillyFreak
 */
public class BranchManager {
    private final Engine                   engine;
    private final Map<String, MetaState[]> branches = new HashMap<>();
    private final Map<Long, MetaState>     states   = new HashMap<>();
    
    public BranchManager() {
        this(new Engine());
    }
    
    public BranchManager(boolean spectating) {
        this(new Engine(spectating));
    }
    
    public BranchManager(int id) {
        this(new Engine(id));
    }
    
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
        
        public MetaState(State state) {
            this.state = state;
            stateId = state.getId();
            if(stateId != 0) {
                State parentState = state.getParent();
                parentId = parentState.getId();
                parent = get(parentState);
            } else {
                parentId = 0;
            }
        }
        
        public MetaState(StateP state, Action action) {
            stateId = state.getId();
            parentId = state.getParent();
            this.action = action;
        }
        
        public boolean resolve() {
            if(state != null) return true;
            assert stateId != 0;
            if(parent == null) parent = states.get(parentId);
            if(parent == null || !parent.resolve()) return false;
            state = new State(parent.state, action);
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
