/**
 * State.scala
 *
 * Created on 16.05.2013
 */

package at.pria.koza.harmonic

import scala.util.control.Breaks._

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
 * The class State.
 * </p>
 *
 * @version V0.0 16.05.2013
 * @author SillyFreak
 */
object State extends IOFactory[State] {
  val FIELD = StateP.STATE_FIELD_NUMBER
  val EXTENSION = StateP.state

  def getIO(implicit engine: Engine): PolybufIO[State] = new IO()

  private class IO(implicit engine: Engine) extends PolybufIO[State] {
    override def extension: GeneratedExtension[Obj, StateP] = EXTENSION

    @throws[PolybufException]
    override def serialize(out: PolybufOutput, instance: State, obj: Obj.Builder): Unit = {
      val b = StateP.newBuilder()
      b.setId(instance.id)

      instance match {
        case root: RootState =>
        //handle the root state differently:
        //it has id zero, i.e. no original engine, no parent and no action
        //it's inherently different, and must be handled differently
        case node: DerivedState =>
          b.setParent(node.parent.id)
          b.setAction(node.actionObj)
      }

      obj.setExtension(extension, b.build())
    }

    @throws[PolybufException]
    override def initialize(in: PolybufInput, obj: Obj): State = {
      val p = obj.getExtension(extension)
      val id = p.getId()
      //handle states already present properly
      engine.states.get(id) match {
        case Some(e) => e
        case None =>
          if (id == 0)
            throw new AssertionError("engine has no root state")
          new DerivedState(engine.states(p.getParent()), id, p.getAction())
      }
    }
  }
}

sealed abstract class State(val engine: Engine, val id: Long) extends PolybufSerializable with Ref {
  engine.States += this

  //PolybufSerializable
  override def typeId: Int = State.FIELD

  //Ref
  override def state = this

  val seq: List[State]
  val seqNoRoot: List[DerivedState]

  def parent: State
  def engineId: Int = (id >> 32).toInt

  /**
   * <p>
   * Computes and returns the nearest common predecessor between this and another state. More formally, this
   * returns the state that is a predecessor of both `this` and `other`, but whose children are not.
   * </p>
   * <p>
   * This method may return the root state of the engine, but never `null`.
   * </p>
   *
   * @param other the other state for which to find the nearest common predecessor
   * @return the nearest common predecessor state
   * @see <a href="http://twistedoakstudios.com/blog/Post3280__">Algorithm source</a>
   */
  def commonPredecessor(other: State): State = {
    if (engine != other.engine) throw new IllegalArgumentException()
    //the root state is shared in an engine, so ???.head is safe
    else commonTail(this.seq, other.seq).head
  }
}

class RootState(engine: Engine) extends State(engine, 0) {
  override val seq: List[State] = this :: Nil
  override val seqNoRoot: List[DerivedState] = Nil

  def parent: State = throw new NoSuchElementException()

  override def toString(): String = getClass().getSimpleName()
}

class DerivedState(val parent: State, id: Long, val actionObj: Obj) extends State(parent.engine, id) {
  private var _action: Action = _
  def action: Action = _action

  def this(parent: State, action: Action) = {
    this(
      parent,
      parent.engine.States.nextStateId(),
      try {
        new PolybufOutput(parent.engine.config).writeObject(action)
      } catch {
        case ex: PolybufException => throw new IllegalArgumentException(ex)
      })
  }

  override val seq: List[State] = this :: parent.seq
  override val seqNoRoot: List[DerivedState] = this :: parent.seqNoRoot

  def apply(): Unit = {
    assert(_action == null)
    try {
      _action = new PolybufInput(engine.config).readObject(actionObj).asInstanceOf[Action]
    } catch {
      case ex: PolybufException => throw new AssertionError(ex)
    }
    _action.apply()
  }

  def revert(): Unit = {
    assert(_action != null)
    _action.revert()
    _action = null
  }

  override def toString(): String = {
    val actionType =
      engine.config.get(actionObj.getTypeId) match {
        case Some(io) => io.extension.getDescriptor().getMessageType().getName()
        case None     => null
      }
    "%s@%016X: %s".format(getClass().getSimpleName(), id, actionType)
  }
}
