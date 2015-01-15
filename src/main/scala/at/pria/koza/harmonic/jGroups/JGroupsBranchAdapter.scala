/**
 * JGroupsBranchAdapter.scala
 *
 * Created on 04.08.2013
 */

package at.pria.koza.harmonic.jGroups

import at.pria.koza.harmonic.proto.HarmonicP.SyncP.Type._

import java.io.ByteArrayInputStream
import java.io.IOException

import org.jgroups.Address
import org.jgroups.JChannel
import org.jgroups.Message
import org.jgroups.ReceiverAdapter

import at.pria.koza.harmonic.BranchManager
import at.pria.koza.harmonic.BranchManager.SyncCallback
import at.pria.koza.harmonic.proto.HarmonicP.SyncP
import at.pria.koza.polybuf.proto.Polybuf.Obj

/**
 * <p>
 * {@code JGroupsBranchAdapter}
 * </p>
 *
 * @version V0.0 04.08.2013
 * @author SillyFreak
 */
class JGroupsBranchAdapter(ch: JChannel, mgr: BranchManager) extends ReceiverAdapter {
  override def receive(msg: Message): Unit = {
    try {
      val src = msg.getSrc()
      val bais = new ByteArrayInputStream(msg.getRawBuffer(), msg.getOffset(), msg.getLength())
      val m = SyncP.parseFrom(bais, mgr.getEngine().getConfig().registry)

      m.getType match {
        case RECEIVE_UPDATE => {
          val ancestors = new Array[Long](m.getStateIdsCount())
          for (i <- 0 to ancestors.length)
            ancestors(i) = m.getStateIds(i)
          receiveUpdate(src, m.getEngine(), m.getBranch(), m.getStates(0), ancestors: _*)
        }
        case SEND_MISSING => {
          sendMissing(src, m.getEngine(), m.getBranch(), m.getStateIds(0))
        }
        case RECEIVE_MISSING => {
          val ancestors = m.getStatesList().toArray(new Array[Obj](m.getStatesCount()))
          receiveMissing(src, m.getEngine(), m.getBranch(), m.getStateIds(0), ancestors: _*)
        }
        case _ =>
          throw new AssertionError()
      }
    } catch {
      case ex: IOException =>
        ex.printStackTrace()
    }
  }

  private def send(dst: Address, message: SyncP): Unit = {
    try {
      ch.send(new Message(dst, message.toByteArray()))
    } catch {
      case ex: Exception =>
        ex.printStackTrace()
    }
  }

  def sendUpdate(dst: Address, engine: Int, branch: String): Unit = {
    mgr.sendUpdate(engine, branch, new Callback(dst));
  }

  protected def receiveUpdate(src: Address, engine: Int, branch: String, state: Obj, ancestors: Long*): Unit = {
    mgr.receiveUpdate(engine, branch, state, ancestors, new Callback(src));
  }

  protected def sendMissing(dst: Address, engine: Int, branch: String, ancestor: Long): Unit = {
    mgr.sendMissing(engine, branch, ancestor, new Callback(dst));
  }

  protected def receiveMissing(src: Address, engine: Int, branch: String, state: Long, ancestors: Obj*): Unit = {
    mgr.receiveMissing(engine, branch, state, ancestors);
  }

  private class Callback(dst: Address) extends SyncCallback {
    override def sendUpdateCallback(engine: Int, branch: String, state: Obj, ancestors: Long*): Unit = {
      def b = SyncP.newBuilder();
      b.setType(RECEIVE_UPDATE);
      b.setEngine(engine);
      b.setBranch(branch);
      b.addStates(state);
      for (ancestor <- ancestors)
        b.addStateIds(ancestor);
      send(dst, b.build());
    }

    override def receiveUpdateCallback(engine: Int, branch: String, ancestor: Long): Unit = {
      def b = SyncP.newBuilder();
      b.setType(SEND_MISSING);
      b.setEngine(engine);
      b.setBranch(branch);
      b.addStateIds(ancestor);
      send(dst, b.build());
    }

    override def sendMissingCallback(engine: Int, branch: String, state: Long, ancestors: Obj*): Unit = {
      def b = SyncP.newBuilder();
      b.setType(RECEIVE_MISSING);
      b.setEngine(engine);
      b.setBranch(branch);
      b.addStateIds(state);
      for (ancestor <- ancestors)
        b.addStates(ancestor);
      send(dst, b.build());
    }
  }
}
