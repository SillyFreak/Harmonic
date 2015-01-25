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

  /**
   * <p>
   * Computes and returns the longest common tail of two `Seq`s. For elements contained in both sequences, element
   * equality must imply link equality in these sequences, that means, if two sequences' heads are equal, their
   * tails must be equal as well. Without this limitation, either infinite sequences could not be handled, because
   * comparing the tails takes infinite time, or alternatively reference equality would have to be used to compare
   * links (not elements), because one and the same link can only have one tail. The element equality limitation is
   * the most practical and intuitive one, and thus the one used.
   * </p>
   * <p>
   * If two finite `Seq`s are otherwise separate, their common tail will be `Nil`. A finite and an infinite
   * sequence don't have a common tail, not even `Nil`, so this method won't terminate in that case. The same holds
   * for two infinite sequences without a common tail.
   * </p>
   * <p>
   * If both sequences are infinite, they may not end in the same circle. Different circles, or not ending in a
   * circle, are permissible, as long as the equality constraint holds, but will then lead to non-termination.
   * Consider two sequences `a (c d)` and `b (d c)`. They end in the same circle, but the head of their longest
   * common tail is not defined. For sequences such as `a c (d e)` and `b c (d e)`, the longest common tail is
   * unambiguous, but can't be reliably computed with the used algorithm.
   * </p>
   *
   * {{{
   * //ok: common tail is "321"
   * commonTail("321", "4321")
   *
   * //undefined: element '1' has tails Nil and "234" at second
   * commonTail("1", "1234")
   *
   * //undefined: element '_' has tails "34" and "cd" at different occurrences
   * commonTail("12_34", "ab_cd")
   *
   * //ok: element '0' has tails "01" and "1" at different occurrences, but does not appear in the second list
   * //common tail is "1"
   * commonTail("001", "1")
   *
   * //undefined: element '1' appears in both sequences, but has tails "001" and Nil at different occurrences
   * commonTail("1001", "1")
   *
   * //ok: common tail is Stream.from(2)
   * commonTail(Stream.from(0), Stream.from(2))
   *
   * //undefined: element 0 has tails Stream.from(2, 2) and Stream.from(3, 3) at different occurrences
   * commonTail(Stream.from(0, 2), Stream.from(0, 3))
   *
   * //ok, but no common tail: doesn't terminate
   * commonTail(Nil, Stream.from(0));
   *
   * //ok, but no common tail: doesn't terminate
   * commonTail(Stream.from(1, 2), Stream.from(2, 2));
   *
   * //ok, one of the sequences ends in a circle. no common tail: doesn't terminate
   * commonTail(Stream.continually(0), Nil)
   *
   * //ok, both sequences end in different circles. no common tail: doesn't terminate
   * commonTail(Stream.continually(0), Stream.continually(1))
   *
   * //undefined: both sequences end in the same circle
   * commonTail(Stream.continually(0), Stream.continually(0))
   *
   * //the sequence 0, 1, 0, 1, ...
   * val alternate = Stream.continually(List(0, 1)).flatten
   *
   * //undefined: both sequences end in the same circle
   * commonTail(alternate, alternate.drop(1))
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
    else State.commonTail(this.seq, other.seq).head
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
