/**
 * AEntity.java
 * 
 * Created on 14.05.2013
 */

package at.pria.koza.harmonic;


/**
 * <p>
 * The class AEntity.
 * </p>
 * 
 * @version V0.0 14.05.2013
 * @author SillyFreak
 */
public abstract class AEntity implements Entity {
    private static final long serialVersionUID = 3158982209784552966L;
    
    private final Engine      engine;
    private final int         id;
    
    public AEntity(Engine engine) {
        this.engine = engine;
        id = engine.newId();
        engine.put(this);
    }
    
    public Engine getEngine() {
        return engine;
    }
    
    public int getId() {
        return id;
    }
}
