/**
 * AbstractTreeNode.java
 * 
 * Created on 11.08.2013
 */

package at.pria.koza.harmonic.viewer;


import static java.util.Collections.*;

import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import javax.swing.tree.TreeNode;


/**
 * <p>
 * {@code AbstractTreeNode}
 * </p>
 * 
 * @version V0.0 11.08.2013
 * @author SillyFreak
 */
public abstract class AbstractTreeNode<E extends TreeNode> implements TreeNode {
    
    protected abstract List<E> getChildren();
    
    @Override
    public abstract TreeNode getParent();
    
    protected Object[] getPath() {
        LinkedList<Object> path = new LinkedList<>();
        for(TreeNode node = this; node != null; node = node.getParent())
            path.addFirst(node);
        return path.toArray();
    }
    
    @Override
    public TreeNode getChildAt(int childIndex) {
        List<E> list = getChildren();
        if(list == null) throw new IndexOutOfBoundsException();
        return list.get(childIndex);
    }
    
    @Override
    public int getChildCount() {
        List<E> list = getChildren();
        return list == null? 0:list.size();
    }
    
    @Override
    public int getIndex(TreeNode node) {
        List<E> list = getChildren();
        if(list == null) return -1;
        return list.indexOf(node);
    }
    
    @Override
    public boolean getAllowsChildren() {
        return getChildren() != null;
    }
    
    @Override
    public boolean isLeaf() {
        List<E> list = getChildren();
        return list == null || list.isEmpty();
    }
    
    @Override
    public Enumeration<E> children() {
        List<E> list = getChildren();
        if(list == null) list = emptyList();
        return enumeration(list);
    }
}
