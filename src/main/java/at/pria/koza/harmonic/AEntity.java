/**
 * AEntity.java
 * 
 * Created on 14.05.2013
 */

package at.pria.koza.harmonic;


/**
 * <p>
 * Base class for simple entities. See the {@linkplain #AEntity(Engine) constructor} for details.
 * </p>
 * 
 * @version V0.0 14.05.2013
 * @author SillyFreak
 */
public abstract class AEntity implements Entity {
    private static final long serialVersionUID = 3158982209784552966L;
    
    private final Engine      engine;
    private final int         id;
    
    /**
     * <p>
     * Initializes the entity by assigning the engine and a {@linkplain Engine#newId() new id}, and then
     * {@linkplain Engine#put(Entity) putting} the entity into the given engine.
     * </p>
     * 
     * @param engine The engine this entity is to belong to.
     */
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
