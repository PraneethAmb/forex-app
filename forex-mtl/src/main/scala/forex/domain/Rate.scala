package forex.domain

import cats.Show
import cats.implicits._
import eu.timepit.refined.auto._
import forex.domain.Price.Price
import forex.programs.rates.Errors.Error.SameCurrencyError

case class Rate(
    pair: Rate.Pair,
    price: Price,
    timestamp: Timestamp
)

object Rate {
  final case class Pair(
      from: Currency,
      to: Currency
  )

  implicit val show: Show[Pair] = Show.show {
    case Pair(f, t) => s"${f.show}${t.show}"
  }

  def getPair(from: Currency, to: Currency): Either[String, Pair] =
    Either.cond(
      !Currency.isEqual(from, to),
      Pair(from, to),
      SameCurrencyError().msg
    )
}
