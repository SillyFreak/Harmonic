/**
 * JGroupsBranchAdapter.java
 * 
 * Created on 04.08.2013
 */

package at.pria.koza.harmonic.jGroups;


import static at.pria.koza.harmonic.proto.HarmonicP.SyncP.Type.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;

import at.pria.koza.harmonic.BranchManager;
import at.pria.koza.harmonic.BranchManager.SyncCallback;
import at.pria.koza.harmonic.State;
import at.pria.koza.harmonic.proto.HarmonicP.SyncP;
import at.pria.koza.polybuf.proto.Polybuf.Obj;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.GeneratedMessage.GeneratedExtension;


/**
 * <p>
 * {@code JGroupsBranchAdapter}
 * </p>
 * 
 * @version V0.0 04.08.2013
 * @author SillyFreak
 */
public class JGroupsBranchAdapter extends ReceiverAdapter {
    private final JChannel          ch;
    private final BranchManager     mgr;
    private final ExtensionRegistry reg;
    
    public JGroupsBranchAdapter(JChannel ch, BranchManager mgr) {
        this.ch = ch;
        this.mgr = mgr;
        reg = ExtensionRegistry.newInstance();
        reg.add(State.EXTENSION);
    }
    
    public void register(GeneratedExtension<?, ?> ext) {
        reg.add(ext);
    }
    
    @Override
    public void receive(Message msg) {
        try {
            Address src = msg.getSrc();
            ByteArrayInputStream bais = new ByteArrayInputStream(msg.getRawBuffer(), msg.getOffset(),
                    msg.getLength());
            SyncP m = SyncP.parseFrom(bais, reg);
            
            switch(m.getType()) {
                case RECEIVE_UPDATE: {
                    long[] ancestors = new long[m.getStateIdsCount()];
                    for(int i = 0; i < ancestors.length; i++)
                        ancestors[i] = m.getStateIds(i);
                    receiveUpdate(src, m.getEngine(), m.getBranch(), m.getStates(0), ancestors);
                    break;
                }
                case SEND_MISSING: {
                    sendMissing(src, m.getEngine(), m.getBranch(), m.getStateIds(0));
                    break;
                }
                case RECEIVE_MISSING: {
                    Obj[] ancestors = m.getStatesList().toArray(new Obj[m.getStatesCount()]);
                    receiveMissing(src, m.getEngine(), m.getBranch(), m.getStateIds(0), ancestors);
                    break;
                }
                default:
                    throw new AssertionError();
            }
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }
    
    private void send(Address dst, SyncP message) {
        try {
            ch.send(new Message(dst, message.toByteArray()));
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void sendUpdate(Address dst, int engine, String branch) {
        mgr.sendUpdate(engine, branch, new Callback(dst));
    }
    
    protected void receiveUpdate(Address src, int engine, String branch, Obj state, long... ancestors) {
        mgr.receiveUpdate(engine, branch, state, ancestors, new Callback(src));
    }
    
    protected void sendMissing(Address dst, int engine, String branch, long ancestor) {
        mgr.sendMissing(engine, branch, ancestor, new Callback(dst));
    }
    
    protected void receiveMissing(Address src, int engine, String branch, long state, Obj... ancestors) {
        mgr.receiveMissing(engine, branch, state, ancestors);
    }
    
    private class Callback implements SyncCallback {
        private final Address dst;
        
        public Callback(Address dst) {
            this.dst = dst;
        }
        
        @Override
        public void sendUpdateCallback(int engine, String branch, Obj state, long... ancestors) {
            SyncP.Builder b = SyncP.newBuilder();
            b.setType(RECEIVE_UPDATE);
            b.setEngine(engine);
            b.setBranch(branch);
            b.addStates(state);
            for(long ancestor:ancestors)
                b.addStateIds(ancestor);
            send(dst, b.build());
        }
        
        @Override
        public void receiveUpdateCallback(int engine, String branch, long ancestor) {
            SyncP.Builder b = SyncP.newBuilder();
            b.setType(SEND_MISSING);
            b.setEngine(engine);
            b.setBranch(branch);
            b.addStateIds(ancestor);
            send(dst, b.build());
        }
        
        @Override
        public void sendMissingCallback(int engine, String branch, long state, Obj... ancestors) {
            SyncP.Builder b = SyncP.newBuilder();
            b.setType(RECEIVE_MISSING);
            b.setEngine(engine);
            b.setBranch(branch);
            b.addStateIds(state);
            for(Obj ancestor:ancestors)
                b.addStates(ancestor);
            send(dst, b.build());
        }
    }
}
