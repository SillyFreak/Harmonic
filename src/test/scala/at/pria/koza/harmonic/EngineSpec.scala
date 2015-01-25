/**
 * EngineSpec.scala
 *
 * Created on 23.01.2015
 */

package at.pria.koza.harmonic

import org.scalatest.FlatSpec
import org.scalatest.GivenWhenThen
import org.scalatest.Matchers

/**
 * <p>
 * {@code EngineSpec}
 * </p>
 *
 * @version V0.0 23.01.2015
 * @author SillyFreak
 */
class EngineSpec extends FlatSpec with Matchers with GivenWhenThen {
  behavior of "An engine"

  it should "initially have the root state as its head" in {
    implicit val engine = new Engine()
    engine.head should be(engine.states(0l))
  }

  it should "execute and revert actions properly" in {
    Given("a new engine and its head")
    implicit val engine = new Engine()
    val oldHead = engine.head

    And("a PolybufIO for a custom action")
    engine.addIO(MyAction)

    When("executing an action that adds an entity")
    val action = engine.execute(new MyAction())

    Then("the new head's parent should be the old head")
    engine.head.parent should be(oldHead)

    And("the engine should contain the created entity")
    engine.Entities.get(action.entityId) should not be None

    When("reverting to the original head")
    engine.head = oldHead

    Then("the head should be the original head")
    engine.head should be(oldHead)

    And("the engine should not contain the entity any more")
    engine.Entities.get(action.entityId) should be(None)
  }
}
