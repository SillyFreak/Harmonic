/**
 * HeadListener.java
 * 
 * Created on 07.08.2013
 */

package at.pria.koza.harmonic;


import java.util.EventListener;


/**
 * <p>
 * {@code HeadListener} provides a listener interface that can be used to detect when an engine's head is moved.
 * </p>
 * 
 * @version V1.0 07.08.2013
 * @author SillyFreak
 */
public interface HeadListener extends EventListener {
    /**
     * <p>
     * Called when an engine's head is {@linkplain Engine#setHead(State) moved}.
     * </p>
     * 
     * @param prevHead the engine's head previous to the move
     * @param newHead the engine's new head state
     */
    public void headMoved(State prevHead, State newHead);
}
