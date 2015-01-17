/*
 * Happy Melly Teller
 * Copyright (C) 2013 - 2015, Happy Melly http://www.happymelly.com
 *
 * This file is part of the Happy Melly Teller.
 *
 * Happy Melly Teller is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Happy Melly Teller is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Happy Melly Teller.  If not, see <http://www.gnu.org/licenses/>.
 *
 * If you have questions concerning this license or the applicable additional
 * terms, you may contact by email Sergey Kotlov, sergey.kotlov@happymelly.com
 * or in writing
 * Happy Melly One, Handelsplein 37, Rotterdam, The Netherlands, 3071 PR
 */
package models.event

import com.github.tototoshi.slick.JodaSupport._
import models.{ Brand, Event }
import models.database.{ EventFacilitators, Events }
import org.joda.time.LocalDate
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB
import play.api.Play.current
import scala.language.postfixOps
import scala.slick.lifted.Query

class EventService {

  /**
   * Returns true if a person is a brand manager of this event
   *
   * @param personId A person unique identifier
   */
  def isBrandManager(personId: Long, event: Event): Boolean = DB.withSession {
    implicit session: Session ⇒
      Brand.find(event.brandCode).exists(_.coordinator.id.get == personId)
  }

  /**
   * Returns event if it exists, otherwise - None
   *
   * @param id Event identifier
   */
  def find(id: Long): Option[Event] = DB.withSession {
    implicit session: Session ⇒
      Query(Events).filter(_.id === id).list.headOption
  }

  /**
   * Returns a list of events based on several parameters
   *
   * @param brandCode Only events of this brand
   * @param future Only future and current events
   * @param public Only public events
   * @param archived Only archived events
   * @param confirmed Only confirmed events
   * @param country Only events in this country
   * @param eventType Only events of this type
   */
  def findByParameters(
    brandCode: Option[String],
    future: Option[Boolean] = None,
    public: Option[Boolean] = None,
    archived: Option[Boolean] = None,
    confirmed: Option[Boolean] = None,
    country: Option[String] = None,
    eventType: Option[Long] = None): List[Event] = DB.withSession {
    implicit session: Session ⇒
      val baseQuery = Query(Events)

      val brandQuery = brandCode map {
        v ⇒ baseQuery filter (_.brandCode === v)
      } getOrElse baseQuery

      val timeQuery = applyTimeFilter(future, brandQuery)
      val publicityQuery = applyPublicityFilter(public, timeQuery)
      val archivedQuery = applyArchivedFilter(archived, publicityQuery)

      val confirmedQuery = confirmed map { value ⇒
        archivedQuery filter (_.confirmed === value)
      } getOrElse archivedQuery

      val countryQuery = country map { value ⇒
        confirmedQuery filter (_.countryCode === value)
      } getOrElse confirmedQuery

      val typeQuery = eventType map { value ⇒
        countryQuery filter (_.eventTypeId === value)
      } getOrElse countryQuery

      typeQuery sortBy (_.start) list
  }

  /**
   * Return a list of events for a given facilitator
   *
   * @param facilitatorId Only events facilitated by this facilitator
   * @param brand Only events of this brand
   * @param future Only future and current events
   * @param public Only public events
   * @param archived Only archived events
   */
  def findByFacilitator(
    facilitatorId: Long,
    brand: Option[String],
    future: Option[Boolean] = None,
    public: Option[Boolean] = None,
    archived: Option[Boolean] = None): List[Event] = DB.withSession {
    implicit session: Session ⇒

      val baseQuery = brand map { value ⇒
        for {
          entry ← EventFacilitators if entry.facilitatorId === facilitatorId
          event ← Events if event.id === entry.eventId && event.brandCode === value
        } yield event
      } getOrElse {
        for {
          entry ← EventFacilitators if entry.facilitatorId === facilitatorId
          event ← Events if event.id === entry.eventId
        } yield event
      }

      val timeQuery = applyTimeFilter(future, baseQuery)
      val publicityQuery = applyPublicityFilter(public, timeQuery)
      val archivedQuery = applyArchivedFilter(archived, publicityQuery)

      archivedQuery sortBy (_.start) list
  }

  /** Returns list with active events */
  def findActive: List[Event] = DB.withSession { implicit session: Session ⇒
    EventService.findByParameters(brandCode = None, archived = Some(false)).
      sortBy(_.title.toLowerCase)
  }

  /** Returns list with all events */
  def findAll: List[Event] = DB.withSession { implicit session: Session ⇒
    EventService.findByParameters(brandCode = None)
  }

  /**
   * Applies time filter on query
   *
   * @param future Defines if time filter should be applied
   * @param parentQuery Query to apply this filter to
   * @return returns updated query if 'future' flag is defined
   */
  private def applyTimeFilter(
    future: Option[Boolean],
    parentQuery: Query[Events.type, Event]) = {
    future map { value ⇒
      val now = LocalDate.now
      val today = new LocalDate(
        now.getValue(0),
        now.getValue(1),
        now.getValue(2))
      if (value)
        parentQuery.filter(_.end >= today)
      else
        parentQuery.filter(_.end <= today)
    } getOrElse parentQuery
  }

  /**
   * Applies publicity filter on query
   *
   * @param public Defines if publicity filter should be applied
   * @param parentQuery Query to apply this filter to
   * @return returns updated query if 'public' flag is defined
   */
  private def applyPublicityFilter(
    public: Option[Boolean],
    parentQuery: Query[Events.type, Event]) = {
    public map { value ⇒
      parentQuery.filter(_.notPublic === !value)
    } getOrElse parentQuery
  }

  /**
   * Applies archived filter on query
   *
   * @param archived Defines if archived filter should be applied
   * @param parentQuery Query to apply this filter to
   * @return returns updated query if 'archived' flag is defined
   */
  private def applyArchivedFilter(
    archived: Option[Boolean],
    parentQuery: Query[Events.type, Event]) = {
    archived map { value ⇒
      parentQuery.filter(_.archived === value)
    } getOrElse parentQuery
  }
}

object EventService {
  private val instance = new EventService()

  /**
   * Returns true if a person is a brand manager of this event
   *
   * @param personId A person unique identifier
   */
  def isBrandManager(personId: Long, event: Event) = instance.isBrandManager(
    personId, event)

  /**
   * Returns event if it exists, otherwise - None
   *
   * @param id Event identifier
   */
  def find(id: Long): Option[Event] = instance.find(id)

  /**
   * Returns a list of events based on several parameters
   *
   * @param brandCode Only events of this brand
   * @param future Only future and current events
   * @param public Only public events
   * @param archived Only archived events
   * @param confirmed Only confirmed events
   * @param country Only events in this country
   * @param eventType Only events of this type
   */
  def findByParameters(
    brandCode: Option[String],
    future: Option[Boolean] = None,
    public: Option[Boolean] = None,
    archived: Option[Boolean] = None,
    confirmed: Option[Boolean] = None,
    country: Option[String] = None,
    eventType: Option[Long] = None) = instance.findByParameters(
    brandCode,
    future,
    public,
    archived,
    confirmed,
    country,
    eventType)

  /**
   * Return a list of events for a given facilitator
   *
   * @param facilitatorId Only events facilitated by this facilitator
   * @param brand Only events of this brand
   * @param future Only future and current events
   * @param public Only public events
   * @param archived Only archived events
   */
  def findByFacilitator(
    facilitatorId: Long,
    brand: Option[String],
    future: Option[Boolean] = None,
    public: Option[Boolean] = None,
    archived: Option[Boolean] = None) = instance.findByFacilitator(
    facilitatorId,
    brand,
    future,
    public,
    archived)

  /** Returns list with active events */
  def findActive: List[Event] = instance.findActive

  /** Returns list with all events */
  def findAll: List[Event] = instance.findAll
}