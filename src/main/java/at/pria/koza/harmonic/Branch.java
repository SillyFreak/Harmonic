/**
 * Branch.java
 * 
 * Created on 29.07.2013
 */

package at.pria.koza.harmonic;


/**
 * <p>
 * A {@code Branch} is a lightweight pointer to a {@link State} that makes it easy to move forward by appending a
 * new state.
 * </p>
 * 
 * @version V1.0 29.07.2013
 * @author SillyFreak
 */
public class Branch {
    private State state;
    
    /**
     * <p>
     * Creates a new branch that is initially at the same state as the given branch.
     * </p>
     * 
     * @param branch the branch where this new branch should point
     */
    public Branch(Branch branch) {
        this(branch.getState());
    }
    
    /**
     * <p>
     * Creates a new branch that is initially at the given state.
     * </p>
     * 
     * @param state the state where this new branch should point
     */
    public Branch(State state) {
        if(state == null) throw new IllegalArgumentException();
        this.state = state;
    }
    
    /**
     * <p>
     * Returns the branch's current state
     * </p>
     * 
     * @return
     */
    public State getState() {
        return state;
    }
    
    /**
     * <p>
     * Appends a new {@link Action} to this branch's history. Note that this does not apply the given action;
     * independent of the engine's current state, this just signals that in this branch, a new action was appended.
     * </p>
     * <p>
     * In detail, this method moves this branch's state forward to a new state referencing the given action, and
     * having the previous state as its parent.
     * </p>
     * 
     * @param action the action to append to this branch
     */
    public void append(Action action) {
        if(action == null) throw new IllegalArgumentException();
        if(action.getEngine() != state.getEngine()) throw new IllegalArgumentException();
        state = new State(state, action);
    }
}
