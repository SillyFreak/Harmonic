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

  it should "return Nil when both lists are Nil" in {
    State.commonTail(Nil, Nil) should be(Nil)
  }

  it should "return Nil when either list is Nil" in {
    val list = List(1, 2, 3, 4)

    State.commonTail(Nil, list) should be(Nil)
    State.commonTail(list, Nil) should be(Nil)
  }

  it should "return the original list for two equal lists" in {
    val list = List(1, 2, 3, 4)

    State.commonTail(list, list) should be(list)
  }

  it should "return the common tail for two different lists of equal length" in {
    val list1 = List(1, 2, 3, 4, 5, 6)
    val list2 = List(7, 8, 9, 4, 5, 6)

    State.commonTail(list1, list2) should be(list1.drop(3))
  }

  it should "return the common tail for two different lists of different lengths" in {
    val list1 = List(0, 1, 2, 3, 4, 5, 6)
    val list2 = List(9, 4, 5, 6)

    State.commonTail(list1, list2) should be(list1.drop(4))
  }
}
