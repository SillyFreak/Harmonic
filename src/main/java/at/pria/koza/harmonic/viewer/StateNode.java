/**
 * StateNode.java
 * 
 * Created on 11.08.2013
 */

package at.pria.koza.harmonic.viewer;


import static java.util.Collections.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
    private final Set<String>     labels       = new TreeSet<>();
    
    private final StateTreeModel  source;
    private final int[]           childIndices;
    private final Object[]        path, childObjects;
    
    protected StateNode() {
        state = null;
        parent = null;
        
        source = null;
        path = null;
        childIndices = null;
        childObjects = null;
    }
    
    protected StateNode(StateTreeModel model, State state) {
        this.state = state;
        parent = state.getId() == 0l? model.getRoot():model.resolve(state.getParent());
        parent.children.add(this);
        
        source = model;
        path = parent.getPath();
        childIndices = new int[] {parent.children.size() - 1};
        childObjects = new Object[] {this};
        model.fireTreeNodesInserted(source, path, childIndices, childObjects);
    }
    
    public State getState() {
        return state;
    }
    
    public void fireChanged() {
        source.fireTreeNodesChanged(source, path, childIndices, childObjects);
    }
    
    public Set<String> getLabels() {
        return labels;
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
        if(state == null) return "<root>";
        StringBuilder sb = new StringBuilder();
        for(Iterator<String> it = labels.iterator(); it.hasNext();) {
            String lbl = it.next();
            if(lbl.startsWith("branch:")) sb.append('[').append(lbl.substring(7)).append(']');
            else sb.append(lbl);
            sb.append(' ');
        }
        if(state.getId() == 0l) sb.append(state.getEngine().toString());
        else sb.append(state.toString());
        return sb.toString();
    }
}
