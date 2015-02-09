/**
 * Action.scala
 *
 * Created on 16.05.2013
 */

package at.pria.koza.harmonic

import scala.util.DynamicVariable

object Action {
  private val action = new DynamicVariable[Action](null)

  def value = action.value
}

/**
 * <p>
 * The class `Action` represents a high-level change that can happen in the `Engine`. An `Action` causes one or
 * more `Modifications` to happen. These are stored in a list, so they can be reverted if necessary.
 * </p>
 * <p>
 * As an `Action` represents an executable, replayable, and even revertable piece of code, calling it is of course
 * delicate; the goal is to get the same result every time it is applied. To achieve this, it is necessary that the
 * original state of the engine is compatible with the action. For a new action, this is the responsibility of the
 * user. For replaying actions on the same or different engines, Harmonic ensures this by associating the action
 * with the same state as the user originally did; together with Harmonic's requirement of determinism of the
 * engine, this should ensure the expected behavior.
 * </p>
 *
 * @version V1.0 09.01.2015
 * @author SillyFreak
 */
trait Action {
  private[this] var _modifications = List.empty[Modification]

  /**
   * Adds a `Modification` for reverting a single change caused by this action. This method must only be called
   * by `Modification.revertBy`.
   */
  private[harmonic] def +=(m: Modification): Unit =
    _modifications = m :: _modifications

  /**
   * Establishes the action in `Action.value` and calls `apply()`. This method must only be called by the engine.
   */
  private[harmonic] def execute(implicit engine: Engine): Unit =
    Action.action.withValue(this) { apply }

  /**
   * Reverts the action by calling `revert()` on all registered modifications in reverse order. This method must
   * only be called by the engine.
   */
  private[harmonic] def revert(): Unit =
    _modifications = _modifications.dropWhile { m => m.revert(); true }

  /**
   * <p>
   * Performs the actual action. This method must only be called from `execute()`, which makes the action available
   * via `Action.value`, which is necessary for `Modification`s to work.
   * </p>
   * <p>
   * Note that this method is responsible for leaving the `Engine` in a well-defined state. If it does not return
   * normally, and the application needs to be able to roll back the engine, then this method must leave the engine
   * in a state where reverting this action really does revert it into the previous state.
   * </p>
   */
  protected[this] def apply(implicit engine: Engine): Unit
}
