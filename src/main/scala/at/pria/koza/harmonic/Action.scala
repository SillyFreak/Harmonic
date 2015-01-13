/**
 * Action.scala
 *
 * Created on 16.05.2013
 */

package at.pria.koza.harmonic

import java.util.Deque
import java.util.LinkedList
import java.util.List
import java.util.ListIterator

/**
 * <p>
 * The class {@code Action} represents a high-level change that can happen in the {@link Engine}. An {@code Action}
 * causes one or more {@link Modification Modifications} to happen. These are stored in a list, so they can be
 * {@linkplain #revert() reverted} if necessary.
 * </p>
 * <p>
 * As an {@code Action} represents an executable, replayable, and even revertable piece of code, calling it is of
 * course delicate; the goal is to get the same result every time it is applied. To achieve this, it is necessary
 * that the original state of the engine is compatible with the action. For a new action, this is the
 * responsibility of the user. For replaying actions on the same or different engines, Harmonic ensures this by
 * associating the action with the same state as the user originally did; together with Harmonic's requirement of
 * determinism of the engine, this should ensure the expected behavior.
 * </p>
 *
 * @version V1.0 26.07.2013
 * @author SillyFreak
 */
object Action {
  private val _actions = new ThreadLocal[Deque[Action]]() {
    override def initialValue(): Deque[Action] =
      new LinkedList[Action]()
  }

  /**
   * <p>
   * Returns the currently active action. That action is the one that modifications will be added to.
   * </p>
   *
   * @return the currently active action
   */
  def get(): Action = {
    val a = _actions.get().peekFirst()
    if (a == null) throw new IllegalStateException("No action active")
    a
  }

  private def push(a: Action): Unit = {
    _actions.get().addFirst(a)
  }

  private def pop(a: Action): Unit = {
    def a0 = _actions.get().pollFirst()
    if (a == null) throw new IllegalStateException("No action active")
    assert(a == a0)
  }
}

/**
 * <p>
 * Creates a new action that modifies the given engine.
 * </p>
 *
 * @param engine the engine that is modified by this action
 */
abstract class Action(engine: Engine) {
  private val _engine = engine
  private val _modifications = new LinkedList[Modification]()

  /**
   * <p>
   * Returns the engine this action is associated with.
   * </p>
   *
   * @return the engine this action is associated with
   */
  def getEngine(): Engine = _engine

  /**
   * <p>
   * Applies the action to the engine. This method should only be called by {@link Engine#setHead(State)}. This
   * method invokes {@link #apply0()}, surrounded by calls to {@link #push(Action)}, and {@link #pop(Action)} in
   * a {@code finally} block to always leave the action stack in a well defined way. Note that this does not mean
   * that the engine, too, will be in a defined state; see {@link #apply0()}.
   * </p>
   */
  def apply(): Unit = {
    try {
      Action.push(this)
      apply0()
    } finally {
      Action.pop(this)
    }
  }

  /**
   * <p>
   * Reverts the action by {@linkplain Modification#revert() reverting} every {@link Modification} in reverse
   * order. This method must only be called by the {@link Engine}. When this method is called, the engine will be
   * in the same state as it was after {@linkplain #apply() applying} this action.
   * </p>
   */
  def revert(): Unit = {
    val it = _modifications.listIterator(_modifications.size())
    while (it.hasPrevious()) {
      it.previous().revert()
      it.remove()
    }
  }

  /**
   * <p>
   * Called by {@link Modification#addToAction()}. Adds a {@link Modification} to this action so that it can be
   * subsequently reverted.
   * </p>
   *
   * @param m the {@link Modification} to add
   */
  def addModification(m: Modification): Unit = {
    _modifications.add(m)
  }

  /**
   * <p>
   * Performs the actual action. This method must only be called from {@link #apply()}, where it will have an
   * {@code Action} context established for {@link Modification}s via the {@link #get()} method.
   * </p>
   * <p>
   * Note that this method is responsible for leaving the {@link Engine} in a well-defined state. If it does not
   * return normally, and the application needs to be able to roll back the engine, then this method must leave
   * the engine in a state where {@linkplain #revert() reverting} this action really does revert it into the
   * previous state.
   * </p>
   */
  def apply0(): Unit
}
