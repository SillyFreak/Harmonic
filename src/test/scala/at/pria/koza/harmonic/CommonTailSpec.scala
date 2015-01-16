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

  it should "return Nil when either list is Nil" in {
    State.commonTail(Nil, Stream.from(0)) should be(Nil)
    State.commonTail(Stream.from(0), Nil) should be(Nil)
    State.commonTail(Nil, Nil) should be(Nil)
  }

  it should "return the original list for two equal lists" in {
    State.commonTail("abcd", "abcd") should be("abcd": Seq[Char])
  }
}
