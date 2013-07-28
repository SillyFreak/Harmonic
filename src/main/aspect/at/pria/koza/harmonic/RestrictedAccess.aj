/**
 * RestrictedAccess.aj
 * 
 * Created on 28.07.2013
 */

package at.pria.koza.harmonic;


/**
 * <p>
 * {@code RestrictedAccess}
 * </p>
 * 
 * @version V0.0 28.07.2013
 * @author SillyFreak
 */
public aspect RestrictedAccess {
    /**
     * <p>
     * Declares it as an error to call {@link Entity#setEngine(Engine, int) setEngine()} by yourself. This is done
     * by the {@link Engine#put(Entity) put()} method, and should be done nowhere else.
     * </p>
     */
    declare error: !withincode(void Engine.put(Entity)) && call(void Entity.setEngine(Engine, int)):
        "setEngine() must not be called explicitly; it is called by Engine";
    
    /**
     * <p>
     * Declares it as an error to call {@link Modification#addToAction() addToAction()} outside of
     * {@link Modification#apply() apply()}.
     * </p>
     */
    declare error: !withincode(void Modification.apply()) && call(void Modification.addToAction()):
        "apply0() must not be called outside of apply()";
    
    /**
     * <p>
     * Declares it as an error to call {@link Modification#apply0() apply0()} outside of
     * {@link Modification#apply() apply()}.
     * </p>
     */
    declare error: !withincode(void Modification.apply()) && call(void Modification.apply0()):
        "apply0() must not be called outside of apply()";
    
    /**
     * <p>
     * Declares it as an error to call {@link Action#apply0() apply0()} outside of {@link Action#apply() apply()}.
     * </p>
     */
    declare error: !withincode(void Action.apply()) && call(void Action.apply0()):
        "apply0() must not be called outside of apply()";
    
}
