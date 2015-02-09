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

  def modification(mod: => Unit): Unit = modification(Modification(mod))
  def modification(mod: Modification): Unit = Action.value.addModification(mod)
}

/**
 * <p>
 * The class `Modification` represents a low-level change that does a simple modification to the `Engine`. "Simple"
 * means that the change is easily reverted. The corresponding `Action` manages a list of `Modifications` that it
 * triggered, so that the `Action`, too, can be reverted.
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
  def revert(): Unit
}
