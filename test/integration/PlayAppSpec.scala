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
 * or in writing Happy Melly One, Handelsplein 37, Rotterdam,
 * The Netherlands, 3071 PR
 */
package integration

import org.specs2.mutable._
import org.specs2.specification._
import play.api.test.FakeApplication
import play.api.Play

trait PlayAppSpec extends Specification with BeforeAllAfterAll {
  sequential
  lazy val app: FakeApplication = {
    val conf = Map(
      "db.default.url" -> "jdbc:mysql://localhost/mellytest",
      "logger.play" -> "INFO",
      "logger.application" -> "DEBUG")
    val withoutPlugins = List("com.github.mumoshu.play2.memcached.MemcachedPlugin")
    FakeApplication(
      additionalConfiguration = conf,
      withoutPlugins = withoutPlugins)
  }

  def beforeAll() {
    Play.start(app)
    setupDb()
  }

  def afterAll() {
    cleanupDb()
    Play.stop()
  }

  def setupDb()
  def cleanupDb()
}
import org.specs2.specification.Step

trait BeforeAllAfterAll extends Specification {
  // see http://bit.ly/11I9kFM (specs2 User Guide)
  override def map(fragments: ⇒ Fragments) = {
    Step(beforeAll) ^ fragments ^ Step(afterAll)
  }

  protected def beforeAll()
  protected def afterAll()
}
