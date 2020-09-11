package forex.programs.rates

import eu.timepit.refined.auto._
import eu.timepit.refined.types.string.NonEmptyString

object Errors {
  sealed trait Error extends Exception

  object Error {
    final case class RateLookupFailedError(msg: String) extends Error
    final case class InvalidCurrencyError(msg: NonEmptyString = "Invalid Currency Specified") extends Error
    final case class SameCurrencyError(msg: NonEmptyString = "Two Currencies Cannot be Equal") extends Error
    final case class InvalidAuthenticationHeader(msg: NonEmptyString = "Invalid Authentication Header for Regular User")
        extends Error
    final case class InvalidRegularUser(msg: NonEmptyString = "Invalid Token for Regular User") extends Error
  }
}
