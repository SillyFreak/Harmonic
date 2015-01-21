/**
 * Entity.scala
 *
 * Created on 14.05.2013
 */

package at.pria.koza.harmonic

import java.io.Serializable

/**
 * <p>
 * `Entity` represents a single object that is part of an `Engine`'s state. An entity can only belong to one engine
 * at a time. In this engine, it is identified by an id assigned by the engine. The id assignment scheme is
 * deterministic so that systems of distributed engines can replay `Action`s that create new entities.
 * </p>
 *
 * @version V1.0 27.07.2013
 * @author SillyFreak
 */
trait Entity extends Serializable {
  private var _engine: Engine = _
  private var _id: Int = _

  def init()(implicit engine: Engine): Unit =
    if (_engine == null) engine.entities += this

  /**
   * <p>
   * Sets the engine this entity belongs to, along with its assigned ID
   * </p>
   *
   * @param engine the engine this entity belongs to
   * @param id the ID assigned to this entity within its engine
   */
  private[harmonic] def engine(engine: Engine, id: Int): Unit = {
    _engine = engine
    _id = id
  }

  /**
   * <p>
   * Returns the engine to which this entity belongs.
   * </p>
   *
   * @return the engine to which this entity belongs
   */
  def engine: Engine = _engine

  /**
   * <p>
   * Returns the id assigned to this entity by the engine.
   * </p>
   *
   * @return this entity's id
   */
  def id: Int = _id

  override def toString(): String = "%s@%08X".format(getClass().getSimpleName(), id)
}
