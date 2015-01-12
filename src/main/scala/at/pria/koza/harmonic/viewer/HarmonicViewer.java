/**
 * HarmonicViewer.java
 * 
 * Created on 11.08.2013
 */

package at.pria.koza.harmonic.viewer;


import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

import at.pria.koza.harmonic.BranchListener;
import at.pria.koza.harmonic.BranchManager;
import at.pria.koza.harmonic.Engine;
import at.pria.koza.harmonic.HeadListener;
import at.pria.koza.harmonic.State;
import at.pria.koza.harmonic.StateListener;


/**
 * <p>
 * {@code HarmonicViewer}
 * </p>
 * 
 * @version V0.0 11.08.2013
 * @author SillyFreak
 */
public class HarmonicViewer extends JPanel {
    private static final long    serialVersionUID = 1L;
    
    private final Listener       listener;
    private final StateTreeModel states;
    private final JTree          statesTree;
    
    public HarmonicViewer() {
        super(new BorderLayout());
        listener = new Listener();
        states = new StateTreeModel();
        
        statesTree = new JTree(states);
        statesTree.setMinimumSize(new Dimension(100, 0));
        statesTree.setRootVisible(false);
        
        JPanel statesTreePanel = new JPanel(new BorderLayout());
        statesTreePanel.add(new JScrollPane(statesTree));
        add(statesTreePanel);
    }
    
    public void listenTo(Engine engine) {
        engine.addStateListener(listener);
        engine.addHeadListener(listener);
        for(State state:engine.getStates().values())
            states.resolve(state);
        
        StateNode head = states.resolve(engine.getHead());
        head.getLabels().add("<HEAD>");
        head.fireChanged();
        makeVisible(head);
    }
    
    public void listenTo(BranchManager mgr) {
        listenTo(mgr.getEngine());
        mgr.addBranchListener(listener);
        
        for(String branch:mgr.getBranches()) {
            StateNode head = states.resolve(mgr.getBranchTip(branch));
            head.getLabels().add("branch:" + branch);
            head.fireChanged();
        }
    }
    
    private void makeVisible(StateNode node) {
        statesTree.makeVisible(new TreePath(node.getPath()));
    }
    
    private class Listener implements HeadListener, StateListener, BranchListener {
        @Override
        public void headMoved(State prevHead, State newHead) {
            StateNode prevNode = states.resolve(prevHead);
            prevNode.getLabels().remove("<HEAD>");
            prevNode.fireChanged();
            StateNode newNode = states.resolve(newHead);
            newNode.getLabels().add("<HEAD>");
            newNode.fireChanged();
        }
        
        @Override
        public void stateAdded(State state) {
            StateNode node = states.resolve(state);
            makeVisible(node);
        }
        
        @Override
        public void branchCreated(BranchManager mgr, String branch, State head) {
            StateNode headNode = states.resolve(head);
            headNode.getLabels().add("branch:" + branch);
            headNode.fireChanged();
        }
        
        @Override
        public void branchMoved(BranchManager mgr, String branch, State prevHead, State newHead) {
            StateNode prevNode = states.resolve(prevHead);
            prevNode.getLabels().remove("branch:" + branch);
            prevNode.fireChanged();
            StateNode newNode = states.resolve(newHead);
            newNode.getLabels().add("branch:" + branch);
            newNode.fireChanged();
        }
        
        @Override
        public void branchDeleted(BranchManager mgr, String branch, State prevHead) {
            StateNode prevNode = states.resolve(prevHead);
            prevNode.getLabels().remove("branch:" + branch);
            prevNode.fireChanged();
        }
    }
}
