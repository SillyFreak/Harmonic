/**
 * StateListener.java
 * 
 * Created on 07.08.2013
 */

package at.pria.koza.harmonic;


import java.util.EventListener;


/**
 * <p>
 * {@code StateListener} provides a listener interface that can be used to detect when new states are added to an
 * engine.
 * </p>
 * 
 * @version V1.0 07.08.2013
 * @author SillyFreak
 */
public interface StateListener extends EventListener {
    /**
     * <p>
     * Called when a {@link State} is added. All relevant information can be retrieved from the state: The engine
     * the state was added to via {@link State#getEngine() getEngine()}; the engine originating the state via
     * {@link State#getEngineId() getEngineId()} etc. Note that, because at this moment the state was definitely
     * not executed yet, {@link State#getAction() getAction()} will return {@code null}.
     * </p>
     * 
     * @param state the state that was added
     */
    public void stateAdded(State state);
}
