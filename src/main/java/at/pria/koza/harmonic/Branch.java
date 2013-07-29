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
 * @version V0.0 29.07.2013
 * @author SillyFreak
 */
public class Branch {
    private State state;
    
    public Branch(Branch branch) {
        this(branch.getState());
    }
    
    public Branch(State state) {
        if(state == null) throw new IllegalArgumentException();
        this.state = state;
    }
    
    public State getState() {
        return state;
    }
    
    public void append(Action action) {
        if(action == null) throw new IllegalArgumentException();
        if(action.getEngine() != state.getEngine()) throw new IllegalArgumentException();
        state = new State(state.getEngine(), state, state.getEngine().nextStateId(), action);
    }
}
