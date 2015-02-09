/**
 * Entity.scala
 *
 * Created on 14.05.2013
 */

package at.pria.koza.harmonic

import java.io.Serializable

/**
 * <p>
 * `Entity` represents a single object that is part of an `Engine`'s state. An entity belongs to one engine. In
 * this engine, it is identified by an id assigned by the engine. The id assignment scheme is deterministic so that
 * systems of distributed engines can replay `Action`s that create new entities.
 * </p>
 *
 * @version V1.0 27.07.2013
 * @author SillyFreak
 */
trait Entity {
  val engine: Engine
  private[this] var _id: Option[Int] = None
  def id: Int = _id match {
    case Some(id) => id
    case None     => throw new AssertionError()
  }
  private[harmonic] def id_=(id: Int): Unit = _id match {
    case Some(_) => throw new AssertionError()
    case None    => _id = Some(id)
  }

  engine.Entities += this

  override def toString(): String = "%s@%08X".format(getClass().getSimpleName(), id)
}
