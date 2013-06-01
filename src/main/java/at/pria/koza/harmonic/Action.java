/**
 * Action.java
 * 
 * Created on 16.05.2013
 */

package at.pria.koza.harmonic;


import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;


/**
 * <p>
 * The class Action.
 * </p>
 * 
 * @version V0.0 16.05.2013
 * @author SillyFreak
 */
public abstract class Action {
    private static final ThreadLocal<Deque<Action>> actions;
    
    static {
        actions = new ThreadLocal<Deque<Action>>() {
            @Override
            protected Deque<Action> initialValue() {
                return new LinkedList<>();
            }
        };
    }
    
    static Action get() {
        return actions.get().getFirst();
    }
    
    private static void push(Action a) {
        actions.get().addFirst(a);
    }
    
    private static void pop(Action a) {
        Action a0 = actions.get().removeFirst();
        assert a == a0;
    }
    
    
    private final Engine             engine;
    private final List<Modification> modifications;
    
    public Action(Engine engine) {
        this.engine = engine;
        modifications = new LinkedList<Modification>();
    }
    
    public Engine getEngine() {
        return engine;
    }
    
    public void apply() {
        try {
            push(this);
            apply0();
        } finally {
            pop(this);
        }
    }
    
    public void revert() {
        for(ListIterator<Modification> it = modifications.listIterator(modifications.size()); it.hasPrevious();) {
            it.previous().revert();
            it.remove();
        }
    }
    
    void addModification(Modification m) {
        modifications.add(m);
    }
    
    protected abstract void apply0();
}
