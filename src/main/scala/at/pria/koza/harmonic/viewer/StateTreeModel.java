/**
 * StateTreeModel.java
 * 
 * Created on 11.08.2013
 */

package at.pria.koza.harmonic.viewer;


import java.util.HashMap;
import java.util.Map;

import javax.swing.tree.DefaultTreeModel;

import at.pria.koza.harmonic.State;


/**
 * <p>
 * {@code StateTreeModel}
 * </p>
 * 
 * @version V0.0 11.08.2013
 * @author SillyFreak
 */
public class StateTreeModel extends DefaultTreeModel {
    private static final long          serialVersionUID = 1L;
    
    private final Map<Long, StateNode> nodes            = new HashMap<>();
    private final StateNode            root             = new StateNode();
    
    public StateTreeModel() {
        super(null);
    }
    
    @Override
    public StateNode getRoot() {
        return root;
    }
    
    public StateNode resolve(State state) {
        Long id = state.getId();
        StateNode result = nodes.get(id);
        if(result == null) nodes.put(id, result = new StateNode(this, state));
        return result;
    }
    
    @Override
    protected void fireTreeNodesInserted(Object source, Object[] path, int[] childIndices, Object[] children) {
        super.fireTreeNodesInserted(source, path, childIndices, children);
    }
    
    @Override
    protected void fireTreeNodesChanged(Object source, Object[] path, int[] childIndices, Object[] children) {
        super.fireTreeNodesChanged(source, path, childIndices, children);
    }
}
