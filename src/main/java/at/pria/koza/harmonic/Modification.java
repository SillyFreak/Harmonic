/**
 * Modification.java
 * 
 * Created on 16.05.2013
 */

package at.pria.koza.harmonic;


/**
 * <p>
 * The class Modification.
 * </p>
 * 
 * @version V0.0 16.05.2013
 * @author SillyFreak
 */
public abstract class Modification {
    public void apply() {
        //TODO specify exception handling?
        apply0();
        Action.get().addModification(this);
    }
    
    public abstract void apply0();
    
    public abstract void revert();
}
