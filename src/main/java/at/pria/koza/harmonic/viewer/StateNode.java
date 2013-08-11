/**
 * StateNode.java
 * 
 * Created on 11.08.2013
 */

package at.pria.koza.harmonic.viewer;


import static java.util.Collections.*;

import java.util.ArrayList;
import java.util.List;

import javax.swing.tree.TreeNode;

import at.pria.koza.harmonic.State;


/**
 * <p>
 * {@code StateNode}
 * </p>
 * 
 * @version V0.0 11.08.2013
 * @author SillyFreak
 */
public class StateNode extends AbstractTreeNode<StateNode> {
    private final State           state;
    
    private final StateNode       parent;
    private final List<StateNode> children     = new ArrayList<>();
    private final List<StateNode> childrenView = unmodifiableList(children);
    
    protected StateNode() {
        state = null;
        parent = null;
    }
    
    protected StateNode(StateTreeModel model, State state) {
        this.state = state;
        parent = state.getId() == 0l? model.getRoot():model.resolve(state.getParent());
        parent.children.add(this);
        
        model.fireTreeNodesInserted(model, parent.getPath(), new int[] {parent.children.size() - 1},
                new Object[] {this});
    }
    
    public State getState() {
        return state;
    }
    
    @Override
    public TreeNode getParent() {
        return parent;
    }
    
    @Override
    protected List<StateNode> getChildren() {
        return childrenView;
    }
    
    @Override
    public String toString() {
        return state == null? "<root>":state.getId() == 0l? state.getEngine().toString():state.toString();
    }
}
