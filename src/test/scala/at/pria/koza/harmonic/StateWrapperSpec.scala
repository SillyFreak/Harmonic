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

  it should "be resolvable if it's wrapping a known derived state" in {
    Given("a new engine")
    implicit val engine = new Engine()

    And("a PolybufIO for a custom action")
    engine.addIO(MyAction)

    When("executing an action")
    val action = engine.execute(new MyAction())

    Then("the new head's wrapper should be resolvable")
    engine.wrappers(engine.head().id).state should be(engine.head())
  }
}
