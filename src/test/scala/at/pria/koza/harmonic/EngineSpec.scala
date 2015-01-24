/**
 * EngineSpec.scala
 *
 * Created on 23.01.2015
 */

package at.pria.koza.harmonic

import at.pria.koza.harmonic.proto.HarmonicTestP.MyActionP
import at.pria.koza.polybuf.PolybufException
import at.pria.koza.polybuf.PolybufInput
import at.pria.koza.polybuf.PolybufIO
import at.pria.koza.polybuf.PolybufOutput
import at.pria.koza.polybuf.PolybufSerializable
import at.pria.koza.polybuf.proto.Polybuf.Obj

import org.scalatest.FlatSpec
import org.scalatest.GivenWhenThen
import org.scalatest.Matchers

import com.google.protobuf.GeneratedMessage.GeneratedExtension

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
    engine.head() should be(engine.states(0l))
  }

  it should "execute and revert actions properly" in {
    Given("a new engine and its head")
    implicit val engine = new Engine()
    val oldHead = engine.head()

    And("a PolybufIO for a custom action")
    engine.addIO(MyAction)

    When("executing an action that adds an entity")
    val action = engine.execute(new MyAction())

    Then("the new head's parent should be the old head")
    engine.head().parent should be(oldHead)

    And("the engine should contain the created entity")
    engine.entities.get(action.entityId) should not be None

    When("reverting to the original head")
    engine.head() = oldHead

    Then("the head should be the original head")
    engine.head() should be(oldHead)

    And("the engine should not contain the entity any more")
    engine.entities.get(action.entityId) should be(None)
  }

  object MyAction extends IOFactory[MyAction] {
    val FIELD = MyActionP.NEW_GAME_ACTION_FIELD_NUMBER
    val EXTENSION = MyActionP.newGameAction

    def getIO(implicit engine: Engine): PolybufIO[MyAction] = new IO()

    private class IO()(implicit engine: Engine) extends PolybufIO[MyAction] {
      override def extension: GeneratedExtension[Obj, MyActionP] = EXTENSION

      @throws[PolybufException]
      override def serialize(out: PolybufOutput, instance: MyAction, obj: Obj.Builder): Unit = {
        obj.setExtension(extension, MyActionP.newBuilder().build())
      }

      @throws[PolybufException]
      override def initialize(in: PolybufInput, obj: Obj): MyAction = {
        //val p = obj.getExtension(extension)
        return new MyAction()
      }
    }
  }

  class MyAction()(implicit engine: Engine) extends Action with PolybufSerializable {
    //PolybufSerializable
    def typeId: Int = MyAction.FIELD

    var entityId: Int = _

    protected[this] def apply0(): Unit = {
      entityId = new MyEntity().id
    }
  }

  class MyEntity()(implicit engine: Engine) extends Entity {
    init()
  }
}
