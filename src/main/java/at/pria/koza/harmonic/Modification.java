/**
 * Modification.java
 * 
 * Created on 16.05.2013
 */

package at.pria.koza.harmonic;


/**
 * <p>
 * The class {@code Modification} represents a low-level change that does a simple modification to the
 * {@link Engine}. "Simple" means that the change is easily {@linkplain #revert() reverted}. The corresponding
 * {@link Action} manages a list of {@code Modifications} that it triggered, so that the {@code Action}, too, can
 * be reverted.
 * </p>
 * 
 * @version V1.0 26.07.2013
 * @author SillyFreak
 */
public abstract class Modification {
    /**
     * <p>
     * Applies the modification to the engine. This method must only be called by {@link Action#apply0()}
     * implementations. This method invokes {@link #apply0()}, followed by {@link #addToAction()}.
     * </p>
     * <p>
     * Note that this means that in the case of an exception, this means the modification will <i>not</i> be added
     * the the current action. Implementations may ignore error handling, losing the ability to use an engine after
     * a failed modification; use these semantics handle the exception as described in {@link #apply0()}; or
     * overwrite this method and employ another way for error-handling, as long as {@link #addToAction()} is called
     * for a successful invocation.
     * </p>
     */
    public void apply() {
        apply0();
        addToAction();
    }
    
    /**
     * <p>
     * Adds this {@code Modification} to the current action. This method must be called by {@link #apply()} if the
     * modification left the engine changed; in particular, it must be called if the modification was successful.
     * If there was an error, and error recovery is needed, then this method must be called exactly if the engine
     * changed. In that case, {@link #revert()} must be able to revert the actual changes, even if they differ from
     * the changes that the successful modification would have performed.
     * </p>
     * 
     * @see Action#get()
     */
    protected final void addToAction() {
        Action.get().addModification(this);
    }
    
    /**
     * <p>
     * Performs the actual action. This method must only be called from {@link #apply()}.
     * </p>
     * <p>
     * Note that this method is responsible for leaving the {@link Engine} in a well-defined state. If it does not
     * return normally, and the application needs to be able to roll back the engine, then this method must leave
     * the engine in a state where {@linkplain #revert() reverting} the action really does revert it into the
     * previous state. In particular, that means, if {@link #addToAction()} was called despite the exception (i.e.
     * {@link #apply()} was overwritten), then {@link #revert()} must revert exactly those changes that were
     * actually performed; or if {@link #addToAction()} was not called, then this method must ensure before
     * re-throwing the exception that the engine is in its previous state.
     * </p>
     */
    protected abstract void apply0();
    
    /**
     * <p>
     * Reverts this Modification. This method is only called by {@link Action#revert()}, and only if a previous
     * execution of {@link #apply()} called {@link #addToAction()}. When this method is called, the engine will be
     * in the same state as it was after {@linkplain #apply() applying} this modification.
     * </p>
     */
    protected abstract void revert();
}
