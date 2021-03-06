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
package helpers

import models._
import org.joda.time.{ DateTime, LocalDate }
import play.api.libs.json.{ Json, JsValue }

/** Provides a set of useful functions for testing */
object PersonHelper {

  def make(id: Option[Long],
    firstName: String,
    lastName: String,
    birthday: Option[LocalDate] = None,
    photo: Option[Photo] = None,
    signature: Boolean = false,
    bio: Option[String] = None,
    interests: Option[String] = None,
    webSite: Option[String] = None,
    blog: Option[String] = None,
    virtual: Boolean = false,
    active: Boolean = true): Person = {
    val address = new Address(id = Some(1), countryCode = "UK")
    val realPhoto = new Photo(None, None)
    val dateStamp = new DateStamp(DateTime.now(),
      "Sergey Kotlov",
      DateTime.now(),
      "Sergey Kotlov")
    val person = new Person(id, firstName, lastName, birthday, realPhoto,
      signature, address.id.get, bio, interests, PersonRole.NoRole,
      webSite, blog, virtual, active, dateStamp)
    person.address_=(address)
    person
  }

  def one(): Person = make(Some(1), "First", "Tester")
  def two(): Person = make(Some(2), "Second", "Tester")

  def oneAsJson(): JsValue = Json.obj(
    "id" -> 1,
    "unique_name" -> "first.tester",
    "href" -> "/api/v1/person/1",
    "first_name" -> "First",
    "last_name" -> "Tester",
    "photo" -> None.asInstanceOf[Option[String]],
    "country" -> "UK")

  def twoAsJson(): JsValue = Json.obj(
    "id" -> 2,
    "unique_name" -> "second.tester",
    "href" -> "/api/v1/person/2",
    "first_name" -> "Second",
    "last_name" -> "Tester",
    "photo" -> None.asInstanceOf[Option[String]],
    "country" -> "UK")
}
