/**
 * RemoteEngine.scala
 *
 * Created on 27.01.2015
 */

package at.pria.koza.harmonic

import scala.collection.mutable
import scala.util.control.Breaks._
import at.pria.koza.polybuf.RemoteEngineException
import at.pria.koza.polybuf.proto.Polybuf.Obj
import java.io.IOException
import java.util.EventListener
import at.pria.koza.harmonic.util.ListenerManager

/**
 * <p>
 * A `RemoteEngine` is a handle to a remote engine and used to synchronize multiple engines. The trait is defined
 * to allow similar communication to git, i.e. push (sender initiated) and fetch (receiver initiated). Note that
 * git's pull is just a fetch followed by a merge/rebase (local operation). Harmonic does not directly model this.
 * </p>
 * <p>
 * First some terminology:
 * </p>
 * <ul>
 * <li>Local engine: the engine on which an application is working</li>
 * <li>Remote engine: another engine, usually in a different JVM, that is to be synchronized with the local engine
 * using Harmonic</li>
 * <li>Remote, `RemoteEngine`: an endpoint that connects to a remote engine.</li>
 * </ul>
 * <p>
 * Every remote stores a list of the remote engine's branches, similar to git's remote tracking branches. These are
 * stored directly inside the `RemoteEngine`. While a remote branch on git points to a real commit, i.e. the whole
 * history is known, remote branches in Harmonic are just a name and a state ID. Downloading a remote branch's
 * states, and synchronizing any local branches with it, happens at the engine's discretion at a later point in
 * time.
 * </p>
 * <p>
 * These communication methods are supported:
 * </p>
 * <ul>
 * <li>TODO Push: the local engine sends a map of branch names and IDs to a remote engine's remote; the remote
 * stores those, and usually notifies the engine it belongs to.</li>
 * <li>Fetch: the remote requests a map of branch names and IDs from the remote engine it connects to, stores
 * those, and usually notifies the local engine.</li>
 * <li>Retrieve ancestors: the remote requests a sequence of ancestors of an ID. This is followed by "retrieve
 * states," or another "retrieve ancestors" if the ancestors did not contain any state IDs known to the local
 * engine.</li>
 * <li>Retrieve states: the remote requests the `StateNode`s associated with the unknown states; they are then
 * added to the local engine.</li>
 *
 * @version V0.0 27.01.2015
 * @author SillyFreak
 */
trait RemoteEngine extends ListenerManager[RemoteEngineListener] {
  private var _heads = Map[String, Long]()
  def heads = _heads
  protected[this] def heads_=(newHeads: Map[String, Long]) = {
    val oldHeads = _heads
    _heads = newHeads
    fireHeadsUpdated(this, oldHeads, newHeads)
  }

  private[harmonic] def fireHeadsUpdated(remote: RemoteEngine, oldHeads: Map[String, Long], newHeads: Map[String, Long]): Unit =
    fire { _.headsUpdated(remote, oldHeads, newHeads) }

  /**
   * TODO not implemented
   */
  @throws[IOException]
  def push(heads: Map[String, Long]): Unit = ???

  /**
   * Retrieves a map of head IDs from the remote engine. The remote may choose to only send some of its heads, or
   * even none.
   */
  @throws[IOException]
  def fetch(): Map[String, Long]

  /**
   * Retrieves a list of ancestor IDs for the given state. The first element of the sequence is the parent, and so
   * on. There is no guarantee about the number of ancestors being returned, except that the sequence must not be
   * empty.
   *
   * If the state ID is not present on the remote engine, or if the remote refuses to send any state IDs, a
   * `RemoteEngineException` is thrown.
   */
  @throws[IOException]
  @throws[RemoteEngineException]
  def retrieveAncestors(state: Long): List[Long]

  /**
   * Retrieves a sequence of states corresponding to the given sequence of state IDs. Besides being in the same
   * order, each `StateNode` also contains its own state ID.
   *
   * The given IDs are usually a subsequence of those being retrieved using `ancestors`. When other IDs are given,
   * it may be the case that they are missing from the remote engine, or that the remote engine would refuse to
   * send them. In such a case, a `RemoteEngineException` may be thrown.
   */
  @throws[IOException]
  @throws[RemoteEngineException]
  def retrieveStates(states: Seq[Long]): List[StateNode]

  def download(branch: String)(implicit engine: Engine): Unit = download(heads(branch))

  def download(state: Long)(implicit engine: Engine): State =
    engine.states.get(state) match {
      case Some(state) =>
        state
      case None =>
        //the requested state is the last to be retrieved
        var ancestors = state :: Nil
        breakable {
          //repeat the inner loop as necessary, as the remote engine is not required to return the full history at
          //once. The inner loop is implemented to require O(d), where d is the distance between the queried state
          //and the first known ancestor, instead of O(n), where n is the length of the history. In other words:
          //don't do more than required
          while (true) {
            //retrieve more ancestors
            for (state <- retrieveAncestors(ancestors.head)) {
              //break when a known state is encountered. this happens at the latest when reaching the root (0l)
              if (engine.states.contains(state)) break()
              //otherwise, prepend the unknown state
              ancestors = state :: ancestors
            }
          }
        }
        //ancestors now contains all unknown ancestors, beginning with a state whose parent is known. deserializing
        //in this order is no problem
        var result: State = null
        for (node <- retrieveStates(ancestors))
          result = engine.States += node
        assert(result != null)
        result
    }
}
