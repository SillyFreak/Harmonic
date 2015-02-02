/**
 * RemoteEngineException.scala
 *
 * Created on 27.01.2015
 */

package at.pria.koza.polybuf

import java.io.IOException

/**
 * <p>
 * `RemoteEngineException`
 * </p>
 *
 * @version V1.0 27.01.2015
 * @author SillyFreak
 */
class RemoteEngineException(msg: String, cause: Throwable) extends IOException(msg, cause) {
  def this() = this(null, null)
  def this(msg: String) = this(msg, null)
  def this(cause: Throwable) = this(if (cause == null) null else cause.toString(), cause)
}
