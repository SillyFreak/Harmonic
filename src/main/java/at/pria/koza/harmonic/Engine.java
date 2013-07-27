/**
 * Engine.java
 * 
 * Created on 14.05.2013
 */

package at.pria.koza.harmonic;


import java.util.HashMap;
import java.util.Map;


/**
 * <p>
 * The class {@code Engine} represents a graph of objects that describes the application. The Engine's State is
 * modified by Actions.
 * </p>
 * 
 * @version V1.0 26.07.2013
 * @author SillyFreak
 */
public class Engine {
    private int                  nextId   = 0;
    private Map<Integer, Entity> entities = new HashMap<>();
    
    /**
     * <p>
     * Returns the next ID to be assigned to an entity belonging to this engine.
     * </p>
     * 
     * @return the next ID to be assigned to an entity belonging to this engine
     */
    public int newId() {
        return nextId++;
    }
    
    /**
     * <p>
     * Adds an entity to this engine, so that it can be resolved using the assigned ID.
     * </p>
     * 
     * @param entity the entity to register in this engine
     */
    public void put(Entity entity) {
        entities.put(entity.getId(), entity);
    }
    
    /**
     * <p>
     * Returns the entity associated with the given ID.
     * </p>
     * 
     * @param id the ID to resolve
     * @return the entity that is associated with the ID, or {@code null}
     */
    public Entity get(int id) {
        return entities.get(id);
    }
}
