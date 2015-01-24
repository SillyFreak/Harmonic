/**
 * StateWrapperSpec.scala
 *
 * Created on 20.01.2015
 */

package at.pria.koza.harmonic

import scala.collection.mutable

import org.scalatest.FlatSpec
import org.scalatest.GivenWhenThen
import org.scalatest.Matchers

/**
 * <p>
 * {@code StateWrapperSpec}
 * </p>
 *
 * @version V0.0 20.01.2015
 * @author SillyFreak
 */
class StateWrapperSpec extends FlatSpec with Matchers with GivenWhenThen {
  behavior of "A StateWrapper"

  it should "be resolvable if it's wrapping the root state" in {
    implicit val engine = new Engine()
    engine.addIO(StateWrapper)

    engine.wrappers(0l).state should be(engine.head())
  }
}
