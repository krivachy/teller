/*
 * Happy Melly Teller
 * Copyright (C) 2013 - 2014, Happy Melly http://www.happymelly.com
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
 * If you have questions concerning this license or the applicable additional terms, you may contact
 * by email Sergey Kotlov, sergey.kotlov@happymelly.com or
 * in writing Happy Melly One, Handelsplein 37, Rotterdam, The Netherlands, 3071 PR
 */

package models.database

import com.github.tototoshi.slick.JodaSupport._
import models._
import org.joda.time.{ LocalDate, DateTime }
import play.api.db.slick.Config.driver.simple._

/**
 * `Event` database table mapping.
 */
private[models] object Events extends Table[Event]("EVENT") {

  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def eventTypeId = column[Long]("EVENT_TYPE_ID")
  def brandCode = column[String]("BRAND_CODE")
  def title = column[String]("TITLE")

  def spokenLanguage = column[String]("SPOKEN_LANGUAGE")
  def secondSpokenLanguage = column[Option[String]]("SECOND_SPOKEN_LANGUAGE")
  def materialsLanguage = column[Option[String]]("MATERIALS_LANGUAGE")
  def city = column[String]("CITY")
  def countryCode = column[String]("COUNTRY_CODE")

  def description = column[Option[String]]("DESCRIPTION", O.DBType("TEXT"))
  def specialAttention = column[Option[String]]("SPECIAL_ATTENTION", O.DBType("TEXT"))
  def webSite = column[Option[String]]("WEB_SITE")
  def registrationPage = column[Option[String]]("REGISTRATION_PAGE")

  def start = column[LocalDate]("START_DATE")
  def end = column[LocalDate]("END_DATE")
  def hoursPerDay = column[Int]("HOURS_PER_DAY")
  def totalHours = column[Int]("TOTAL_HOURS")

  def notPublic = column[Boolean]("NOT_PUBLIC")
  def archived = column[Boolean]("ARCHIVED")
  def confirmed = column[Boolean]("CONFIRMED")

  def created = column[DateTime]("CREATED")
  def createdBy = column[String]("CREATED_BY")
  def updated = column[DateTime]("UPDATED")
  def updatedBy = column[String]("UPDATED_BY")

  def brand = foreignKey("BRAND_FK", brandCode, Brands)(_.code)

  def * = id.? ~ eventTypeId ~ brandCode ~ title ~ spokenLanguage ~ secondSpokenLanguage ~ materialsLanguage ~ city ~
    countryCode ~ description ~ specialAttention ~ webSite ~ registrationPage ~ start ~ end ~ hoursPerDay ~ totalHours ~
    notPublic ~ archived ~ confirmed <> (
      e ⇒ Event(e._1, e._2, e._3, e._4, Language(e._5, e._6, e._7), Location(e._8, e._9), Details(e._10, e._11, e._12, e._13),
        Schedule(e._14, e._15, e._16, e._17), e._18, e._19, e._20, DateTime.now(), "", DateTime.now(), ""),
      (e: Event) ⇒ Some((e.id, e.eventTypeId, e.brandCode, e.title, e.language.spoken, e.language.secondSpoken,
        e.language.materials, e.location.city, e.location.countryCode, e.details.description, e.details.specialAttention,
        e.details.webSite, e.details.registrationPage, e.schedule.start, e.schedule.end, e.schedule.hoursPerDay,
        e.schedule.totalHours, e.notPublic, e.archived, e.confirmed)))

  def forInsert = eventTypeId ~ brandCode ~ title ~ spokenLanguage ~ secondSpokenLanguage ~ materialsLanguage ~ city ~
    countryCode ~ description ~ specialAttention ~ webSite ~ registrationPage ~ start ~ end ~ hoursPerDay ~ totalHours ~
    notPublic ~ archived ~ confirmed ~ created ~ createdBy returning id

  def forUpdate = eventTypeId ~ brandCode ~ title ~ spokenLanguage ~ secondSpokenLanguage ~ materialsLanguage ~ city ~
    countryCode ~ description ~ specialAttention ~ webSite ~ registrationPage ~ start ~ end ~ hoursPerDay ~ totalHours ~
    notPublic ~ archived ~ confirmed ~ updated ~ updatedBy
}
