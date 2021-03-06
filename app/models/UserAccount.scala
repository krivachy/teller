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

package models

import be.objectify.deadbolt.core.models.{ Permission, Subject }
import models.database.UserAccounts
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB
import play.api.Play.current
import play.libs.Scala

/**
 * A log-in user account.
 */
case class UserAccount(id: Option[Long], personId: Long, role: String, twitterHandle: Option[String],
  facebookUrl: Option[String], linkedInUrl: Option[String], googlePlusUrl: Option[String]) extends Subject {

  private var _roles: Option[java.util.List[UserRole]] = None

  def roles_=(roles: java.util.List[UserRole]): Unit = {
    _roles = Some(roles)
  }

  /**
   * Returns a string list of role names, for the Subject interface.
   */
  def getRoles: java.util.List[UserRole] = if (_roles.isEmpty) {
    val accountRole = UserAccount.findRole(personId).map(role ⇒ UserRole(role))
    roles_=(accountRole.map(_.list).getOrElse(java.util.Collections.emptyList()))
    _roles.get
  } else {
    _roles.get
  }

  lazy val admin = getRoles.contains(UserRole(UserRole.Role.Admin))
  lazy val editor = getRoles.contains(UserRole(UserRole.Role.Editor))
  lazy val viewer = getRoles.contains(UserRole(UserRole.Role.Viewer))

  def getIdentifier = personId.toString
  def getPermissions: java.util.List[Permission] = Scala.asJava(List.empty[Permission])

  def facilitator: Boolean = {
    !licenses.isEmpty || !brands.isEmpty
  }

  def coordinator: Boolean = {
    !brands.isEmpty
  }

  lazy val person: Option[Person] = Person.find(personId)
  lazy val account: Option[Account] = Account.findByPerson(personId)
  lazy val licenses: List[LicenseView] = License.activeLicenses(personId)
  lazy val brands: List[Brand] = Brand.findByCoordinator(personId)

}

object UserAccount {

  def delete(personId: Long) = DB.withSession { implicit session: Session ⇒
    UserAccounts.where(_.personId === personId).mutate(_.delete)
  }

  /**
   * Returns the account for the person who has a duplicate social network identity, if there is one.
   */
  def findDuplicateIdentity(person: Person): Option[UserAccount] = DB.withSession { implicit session: Session ⇒
    val query = Query(UserAccounts).filter(_.personId =!= person.id).filter { account ⇒
      account.twitterHandle.toLowerCase === person.socialProfile.twitterHandle.map(_.toLowerCase) ||
        account.googlePlusUrl === person.socialProfile.googlePlusUrl ||
        (account.facebookUrl like "https?".r.replaceFirstIn(person.socialProfile.facebookUrl.getOrElse(""), "%")) ||
        (account.linkedInUrl like "https?".r.replaceFirstIn(person.socialProfile.linkedInUrl.getOrElse(""), "%"))
    }
    query.firstOption
  }

  def insert(account: UserAccount) = DB.withSession { implicit session: Session ⇒
    val id = UserAccounts.forInsert.insert(account)
    account.copy(id = Some(id))
  }

  /**
   * Returns the given person’s role.
   */
  def findRole(personId: Long): Option[UserRole.Role.Role] = DB.withSession { implicit session: Session ⇒
    val query = for {
      account ← UserAccounts if account.personId === personId
    } yield account.role
    query.firstOption.map(role ⇒ UserRole.Role.withName(role))
  }

  /**
   * Updates the user’s role.
   */
  def updateRole(personId: Long, role: String): Unit = DB.withSession { implicit session: Session ⇒
    val query = for {
      account ← UserAccounts if account.personId === personId
    } yield account.role
    query.update(role)
  }

  /**
   * Updates the social network authentication provider identifiers, used when these may have been edited for a person,
   * so that an existing account can be able to log in on a new provider or for a provider with a edited identifier.
   */
  def updateSocialNetworkProfiles(person: Person): Unit = DB.withSession { implicit session: Session ⇒
    val query = for {
      account ← UserAccounts if account.personId === person.id
    } yield account.twitterHandle ~ account.facebookUrl ~ account.googlePlusUrl ~ account.linkedInUrl
    query.update(person.socialProfile.twitterHandle, person.socialProfile.facebookUrl,
      person.socialProfile.googlePlusUrl, person.socialProfile.linkedInUrl)
  }
}
