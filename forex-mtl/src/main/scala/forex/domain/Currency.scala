package forex.domain

import cats.Show
import cats.implicits._
import cats.kernel.Eq
import forex.domain.Rate.Pair

sealed trait Currency

object Currency {
  case object AUD extends Currency
  case object CAD extends Currency
  case object CHF extends Currency
  case object EUR extends Currency
  case object GBP extends Currency
  case object NZD extends Currency
  case object JPY extends Currency
  case object SGD extends Currency
  case object USD extends Currency
  case object INVALID extends Currency

  implicit val show: Show[Currency] = Show.show {
    case AUD     => "AUD"
    case CAD     => "CAD"
    case CHF     => "CHF"
    case EUR     => "EUR"
    case GBP     => "GBP"
    case NZD     => "NZD"
    case JPY     => "JPY"
    case SGD     => "SGD"
    case USD     => "USD"
    case INVALID => "Invalid Currency"
  }

  def fromString(s: String): Currency = s.toUpperCase match {
    case "AUD" => AUD
    case "CAD" => CAD
    case "CHF" => CHF
    case "EUR" => EUR
    case "GBP" => GBP
    case "NZD" => NZD
    case "JPY" => JPY
    case "SGD" => SGD
    case "USD" => USD
    case _     => INVALID
  }

  implicit val eqCurrency: Eq[Currency] = Eq.fromUniversalEquals
  def isEqual(currency1: Currency, currency2: Currency): Boolean =
    currency1 === currency2

  /* Get all valid combinations of currency pairs*/
  lazy val currencyPairs: List[Pair] = List(AUD, CAD, CHF, EUR, GBP, NZD, JPY, SGD, USD)
    .combinations(2)
    .map { case Seq(x, y) => (Pair(x, y), Pair(y, x)) }
    .toList
    .flatMap(p => List(p._1, p._2))

}
