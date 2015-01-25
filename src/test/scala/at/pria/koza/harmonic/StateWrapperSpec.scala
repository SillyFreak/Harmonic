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

    engine.headWrapper.state should be(engine.head)
  }

  it should "be resolvable if it's wrapping a known derived state" in {
    Given("a new engine")
    implicit val engine = new Engine()

    And("a PolybufIO for a custom action")
    engine.addIO(MyAction)

    When("executing an action")
    val action = engine.execute(new MyAction())

    Then("the new head's wrapper should be resolvable")
    engine.headWrapper.state should be(engine.head)
  }

  it should "be resolvable in another engine if the parent is known" in {
    val (obj, id) = {
      Given("one engine with IOs for StateWrapper and MyAction")
      implicit val engine = new Engine()
      engine.addIO(StateWrapper)
      engine.addIO(MyAction)

      And("executing an action")
      engine.execute(new MyAction())
      val wrapper = engine.headWrapper

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
      val wrapper = engine.Wrappers.get(id)
      wrapper should not be None

      And("it should be resolvable")
      wrapper.get.state should be(engine.states(id))
    }
  }

  it should "not be resolvable in another engine if the parent is not known" in {
    val (obj1, id1, obj2, id2) = {
      Given("one engine with IOs for StateWrapper and MyAction")
      implicit val engine = new Engine()
      engine.addIO(StateWrapper)
      engine.addIO(MyAction)

      And("executing two actions")
      engine.execute(new MyAction())
      val wrapper1 = engine.headWrapper
      engine.execute(new MyAction())
      val wrapper2 = engine.headWrapper

      And("serializing both states")
      val out = new PolybufOutput(engine.config)
      (out.writeObject(wrapper1), wrapper1.stateId,
        out.writeObject(wrapper2), wrapper2.stateId)
    }

    {
      When("creating a new engine with IOs for StateWrapper and MyAction")
      implicit val engine = new Engine()
      engine.addIO(StateWrapper)
      engine.addIO(MyAction)

      And("deserializing the second state in that engine")
      val in = new PolybufInput(engine.config)
      in.readObject(obj2)

      Then("the wrapper should be available")
      val wrapper2 = engine.Wrappers.get(id2)
      wrapper2 should not be None

      And("it should *not* be resolvable")
      intercept[IllegalStateException] {
        wrapper2.get.state
      }

      When("deserializing the first state in that engine")
      in.readObject(obj1)

      Then("the second state should be resolvable")
      wrapper2.get.state
    }
  }
}
