/**
 * IOFactory.scala
 *
 * Created on 17.01.2015
 */

package at.pria.koza.harmonic

import at.pria.koza.polybuf.PolybufIO
import at.pria.koza.polybuf.PolybufSerializable

/**
 * <p>
 * {@code IOFactory}
 * </p>
 *
 * @version V0.0 17.01.2015
 * @author SillyFreak
 */
trait IOFactory[T <: PolybufSerializable] {
  def getIO(engine: Engine): PolybufIO[T]
}
