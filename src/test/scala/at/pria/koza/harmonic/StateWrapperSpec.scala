/**
 * StateWrapperSpec.scala
 *
 * Created on 20.01.2015
 */

package at.pria.koza.harmonic

import scala.collection.mutable

import at.pria.koza.polybuf.PolybufOutput
import at.pria.koza.polybuf.PolybufInput

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

    engine.wrappers.head.state should be(engine.head())
  }

  it should "be resolvable if it's wrapping a known derived state" in {
    Given("a new engine")
    implicit val engine = new Engine()

    And("a PolybufIO for a custom action")
    engine.addIO(MyAction)

    When("executing an action")
    val action = engine.execute(new MyAction())

    Then("the new head's wrapper should be resolvable")
    engine.wrappers.head.state should be(engine.head())
  }

  it should "be resolvable in another engine if the parent is known" in {
    val (obj, id) = {
      Given("one engine with IOs for StateWrapper and MyAction")
      implicit val engine = new Engine()
      engine.addIO(StateWrapper)
      engine.addIO(MyAction)

      And("executing an action")
      engine.execute(new MyAction())
      val wrapper = engine.wrappers.head

      And("serializing the head state")
      val out = new PolybufOutput(engine.config)
      (out.writeObject(wrapper), wrapper.stateId)
    }

    {
      When("creating a new engine with IOs for StateWrapper and MyAction")
      implicit val engine = new Engine()
      engine.addIO(StateWrapper)
      engine.addIO(MyAction)

      And("deserializing the head in that engine")
      val in = new PolybufInput(engine.config)
      in.readObject(obj)

      Then("the wrapper should be available")
      val wrapper = engine.wrappers.get(id)
      wrapper should not be None

      And("it should be resolvable")
      wrapper.get.state should be(engine.states(id))
    }
  }
}
