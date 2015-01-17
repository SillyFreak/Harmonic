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

  /**
   * <p>
   * Computes and returns the longest common tail of two `Seq`s. If two (finite) `Seq`s are otherwise separate,
   * their common tail will be `Nil`.
   * </p>
   * <p>
   * Note that this method checks the sequences' elements, not the actual links used. It is therefore important
   * that the lists do not contain equal elements other than in their common tail. Consider these examples:
   * </p>
   *
   * {{{
   * commonTail("4321", "54321");      //ok: common tail is ("4321": Seq[Char])
   * commonTail("1234", "12345");      //undefined: elements "1234" are common, but not part of a common tail
   * commonTail("123_456", "abc_def"); //undefined: element '_' is common, but not part of a common tail
   * commonTail(Stream.from(2),
   *            Stream.from(4));       //ok: common tail is Stream.from(4)
   * commonTail(Stream.from(2, 2),
   *            Stream.from(4, 3));    //undefined: element 4 is common, but not part of a common tail
   * commonTail(Stream.from(1, 2),
   *            Stream.from(2, 2));    //ok, but doesn't terminate
   * }}}
   *
   * @param as the first sequence
   * @param bs the first sequence
   * @return their common tail, or `Nil` for finite, separate sequences
   *
   * @see <a href="http://twistedoakstudios.com/blog/Post3280__">Algorithm source</a>
   */
  def commonTail[T](as: Seq[T], bs: Seq[T]): Seq[T] = {
    //code taken from
    //http://twistedoakstudios.com/blog/Post3280_intersecting-linked-lists-faster

    // find *any* common node, and the distances to it
    val r = {
      val lists = Array(as, bs)
      val dists = Array(0, 0)
      var stepSize = 1

      while (lists(0).headOption != lists(1).headOption) {
        // advance each node progressively farther, watching for the other node
        for (i <- 0 to 1) {
          breakable {
            for (_ <- 1 to stepSize) {
              if (lists(0).headOption == lists(1).headOption) break
              else if (lists(i).isEmpty) break
              else {
                dists(i) += 1
                lists(i) = lists(i).tail
              }
            }
          }
          stepSize *= 2
        }
      }
      dists(1) - dists(0)
    }

    // align heads to be an equal distance from the first common node
    var _as = as.drop(-r)
    var _bs = bs.drop(r)

    // advance heads until they meet at the first common node
    while (_as.headOption != _bs.headOption) {
      // at this point heads are aligned, so none of the lists may be Nil without the loop breaking before this
      _as = _as.tail
      _bs = _bs.tail
    }

    _as
  }
}

abstract class State(val engine: Engine, val id: Long) extends PolybufSerializable {
  engine.putState(this)

  //PolybufSerializable
  override def typeId: Int = State.FIELD

  def seq: List[State]
  def seqNoRoot: List[DerivedState]

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
    else State.commonTail(this.seq, other.seq).head
  }
}

class RootState(engine: Engine) extends State(engine, 0) {
  def seq: List[State] = this :: Nil
  def seqNoRoot: List[DerivedState] = Nil

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

  def seq: List[State] = this :: parent.seq
  def seqNoRoot: List[DerivedState] = this :: parent.seqNoRoot

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
