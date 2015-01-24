/**
 * refs.scala
 *
 * Created on 20.01.2015
 */
package at.pria.koza.harmonic

import scala.collection.mutable.Map

import at.pria.koza.harmonic.proto.HarmonicP.StateP
import at.pria.koza.polybuf.PolybufException
import at.pria.koza.polybuf.PolybufInput
import at.pria.koza.polybuf.PolybufIO
import at.pria.koza.polybuf.PolybufOutput
import at.pria.koza.polybuf.PolybufSerializable
import at.pria.koza.polybuf.proto.Polybuf.Obj

import com.google.protobuf.GeneratedMessage.GeneratedExtension

/**
 * <p>
 * {@code refs}
 * </p>
 *
 * @version V0.0 20.01.2015
 * @author SillyFreak
 */
trait Ref {
  def state: State
}

object StateWrapper extends IOFactory[StateWrapper] {

  def getIO(implicit engine: Engine): PolybufIO[StateWrapper] = new IO()

  private class IO(implicit engine: Engine) extends PolybufIO[StateWrapper] {
    private val delegate = State.getIO(engine)

    override def extension: GeneratedExtension[Obj, _] = delegate.extension

    @throws[PolybufException]
    override def serialize(out: PolybufOutput, instance: StateWrapper, obj: Obj.Builder): Unit =
      delegate.serialize(out, instance.state, obj)

    @throws[PolybufException]
    override def initialize(in: PolybufInput, obj: Obj): StateWrapper = {
      val id = obj.getExtension(State.EXTENSION).getId()
      //handle states already present properly
      engine.wrappers.getOrElseUpdate(id, new StateWrapper(obj))
    }

    @throws[PolybufException]
    override def deserialize(in: PolybufInput, obj: Obj, instance: StateWrapper): Unit =
      delegate.deserialize(in, obj, instance.state)
  }
}

class StateWrapper private (val stateId: Long, val parentId: Long,
                            private var _action: Obj)(implicit engine: Engine)
    extends PolybufSerializable with Ref {
  //PolybufSerializable
  override def typeId: Int = State.FIELD

  //Ref
  //TODO use lazy?
  lazy override val state: State = {
    engine.states.get(stateId) match {
      case Some(state) =>
        state
      case None =>
        engine.wrappers.get(parentId) match {
          case Some(parent) =>
            new DerivedState(parent.state, stateId, _action)
          case None =>
            throw new IllegalStateException("state not resolvable: State@016X".format(stateId))
        }
    }
  }

  /**
   * <p>
   * Used to add states created by the engine managed by this branch manager.
   * </p>
   *
   * @param state the state to be added
   */
  def this(state: State) =
    this(
      state.id,
      state match {
        case root: RootState    => 0
        case node: DerivedState => node.parent.id
      },
      null)(state.engine)

  def this(state: StateP)(implicit engine: Engine) =
    this(state.getId(), state.getParent(), state.getAction())

  /**
   * <p>
   * Used to add states received from an engine other than managed by this branch manager.
   * {@linkplain #resolve() Resolving} will be necessary before this MetaState can be used.
   * </p>
   *
   * @param state the protobuf serialized form of the state to be added
   * @param action the action extracted from that protobuf extension
   */
  def this(state: Obj)(implicit engine: Engine) =
    this(state.getExtension(State.EXTENSION))
}
