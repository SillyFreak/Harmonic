/**
 * ListenerManager.scala
 *
 * Created on 08.02.2015
 */
package at.pria.koza.harmonic.util

/**
 * <p>
 * {@code ListenerManager}
 * </p>
 *
 * @version V0.0 08.02.2015
 * @author SillyFreak
 */
trait ListenerManager[L] {
  private val lock = new Object()

  private var _listeners: List[L] = Nil
  def listeners = _listeners

  protected[this] def fire[U](action: L => U): Unit = _listeners.synchronized {
    _listeners.foreach(action)
  }

  def addListener(l: L): Unit = _listeners.synchronized {
    _listeners = l :: _listeners
  }

  def removeListener(l: L): Unit = _listeners.synchronized {
    _listeners = _listeners filter { _ != l }
  }
}
