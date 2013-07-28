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
     * Adds an entity to this engine, assigning it a unique id.
     * </p>
     * 
     * @param entity the entity to register in this engine
     */
    public void put(Entity entity) {
        new RegisterEntity(this, entity).apply();
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
    
    private static class RegisterEntity extends Modification {
        private final Engine engine;
        private final Entity entity;
        
        public RegisterEntity(Engine engine, Entity entity) {
            this.engine = engine;
            this.entity = entity;
        }
        
        @Override
        protected void apply0() {
            int id = engine.nextId++;
            entity.setEngine(engine, id);
            engine.entities.put(id, entity);
        }
        
        @Override
        public void revert() {
            engine.entities.remove(entity.getId());
            entity.setEngine(null, -1);
            engine.nextId--;
        }
    }
}
