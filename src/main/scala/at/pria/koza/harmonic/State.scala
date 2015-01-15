/**
 * State.scala
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
import at.pria.koza.polybuf.proto.Polybuf.Obj
import scala.util.control.Breaks._
import com.google.protobuf.GeneratedMessage.GeneratedExtension
import at.pria.koza.polybuf.proto.Polybuf

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
class State(engine: Engine, parent: State, id: Long, action: Obj) extends PolybufSerializable {
  private val _engine = engine
  private val _id = id
  private val _parent = parent

  private val _actionObj = action
  private var _action: Action = _

  engine.putState(this)

  /**
   * <p>
   * Creates a root state for the given engine.
   * </p>
   *
   * @param engine the engine for which this is the root state
   */
  private[harmonic] def this(engine: Engine) = this(engine, null, 0, null)

  /**
   * <p>
   * Creates a new state, using the {@linkplain Engine#nextStateId() next generated state ID} for that engine.
   * </p>
   *
   * @param parent the parent state for this new state
   * @param action the action leading to this new state
   */
  private[harmonic] def this(parent: State, action: Action) =
    this(
      parent.getEngine(),
      parent,
      parent.getEngine().nextStateId(),
      try {
        new PolybufOutput(parent.getEngine().getConfig()).writeObject(action);
      } catch {
        case ex: PolybufException => throw new IllegalArgumentException(ex)
      })

  /**
   * <p>
   * Returns the engine in which this state resides. This may be different from the engine which originally
   * created the state.
   * </p>
   *
   * @return the engine in which this state resides
   */
  def getEngine(): Engine = _engine

  /**
   * <p>
   * Returns the unique ID assigned to this state. For the root state of any engine, this is zero. Otherwise, the
   * upper four bytes identify the original engine that created it, the lower four bytes is a sequentially
   * assigned number chosen by that engine.
   * </p>
   *
   * @return the unique ID assigned to this state
   */
  def getId(): Long = _id

  /**
   * <p>
   * Returns the id of the engine that created this state, extracted from this state's id.
   * </p>
   *
   * @return
   */
  def getEngineId(): Int = (_id >> 32).toInt

  /**
   * <p>
   * Returns this state's parent. Only the root state has {@code null} as its parent.
   * </p>
   *
   * @return this state's parent
   */
  def getParent(): State = _parent

  def apply(): Unit = {
    assert(_action == null)
    try {
      _action = new PolybufInput(_engine.getConfig()).readObject(_actionObj).asInstanceOf[Action]
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
  def getAction(): Action = _action

  def getActionObj(): Polybuf.Obj = _actionObj

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
  def getCommonPredecessor(other: State): State = {
    if (getEngine() != other.getEngine()) throw new IllegalArgumentException()

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
          if (node0.getId() == 0) break
          if (node0 == node1) break
          node0 = node0.getParent()
          dist0 += 1
        }
      }
      stepSize *= 2
      breakable {
        for (i <- 0 to stepSize) {
          if (node1.getId() == 0) break
          if (node0 == node1) break
          node1 = node1.getParent()
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
      node0 = node0.getParent()
      r += 1
    }
    while (r > 0) {
      node1 = node1.getParent()
      r -= 1
    }

    // advance heads until they meet at the first common node
    while (node0 != node1) {
      node0 = node0.getParent()
      node1 = node1.getParent()
    }

    node0
  }

  override def toString(): String = {
    val actionType =
      if (_actionObj == null) null
      else engine.getConfig().get(_actionObj.getTypeId()) match {
        case Some(io) =>
          io.extension.getDescriptor().getMessageType().getName()
        case None =>
          null
      }
    format("%s@%016X: %s", (getClass().getSimpleName(), _id, actionType))
  }
}

private class IO(engine: Engine) extends PolybufIO[State] {
  override def typeId: Int = State.FIELD

  override def extension: GeneratedExtension[Obj, StateP] = State.EXTENSION

  @throws[PolybufException]
  override def serialize(out: PolybufOutput, instance: State, obj: Obj.Builder): Unit = {
    val b = StateP.newBuilder()
    b.setId(instance.getId())
    //handle the root state differently:
    //it has id zero, i.e. no original engine, no parent and no action
    //it's inherently different, and must be handled differently
    if (instance.getId() != 0l) {
      b.setParent(instance.getParent().getId())
      b.setAction(instance.getActionObj())
    }

    obj.setExtension(State.EXTENSION, b.build())
  }

  @throws[PolybufException]
  override def initialize(in: PolybufInput, obj: Obj): State = {
    val p = obj.getExtension(State.EXTENSION)
    val id = p.getId()
    //handle states already present properly
    val result = engine.getState(id)

    if (result != null) result
    else new State(engine, engine.getState(p.getParent()), id, p.getAction())
  }

  @throws[PolybufException]
  override def deserialize(in: PolybufInput, obj: Obj, instance: State): Unit = {}
}
