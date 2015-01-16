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

abstract class State(val engine: Engine, val id: Long) extends PolybufSerializable {
  engine.putState(this)

  //PolybufSerializable
  override def typeId: Int = State.FIELD

  def commonPredecessor(other: State): State

  def engineId: Int = (id >> 32).toInt
}

class RootState(engine: Engine) extends State(engine, 0) {
  def commonPredecessor(other: State): State = this

  override def toString(): String = getClass().getSimpleName()
}

class DerivedState(val parent: State, id: Long, val actionObj: Obj) extends State(parent.engine, id) {
  private var _action: Action = _
  def action: Action = _action

  def this(parent: State, action: Action) = {
    this(
      parent,
      parent.engine.nextStateId(),
      try {
        new PolybufOutput(parent.engine.config).writeObject(action)
      } catch {
        case ex: PolybufException => throw new IllegalArgumentException(ex)
      })
    _action = action
  }

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

    //code taken from
    //http://twistedoakstudios.com/blog/Post3280_intersecting-linked-lists-faster

    // find *any* common node, and the distances to it
    var dist0 = 0
    var dist1 = 0

    {
      var node0 = this: State
      var node1 = other
      var stepSize = 1

      while (node0 != node1) {
        // advance each node progressively farther, watching for the other node
        breakable {
          for (_ <- 0 to stepSize) {
            if (node0 == node1) break
            node0 match {
              case root: RootState => break
              case node: DerivedState =>
                node0 = node.parent
                dist0 += 1
            }
          }
        }
        stepSize *= 2
        breakable {
          for (_ <- 0 to stepSize) {
            if (node0 == node1) break
            node1 match {
              case root: RootState => break
              case node: DerivedState =>
                node1 = node.parent
                dist1 += 1
            }
          }
        }
        stepSize *= 2
      }
    }

    var node0 = this: State
    var node1 = other
    // align heads to be an equal distance from the first common node
    var r = dist1 - dist0
    while (r < 0) {
      node0 = node0.asInstanceOf[DerivedState].parent
      r += 1
    }
    while (r > 0) {
      node1 = node1.asInstanceOf[DerivedState].parent
      r -= 1
    }

    // advance heads until they meet at the first common node
    while (node0 != node1) {
      node0 = node0.asInstanceOf[DerivedState].parent
      node1 = node1.asInstanceOf[DerivedState].parent
    }

    node0
  }

  override def toString(): String = {
    val actionType =
      engine.config.get(actionObj.getTypeId) match {
        case Some(io) => io.extension.getDescriptor().getMessageType().getName()
        case None     => null
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

    instance match {
      case root: RootState =>
      //handle the root state differently:
      //it has id zero, i.e. no original engine, no parent and no action
      //it's inherently different, and must be handled differently
      case node: DerivedState =>
        b.setParent(node.parent.id)
        b.setAction(node.actionObj)
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
    else if (id == 0) new RootState(engine)
    else new DerivedState(engine.state(p.getParent()), id, p.getAction())
  }
}
