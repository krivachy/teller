/*
 * Happy Melly Teller
 * Copyright (C) 2013, Happy Melly http://www.happymelly.com
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

import models.database.ExchangeRates
import org.joda.money.{ Money, CurrencyUnit }
import org.joda.time.{ LocalDate, DateTime }
import org.joda.time.DateTimeZone.UTC
import math.BigDecimal.int2bigDecimal
import java.math.RoundingMode
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB.withSession
import play.api.Play.current
import models.JodaMoney.CurrencyMapper
import com.github.tototoshi.slick.JodaSupport._

/**
 * Represents an exchange rate between two currencies at a certain time.
 *
 * The exchange rate means that a one `base` is worth `rate` in the `counter` currency.
 * @see http://en.wikipedia.org/wiki/Currency_pair
 * @see https://openexchangerates.org/documentation#how-to-use
 * @param base The base currency (a.k.a. ‘unit’)
 * @param counter The counter currency (a.k.a. ‘quote’)
 * @param rate The conversion rate
 * @param timestamp The time at which the exchange rate was determined
 */
case class ExchangeRate(id: Option[Long], base: CurrencyUnit, counter: CurrencyUnit, rate: BigDecimal, timestamp: DateTime) {
  if (rate.signum != 1) throw new IllegalArgumentException(s"Illegal rate: $rate")
  assert(!base.equals(counter) || (base.equals(counter) && rate.compare(1.bigDecimal) == 0))

  lazy val inverseRate = 1.bigDecimal.setScale(rate.scale).divide(rate.bigDecimal, RoundingMode.DOWN)

  /**
   * @see `convert`
   */
  def apply(amount: Money) = convert(amount)

  /**
   * Converts an amount from `base` or `counter` or vice versa, using `rate`.
   * @param amount The amount to convert
   * @return The converted amount.
   */
  def convert(amount: Money): Money = amount.getCurrencyUnit match {
    case currency if base.equals(counter) && currency.equals(base) ⇒ amount
    case currency if currency.equals(base) ⇒ amount.convertedTo(counter, rate.bigDecimal, RoundingMode.DOWN)
    case currency if currency.equals(counter) ⇒ amount.convertedTo(base, inverseRate, RoundingMode.DOWN)
    case currency ⇒ throw new IllegalArgumentException(s"Exchange rate is for $base $counter, tried to convert $currency")
  }

  /**
   * Checks if this ExchangeRate can convert the given amount.
   * @param amount
   * @return
   */
  def canConvert(amount: Money): Boolean = base.equals(amount.getCurrencyUnit) || counter.equals(amount.getCurrencyUnit)

  def insert: ExchangeRate = withSession { implicit session ⇒
    val id = ExchangeRates.forInsert.insert(this)
    this.copy(id = Some(id))
  }

  override def toString = s"$base$counter $rate"
}

object ExchangeRate {

  import play.api.Play.current

  private def today = LocalDate.now(UTC)

  def fromDatabase(base: CurrencyUnit, counter: CurrencyUnit, date: LocalDate = today): Option[ExchangeRate] = withSession {
    implicit session ⇒
      (for {
        rate ← ExchangeRates
        if rate.base === base
        if rate.counter === counter
        if rate.timestamp >= date.toDateTimeAtStartOfDay(UTC)
        if rate.timestamp < date.plusDays(1).toDateTimeAtStartOfDay(UTC)
      } yield rate).firstOption
  }

}
