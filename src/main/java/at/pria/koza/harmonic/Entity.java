/**
 * Entity.java
 * 
 * Created on 14.05.2013
 */

package at.pria.koza.harmonic;


import java.io.Serializable;


/**
 * <p>
 * The class {@code Entity} represents a single object that is part of an {@linkplain Engine engine's} state. An
 * entity can only belong to one engine at a time. In this engine, it is identified by an id assigned by the
 * engine. The id assignment scheme is deterministic so that systems of distributed engines can replay
 * {@link Action Actions} that create new entities.
 * </p>
 * 
 * @version V1.0 27.07.2013
 * @author SillyFreak
 */
public interface Entity extends Serializable {
    /**
     * <p>
     * Returns the engine to which this entity belongs.
     * </p>
     * 
     * @return the engine to which this entity belongs
     */
    public Engine getEngine();
    
    /**
     * <p>
     * Returns the id assigned to this entity by the engine.
     * </p>
     * 
     * @return this entity's id
     */
    public int getId();
}
