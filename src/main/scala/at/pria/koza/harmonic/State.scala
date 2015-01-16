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
object State {
  val FIELD = StateP.STATE_FIELD_NUMBER
  val EXTENSION = StateP.state

  def getIO(engine: Engine): PolybufIO[State] = new IO(engine)

  def configure(config: PolybufConfig, engine: Engine): Unit = {
    config.add(getIO(engine))
  }
}

/**
 * <p>
 * This constructor is only directly called by the {@link IO}. In contrast to a newly created state, a
 * deserialized state has an ID assigned by a different engine.
 * </p>
 *
 * @param engine the engine for which this state is created/deserialized
 * @param parent the parent state for this state
 * @param id the ID assigned to this state by the engine that originally created it
 * @param action the action leading to this state
 */
class State(val engine: Engine, val parent: State, val id: Long, private[harmonic] val actionObj: Obj) extends PolybufSerializable {
  private var _action: Action = _
  engine.putState(this)

  /**
   * <p>
   * Creates a root state for the given engine.
   * </p>
   *
   * @param engine the engine for which this is the root state
   */
  private[harmonic] def this(_engine: Engine) = this(_engine, null, 0, null)

  /**
   * <p>
   * Creates a new state, using the {@linkplain Engine#nextStateId() next generated state ID} for that engine.
   * </p>
   *
   * @param parent the parent state for this new state
   * @param action the action leading to this new state
   */
  private[harmonic] def this(_parent: State, _action: Action) =
    this(
      _parent.engine,
      _parent,
      _parent.engine.nextStateId(),
      try {
        new PolybufOutput(_parent.engine.config).writeObject(_action)
      } catch {
        case ex: PolybufException => throw new IllegalArgumentException(ex)
      })

  /**
   * <p>
   * Returns the id of the engine that created this state, extracted from this state's id.
   * </p>
   *
   * @return
   */
  def engineId: Int = (id >> 32).toInt

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

  /**
   * <p>
   * Returns the action that led from the parent to this state. This method will return {@code null} if in the
   * current head state of the engine, the action was not executed.
   * </p>
   *
   * @return the action that led from the parent to this state
   */
  def action: Action = _action

  override def typeId: Int = State.FIELD

  /**
   * <p>
   * Computes and returns the nearest common predecessor between this and another state. More formally, this
   * returns the state that is a predecessor of both {@code this} and {@code other}, but whose children are not.
   * </p>
   * <p>
   * This method may return the root state of the engine, but never {@code null}.
   * </p>
   *
   * @param other the other state for which to find the nearest common predecessor
   * @return the nearest common predecessor state
   * @see <a href="http://twistedoakstudios.com/blog/Post3280__">Algorithm source</a>
   */
  def commonPredecessor(other: State): State = {
    if (engine != other.engine) throw new IllegalArgumentException()

    //code taken from
    //http://twistedoakstudios.com/blog/Post3280_intersecting-linked-lists-faster

    // find *any* common node, and the distances to it
    var node0 = this
    var node1 = other
    var dist0 = 0
    var dist1 = 0
    var stepSize = 1

    while (node0 != node1) {
      // advance each node progressively farther, watching for the other node
      breakable {
        for (i <- 0 to stepSize) {
          if (node0.id == 0) break
          if (node0 == node1) break
          node0 = node0.parent
          dist0 += 1
        }
      }
      stepSize *= 2
      breakable {
        for (i <- 0 to stepSize) {
          if (node1.id == 0) break
          if (node0 == node1) break
          node1 = node1.parent
          dist1 += 1
        }
      }
      stepSize *= 2
    }

    node0 = this
    node1 = other
    // align heads to be an equal distance from the first common node
    var r = dist1 - dist0
    while (r < 0) {
      node0 = node0.parent
      r += 1
    }
    while (r > 0) {
      node1 = node1.parent
      r -= 1
    }

    // advance heads until they meet at the first common node
    while (node0 != node1) {
      node0 = node0.parent
      node1 = node1.parent
    }

    node0
  }

  override def toString(): String = {
    val actionType =
      if (actionObj == null) null
      else engine.config.get(actionObj.getTypeId) match {
        case Some(io) =>
          io.extension.getDescriptor().getMessageType().getName()
        case None =>
          null
      }
    format("%s@%016X: %s", (getClass().getSimpleName(), id, actionType))
  }
}

private class IO(engine: Engine) extends PolybufIO[State] {
  override def extension: GeneratedExtension[Obj, StateP] = State.EXTENSION

  @throws[PolybufException]
  override def serialize(out: PolybufOutput, instance: State, obj: Obj.Builder): Unit = {
    val b = StateP.newBuilder()
    b.setId(instance.id)
    //handle the root state differently:
    //it has id zero, i.e. no original engine, no parent and no action
    //it's inherently different, and must be handled differently
    if (instance.id != 0l) {
      b.setParent(instance.parent.id)
      b.setAction(instance.actionObj)
    }

    obj.setExtension(State.EXTENSION, b.build())
  }

  @throws[PolybufException]
  override def initialize(in: PolybufInput, obj: Obj): State = {
    val p = obj.getExtension(State.EXTENSION)
    val id = p.getId()
    //handle states already present properly
    val result = engine.state(id)

    if (result != null) result
    else new State(engine, engine.state(p.getParent()), id, p.getAction())
  }
}
