/**
 * LocalEngineSpec.scala
 *
 * Created on 30.01.2015
 */
package at.pria.koza.harmonic

import at.pria.koza.harmonic.local.LocalEngine

import org.scalatest.FlatSpec
import org.scalatest.GivenWhenThen
import org.scalatest.Matchers

/**
 * <p>
 * {@code LocalEngineSpec}
 * </p>
 *
 * @version V0.0 30.01.2015
 * @author SillyFreak
 */
class LocalEngineSpec extends FlatSpec with Matchers with GivenWhenThen {
  behavior of "LocalEngine"

  it should "fetch heads properly" in {
    Given("two engines")
    val engine1 = new Engine()
    val engine2 = new Engine()

    And("a PolybufIO for a custom action")
    engine1.addIO(MyAction)
    engine2.addIO(MyAction)

    And("a LocalEngine connecting to the second engine")
    val remote = new LocalEngine(engine2)

    Then("the remote should contain no branches")
    remote.heads should be(Map())

    When("creating a branch in the second engine")
    val master = engine2.Branches.createBranchHere("master")

    Then("the remote should still contain no branches")
    remote.heads should be(Map())

    When("fetching from the remote")
    remote.fetch()

    Then("the remote should contain the remote branch")
    remote.heads should be(Map("master" -> 0l))
  }

  it should "download states properly" in {
    Given("two engines")
    val engine1 = new Engine()
    val engine2 = new Engine()

    And("a PolybufIO for a custom action")
    engine1.addIO(MyAction)
    engine2.addIO(MyAction)

    And("a LocalEngine connecting to the second engine")
    val remote = new LocalEngine(engine2)

    {
      When("executing two actions in the second engine")
      implicit val engine = engine2
      engine2.execute(new MyAction())
      engine2.execute(new MyAction())
    }

    And("creating a branch in the second engine")
    val master = engine2.Branches.createBranchHere("master")

    And("fetching from the remote")
    remote.fetch()

    Then("the remote should contain the remote branch")
    remote.heads should be(Map("master" -> master.tip.id))

    {
      When("downloading the master branch")
      implicit val engine = engine1
      remote.download("master")
    }

    Then("the first engine should contain the received states")
    engine1.states.get(master.tip.id) should not be None

    When("creating a branch in the first engine")
    val master2 = engine1.Branches.createBranchHere("master")

    And("setting it to track the remote branch")
    master2.tracking = (remote, "master")

    Then("the branch should be at the engine's head")
    master2.tip should be(engine1.head)

    When("updating that branch")
    master2.update()

    Then("the branch should be at the remote branch's tip")
    master2.tip.id should be(master.tip.id)
  }
}
