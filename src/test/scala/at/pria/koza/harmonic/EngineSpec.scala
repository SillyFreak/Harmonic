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
import org.scalatest.Matchers
import org.scalatest.GivenWhenThen

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

  it should "execute actions properly" in {
    Given("a new engine and its head")
    implicit val engine = new Engine()
    engine.addIO(MyAction)
    val oldHead = engine.head()

    When("executing an action")
    engine.execute(new MyAction())

    Then("the new head's parent sould be the old head")
    engine.head().parent should be(oldHead)
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

    protected[this] def apply0(): Unit = {}
  }

}
