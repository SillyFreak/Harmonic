/**
 * StateNode.scala
 *
 * Created on 31.01.2015
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
 * The `StateNode` object provides an `IOFactory` to serialize `StateNode`.
 * </p>
 *
 * @version V1.0 31.01.2015
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
        case Some(state) =>
          if (state.root) throw new AssertionError("trying to deserialize the root state")
          else state.state
        case None =>
          val node = new StateNode(p.getId(), p.getParent(), p.getAction())
          engine.States += node
          node
      }
    }
  }
}

/**
 * <p>
 * The `StateNode` class represents one node in an engine's history. It is typically accessed through `State`,
 * which also allows accessing the parent state directly, while StateNode only exposes the parent's ID. Informally,
 * "state" can refer to both a `State` and a `StateNode`.
 * </p>
 *
 * @version V1.0 31.01.2015
 * @author SillyFreak
 */
case class StateNode private[harmonic] (val id: Long, val parentId: Long, val action: Obj) extends PolybufSerializable {
  //PolybufSerializable
  override def typeId: Int = StateNode.FIELD

  /**
   * Creates a StateNode using an `Action` object. The implicit engine must be able to serialize the action. The
   * given action is usually discarded: even if the state is applied directly after creation, a new action is
   * freshly deserialized for that purpose.
   */
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

  /**
   * The ID of the engine that originally created this state. This is simply the upper 32 bits of the state
   * id.
   */
  def engineId: Int = (id >> 32).toInt

  /**
   * deserializes the state's action using the implicit engine's `PolybufConfig`. As the action may reference
   * entities in the engine, this method may fail if the engine is in a state other than before executing the
   * stored action.
   */
  def deserializedAction(implicit engine: Engine): Action =
    try {
      new PolybufInput(engine.config).readObject(action).asInstanceOf[Action]
    } catch {
      case ex: PolybufException => throw new IllegalStateException(
        "can't deserialize action; maybe the engine is in a state where the action is not defined", ex);
    }

  override def toString(): String = "%s@%016X".format(getClass().getSimpleName(), id)
}
