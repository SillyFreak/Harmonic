/**
 * harmonic_test.scala
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

import com.google.protobuf.GeneratedMessage.GeneratedExtension

object MyAction extends IOFactory[MyAction] {
  val FIELD = MyActionP.NEW_GAME_ACTION_FIELD_NUMBER
  val EXTENSION = MyActionP.newGameAction

  def getIO(implicit engine: Engine): PolybufIO[MyAction] = new IO()

  private class IO(implicit engine: Engine) extends PolybufIO[MyAction] {
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

class MyAction extends Action with PolybufSerializable {
  //PolybufSerializable
  def typeId: Int = MyAction.FIELD

  var entityId: Int = -1

  protected[this] def apply(): Unit = {
    entityId = new MyEntity().id
  }
}

class MyEntity(implicit val engine: Engine) extends Entity
