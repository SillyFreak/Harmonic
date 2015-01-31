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
    commonTail(Nil, Nil) should be(Nil)
  }

  it should "return Nil when either list is Nil" in {
    val list = List(1, 2, 3, 4)

    commonTail(Nil, list) should be(Nil)
    commonTail(list, Nil) should be(Nil)
  }

  it should "return the original list for two equal lists" in {
    val list = List(1, 2, 3, 4)

    commonTail(list, list) should be(list)
  }

  it should "return the common tail for two different lists of equal length" in {
    val list1 = List(1, 2, 3, 4, 5, 6)
    val list2 = List(7, 8, 9, 4, 5, 6)

    commonTail(list1, list2) should be(list1.drop(3))
  }

  it should "return the common tail for two different lists of different lengths" in {
    val list1 = List(0, 1, 2, 3, 4, 5, 6)
    val list2 = List(9, 4, 5, 6)

    commonTail(list1, list2) should be(list1.drop(4))
  }

  it should "return the original sequence for two equal nonlist sequences" in {
    val list = "1234": Seq[Char]

    commonTail(list, list) should be(list)
  }

  it should "return the common tail for two different nonlist sequences of equal length" in {
    val list1 = "123456": Seq[Char]
    val list2 = "789456": Seq[Char]

    commonTail(list1, list2) should be(list1.drop(3))
  }

  it should "return the common tail for two different nonlist sequences of different lengths" in {
    val list1 = "0123456": Seq[Char]
    val list2 = "9456": Seq[Char]

    commonTail(list1, list2) should be(list1.drop(4))
  }

  it should "return the original sequence for two equal infinite sequences" in {
    val list = Stream.from(0)

    //note: result check for infinite list does only work because the original
    //stream instance is returned, and not a different equal sequence
    commonTail(list, list) should be(list)
  }

  it should "return the common tail for two different infinite sequences where one is a subsequence of the other" in {
    val list1 = Stream.from(2)
    val list2 = Stream.from(0)

    //note: result check for infinite list does only work because the original
    //stream instance is returned, and not a different equal sequence
    commonTail(list1, list2) should be(list1)
  }

  it should "return the common tail for two different infinite sequences with diverging heads of same length" in {
    val tail = Stream.from(0)
    val list1 = -1 #:: tail
    val list2 = -2 #:: tail

    //note: result check for infinite list does only work because the original
    //stream instance is returned, and not a different equal sequence
    commonTail(list1, list2) should be(tail)
  }

  it should "return the common tail for two different infinite sequences with diverging heads of different lengths" in {
    val tail = Stream.from(0)
    val list1 = -5 #:: -3 #:: -1 #:: tail
    val list2 = -2 #:: tail

    //note: result check for infinite list does only work because the original
    //stream instance is returned, and not a different equal sequence
    commonTail(list1, list2) should be(tail)
  }
}
