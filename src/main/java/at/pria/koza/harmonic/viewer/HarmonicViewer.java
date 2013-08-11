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

import at.pria.koza.harmonic.Engine;
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
        for(State state:engine.getStates().values())
            states.resolve(state);
        makeVisible(states.resolve(engine.getHead()));
    }
    
    private void makeVisible(StateNode node) {
        statesTree.makeVisible(new TreePath(node.getPath()));
    }
    
    private class Listener implements StateListener {
        @Override
        public void stateAdded(State state) {
            StateNode node = states.resolve(state);
            makeVisible(node);
        }
    }
}
