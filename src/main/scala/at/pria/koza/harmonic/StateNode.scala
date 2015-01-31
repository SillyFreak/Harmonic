/**
 * StateNode.scala
 *
 * Created on 16.05.2013
 */

package at.pria.koza.harmonic

import java.lang.String._

import at.pria.koza.harmonic.proto.HarmonicP.StateP
import at.pria.koza.polybuf.PolybufConfig
import at.pria.koza.polybuf.PolybufException
import at.pria.koza.polybuf.PolybufIO
import at.pria.koza.polybuf.PolybufInput
import at.pria.koza.polybuf.PolybufOutput
import at.pria.koza.polybuf.PolybufSerializable
import at.pria.koza.polybuf.proto.Polybuf
import at.pria.koza.polybuf.proto.Polybuf.Obj

import com.google.protobuf.GeneratedMessage.GeneratedExtension

/**
 * <p>
 * The class StateNode.
 * </p>
 *
 * @version V0.0 16.05.2013
 * @author SillyFreak
 */
object StateNode extends IOFactory[StateNode] {
  val FIELD = StateP.STATE_FIELD_NUMBER
  val EXTENSION = StateP.state

  def getIO(implicit engine: Engine): PolybufIO[StateNode] = new IO()

  private class IO(implicit engine: Engine) extends PolybufIO[StateNode] {
    override def extension: GeneratedExtension[Obj, StateP] = EXTENSION

    @throws[PolybufException]
    override def serialize(out: PolybufOutput, instance: StateNode, obj: Obj.Builder): Unit = {
      val b = StateP.newBuilder()
      b.setId(instance.id)
      b.setParent(instance.parentId)
      b.setAction(instance.action)

      obj.setExtension(extension, b.build())
    }

    @throws[PolybufException]
    override def initialize(in: PolybufInput, obj: Obj): StateNode = {
      val p = obj.getExtension(extension)
      engine.states.get(p.getId()) match {
        case Some(Nil)   => throw new AssertionError("trying to deserialize the root state")
        case Some(state) => state.head
        case None =>
          val node = new StateNode(p.getId(), p.getParent(), p.getAction())
          engine.States += node
          node
      }
    }
  }
}

case class StateNode private[harmonic] (val id: Long, val parentId: Long, val action: Obj) extends PolybufSerializable {
  //PolybufSerializable
  override def typeId: Int = StateNode.FIELD

  def this(id: Long, parentId: Long, action: Action)(implicit engine: Engine) =
    this(
      id,
      parentId,
      try {
        new PolybufOutput(engine.config).writeObject(action)
      } catch {
        case ex: PolybufException => throw new IllegalArgumentException(
          "can't serialize action; try registering the IOFactory", ex);
      })

  def engineId: Int = (id >> 32).toInt

  def deserializedAction(implicit engine: Engine): Action =
    try {
      new PolybufInput(engine.config).readObject(action).asInstanceOf[Action]
    } catch {
      case ex: PolybufException => throw new IllegalStateException(
        "can't deserialize action; maybe the engine is in a state where the action is not defined", ex);
    }

  override def toString(): String = "%s@%016X".format(getClass().getSimpleName(), id)

  ///**
  // * <p>
  // * Computes and returns the nearest common predecessor between this and another state. More formally, this
  // * returns the state that is a predecessor of both `this` and `other`, but whose children are not.
  // * </p>
  // * <p>
  // * This method may return the root state of the engine, but never `null`.
  // * </p>
  // *
  // * @param other the other state for which to find the nearest common predecessor
  // * @return the nearest common predecessor state
  // * @see <a href="http://twistedoakstudios.com/blog/Post3280__">Algorithm source</a>
  // */
  //def commonPredecessor(other: State): State = {
  //  if (engine != other.engine) throw new IllegalArgumentException()
  //  //the root state is shared in an engine, so ???.head is safe
  //  else State.commonTail(this.seq, other.seq).head
  //}
}
