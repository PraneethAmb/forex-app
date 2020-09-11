package forex.http.rates

import eu.timepit.refined.auto._
import forex.domain.Currency
import forex.domain.Currency.INVALID
import forex.programs.rates.Errors.Error.InvalidCurrencyError
import org.http4s.dsl.impl.ValidatingQueryParamDecoderMatcher
import org.http4s.{ ParseFailure, QueryParamDecoder }

object QueryParams {

  private[http] implicit val currencyQueryParam: QueryParamDecoder[Currency] =
    QueryParamDecoder[String].emap { c =>
      Currency.fromString(c) match {
        case INVALID  => Left(ParseFailure(InvalidCurrencyError().msg, c))
        case currency => Right(currency)
      }
    }

  object FromQueryParam extends ValidatingQueryParamDecoderMatcher[Currency]("from")
  object ToQueryParam extends ValidatingQueryParamDecoderMatcher[Currency]("to")
}
