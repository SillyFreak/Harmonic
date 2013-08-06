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
    //Entity creation
    
    /**
     * <p>
     * Declares it as an error to call {@link Entity#setEngine(Engine, int) setEngine()} by yourself. This is done
     * by the {@link Engine#putEntity(Entity) putEntity()} method, and should be done nowhere else.
     * </p>
     */
    declare error: !within(Engine.RegisterEntity) && call(void Entity.setEngine(Engine, int)):
        "setEngine() must not be called explicitly; it is called by Engine";
    
    //applying modifications
    
    /**
     * <p>
     * Declares it as an error to call {@link Modification#addToAction() addToAction()} outside of
     * {@link Modification#apply() apply()}.
     * </p>
     */
    declare error: !withincode(void Modification.apply()) && call(void Modification.addToAction()):
        "addToAction() must not be called outside of apply()";
    
    /**
     * <p>
     * Declares it as an error to call {@link Modification#apply0() apply0()} outside of
     * {@link Modification#apply() apply()}.
     * </p>
     */
    declare error: !withincode(void Modification.apply()) && call(void Modification.apply0()):
        "apply0() must not be called outside of apply()";
    
    //applying actions
    
    /**
     * <p>
     * Declares it as an error to call {@link Action#apply0()} outside of {@link Action#apply()}.
     * </p>
     */
    declare error: !withincode(void Action.apply()) && call(void Action.apply0()):
        "apply0() must not be called outside of apply()";
    
    /**
     * <p>
     * Declares it as an error to call {@link Action#apply()} outside of {@link State#apply()}.
     * </p>
     */
    declare error: !withincode(void State.apply()) && call(void Action.apply()):
        "apply() must not be called explicitly; it is called by State";
    
    /**
     * <p>
     * Declares it as an error to call {@link State#apply()} outside of {@link Engine#setHead(State) setHead()}.
     * </p>
     */
    declare error: !withincode(void Engine.setHead(..)) && call(void State.apply()):
        "apply() must not be called explicitly; it is called by Engine";
    
    //reverting actions
    
    /**
     * <p>
     * Declares it as an error to call {@link Modification#revert()} outside of {@link Action#revert()}.
     * </p>
     */
    declare error: !withincode(void Action.revert()) && call(void Modification.revert()):
        "apply0() must not be called outside of apply()";
    
    /**
     * <p>
     * Declares it as an error to call {@link Action#revert()} outside of {@link State#revert()}.
     * </p>
     */
    declare error: !withincode(void State.revert()) && call(void Action.revert()):
        "apply0() must not be called outside of apply()";
    
    /**
     * <p>
     * Declares it as an error to call {@link Action#apply()} outside of {@link State#apply()}.
     * </p>
     */
    declare error: !withincode(void State.apply()) && call(void Action.apply()):
        "apply() must not be called explicitly; it is called by State";
    
    /**
     * <p>
     * Declares it as an error to call {@link State#revert()} outside of {@link Engine#setHead(State) setHead()}.
     * </p>
     */
    declare error: !withincode(void Engine.setHead(..)) && call(void State.revert()):
        "apply() must not be called explicitly; it is called by Engine";
}
