package forex.http.rates

import cats.data.{ Kleisli, OptionT }
import cats.effect.Sync
import cats.implicits._
import forex.domain.Rate._
import forex.domain.{ RegularUser, Timestamp }
import forex.http.rates.Converters.GetApiResponseOps
import forex.programs.RatesProgram
import forex.programs.rates.Errors.Error.RateLookupFailedError
import forex.programs.rates.Protocol.GetRatesRequest
import org.http4s.AuthedRoutes
import org.http4s.dsl.Http4sDsl

sealed trait Route
case class AuthenticatedRoute[F[_]](prefix: String, route: AuthedRoutes[RegularUser, F[*]]) extends Route

class RatesHttpRoutes[F[_]: Sync](rates: RatesProgram[F]) extends Http4sDsl[F] {

  import QueryParams._

  private[http] val prefixPath = "/v1/rates"
  private val httpRoutes: AuthedRoutes[RegularUser, F] = AuthedRoutes.of {
    case GET -> Root :? FromQueryParam(from) +& ToQueryParam(to) as _ =>
      (for {
        fromCurrency <- from.leftMap(_.head.sanitized).toEither
        toCurrency <- to.leftMap(_.head.sanitized).toEither
        pair <- getPair(fromCurrency, toCurrency)
      } yield pair) match {
        case Left(error) => BadRequest { error }
        case Right(pair) =>
          rates
            .get(GetRatesRequest(pair, Timestamp.now))
            .flatMap { rate =>
              Ok(rate.asGetApiResponse)
            }
            .recoverWith {
              case RateLookupFailedError(error) => Conflict(error)
            }
      }
  }
  val onFailure: AuthedRoutes[String, F] = Kleisli(req => OptionT.liftF(Forbidden(req.context)))
  val routes: AuthenticatedRoute[F]      = AuthenticatedRoute(prefixPath, httpRoutes)
}
