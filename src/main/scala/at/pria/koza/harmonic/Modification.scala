/**
 * Modification.scala
 *
 * Created on 16.05.2013
 */

package at.pria.koza.harmonic

object Modification {
  def apply(mod: => Unit): Modification = new Modification {
    override def revert(): Unit = mod
  }

  def revertBy(mod: Modification): Unit = Action.current += mod

  def modification[U](mod: => U): ModificationWord[U] = ModificationWord(() => mod)
  case class ModificationWord[U](apply: () => U) {
    def isRevertedBy(revert: => Unit): U = {
      val result = apply()
      revertBy(Modification(revert))
      result
    }
  }
}

/**
 * <p>
 * The class `Modification` represents a low-level change that does a simple modification to the `Engine`. "Simple"
 * means that the change is easily reverted. The corresponding `Action` manages a list of `Modifications` that it
 * triggered, so that the `Action`, too, can be reverted.
 * </p>
 * <p>
 * The modification does only track how to revert a change, not how that change was originally performed. The code
 * that adds the modification should simply perform that change and then add the modification to the action via
 * `revertBy`. Note that a modification's failure could leave an engine inconsistent, which is a problem if the
 * program tries to recover. There are a few ways to handle failed modifications:
 * </p>
 * <ul>
 * <li>If possible, make sure the modification will succeed before doing any changes, and treat the actual
 * modification as infallible.</li>
 * <li>On error, do not register a modification, but revert the change in the exception handler, and rethrow the
 * exception.</li>
 * <li>Register a modification that reverts the change independent of success in a finally clause to the change, or
 * before the change.</li>
 * <li>On error, register a modification that only reverts the successful changes in the exception handler, and
 * rethrow the exception.</li>
 * <li>...</li>
 * </ul>
 * <p>
 * The first two variants have all-or-nothing semantics and are therefore the easiest to deal with. The following
 * DSL-style syntax can be used for such modifications:
 * {{{
 * import at.pria.koza.harmonic.Modification._
 *
 * modification {
 *   doSomething()
 * } isRevertedBy {
 *   revertSomething()
 * }
 * }}}
 * This code simply executes the first code block and, on success, registers the second one to revert it.
 * </p>
 *
 * @version V1.0 26.07.2013
 * @author SillyFreak
 */
trait Modification {
  /**
   * <p>
   * Reverts this Modification. This method is only called by `Action.revert()`, and only if a previous execution
   * of `apply()` called `addToAction()`. When this method is called, the engine will be in the same state as it
   * was after applying this modification.
   * </p>
   */
  private[harmonic] def revert(): Unit
}
