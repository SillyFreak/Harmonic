/**
 * CommonTailSpec.scala
 *
 * Created on 16.01.2015
 */
package at.pria.koza.harmonic

import org.scalatest.FlatSpec
import org.scalatest.Matchers

/**
 * <p>
 * {@code CommonTailSpec}
 * </p>
 *
 * @version V0.0 16.01.2015
 * @author SillyFreak
 */
class CommonTailSpec extends FlatSpec with Matchers {
  behavior of "commonTail"

  it should "return Nil when first list is Nil" in {
    State.commonTail(Nil, Stream.from(0)) should be(Nil)
  }

  it should "return Nil when second list is Nil" in {
    State.commonTail(Stream.from(0), Nil) should be(Nil)
  }
}
