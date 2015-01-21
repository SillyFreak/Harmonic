/**
 * Action.scala
 *
 * Created on 16.05.2013
 */

package at.pria.koza.harmonic

import scala.util.DynamicVariable

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
 * @version V1.0 26.07.2013
 * @author SillyFreak
 */
object Action {
  private val action = new DynamicVariable[Action](null)

  def value = action.value
}

/**
 * <p>
 * Creates a new action that modifies the given engine.
 * </p>
 *
 * @param engine the engine that is modified by this action
 */
abstract class Action()(implicit engine: Engine) {
  private var _modifications = List[Modification]()

  /**
   * <p>
   * Applies the action to the engine. This method should only be called by `Engine.setHead(State)`. This  method
   * invokes `apply0()`, surrounded by calls to `push(Action)`, and `pop(Action)` in a `finally` block to always
   * leave the action stack in a well defined way. Note that this does not mean that the engine, too, will be in a
   * defined state; see `apply0()`.
   * </p>
   */
  private[harmonic] def apply(): Unit = {
    Action.action.withValue(this) {
      apply0()
    }
  }

  /**
   * <p>
   * Reverts the action by reverting every `Modification` in reverse order. This method must only be called by the
   * `Engine`. When this method is called, the engine will be in the same state as it was after applying this
   * action.
   * </p>
   */
  private[harmonic] def revert(): Unit = {
    while (!_modifications.isEmpty) {
      val (m :: ms) = _modifications
      m.revert()
      _modifications = ms
    }
  }

  /**
   * <p>
   * Called by `Modification.addToAction()`. Adds a `Modification` to this action so that it can be subsequently
   * reverted.
   * </p>
   *
   * @param m the `Modification` to add
   */
  private[harmonic] def addModification(m: Modification): Unit = {
    _modifications = m :: _modifications
  }

  /**
   * <p>
   * Performs the actual action. This method must only be called from `apply()`, where it will have an `Action`
   * context established for `Modification`s via the `get()` method.
   * </p>
   * <p>
   * Note that this method is responsible for leaving the `Engine` in a well-defined state. If it does not return
   * normally, and the application needs to be able to roll back the engine, then this method must leave the engine
   * in a state where reverting this action really does revert it into the previous state.
   * </p>
   */
  protected[this] def apply0(): Unit
}
