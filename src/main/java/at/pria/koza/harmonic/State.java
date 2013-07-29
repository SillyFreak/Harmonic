/**
 * State.java
 * 
 * Created on 16.05.2013
 */

package at.pria.koza.harmonic;


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
 * The class State.
 * </p>
 * 
 * @version V0.0 16.05.2013
 * @author SillyFreak
 */
public class State implements PolybufSerializable {
    public static final int                             FIELD     = StateP.STATE_FIELD_NUMBER;
    public static final GeneratedExtension<Obj, StateP> EXTENSION = StateP.state;
    
    public static PolybufIO<State> getIO(Engine engine) {
        return new IO(engine);
    }
    
    public static void configure(PolybufConfig config, Engine engine) {
        config.put(FIELD, getIO(engine));
    }
    
    private final Engine engine;
    private final long   id;
    private final State  parent;
    private final Action action;
    
    public State(Engine engine, State parent, long id, Action action) {
        this.engine = engine;
        this.id = id;
        this.parent = parent;
        this.action = action;
    }
    
    public Engine getEngine() {
        return engine;
    }
    
    public long getId() {
        return id;
    }
    
    public State getParent() {
        return parent;
    }
    
    public Action getAction() {
        return action;
    }
    
    @Override
    public int getTypeId() {
        return FIELD;
    }
    
    private static class IO implements PolybufIO<State> {
        private final Engine engine;
        
        public IO(Engine engine) {
            this.engine = engine;
        }
        
        @Override
        public void serialize(PolybufOutput out, State object, Obj.Builder obj) throws PolybufException {
            StateP.Builder b = StateP.newBuilder();
            b.setId(object.id);
            b.setParent(object.parent.id);
            b.setAction(out.writeObject(object.action));
            
            obj.setExtension(EXTENSION, b.build());
        }
        
        @Override
        public State initialize(PolybufInput in, Obj obj) throws PolybufException {
            StateP p = obj.getExtension(EXTENSION);
            long id = p.getId();
            long parentId = p.getParent();
            //TODO resolve the parentId
            State parent = null;
            //the action won't have a reference to this state, so this line is safe
            Action action = (Action) in.readObject(p.getAction());
            
            return new State(engine, parent, id, action);
        }
        
        @Override
        public void deserialize(PolybufInput in, Obj obj, State object) throws PolybufException {}
    }
}
