/**
 * LocalEngine.scala
 *
 * Created on 30.01.2015
 */

package at.pria.koza.harmonic.local

import java.io.IOException

import at.pria.koza.harmonic.Engine
import at.pria.koza.harmonic.RemoteEngine
import at.pria.koza.harmonic.State
import at.pria.koza.harmonic.StateNode
import at.pria.koza.polybuf.RemoteEngineException
import at.pria.koza.polybuf.proto.Polybuf.Obj

/**
 * <p>
 * `LocalEngine` is an implementation of `RemoteEngine` that connects to an engine in the same JVM, therefore not
 * needing I/O for communication.
 * </p>
 *
 * @version V0.0 30.01.2015
 * @author SillyFreak
 */
class LocalEngine(val engine: Engine) extends RemoteEngine {
  private var _heads = Map[String, Long]()
  override def heads = _heads

  override def push(heads: Map[String, Long]): Unit = ???

  override def fetch(): Map[String, Long] = {
    _heads = Map[String, Long](engine.Branches.branchIterator.map { x => x.name -> x.tip.id }.toSeq: _*)
    heads
  }

  @throws[RemoteEngineException]
  override def retrieveAncestors(state: Long): List[Long] =
    engine.states.get(state) match {
      case Some(state) =>
        if (state.root)
          throw new RemoteEngineException("asking a remote for the root state's ancestors")
        else {
          def stateIds(state: State): List[Long] =
            state.id :: (if (state.root) Nil else stateIds(state.parent))
          stateIds(state.parent)
        }
      case None =>
        throw new RemoteEngineException("No such state: %016X".format(state))
    }

  @throws[RemoteEngineException]
  override def retrieveStates(states: Seq[Long]): List[StateNode] =
    states.map { state =>
      engine.states.get(state) match {
        case Some(state) =>
          if (state.root)
            throw new RemoteEngineException("asking a remote for the root state's ancestors")
          else
            state.state
        case None =>
          throw new RemoteEngineException("No such state: %016X".format(state))
      }
    }.toList
}
