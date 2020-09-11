package forex.services.rates.interpreters

import cats.effect.Sync
import cats.implicits._
import eu.timepit.refined.auto._
import forex.config.ApplicationConfig.OneFrameConfig
import forex.domain.Currency.currencyPairs
import forex.http.rates.HttpClients
import forex.http.rates.Protocol.RemoteRate
import forex.programs.rates.Errors.Error.RateLookupFailedError
import forex.programs.rates.effects.BracketThrow
import forex.services.rates.HttpClientAlgebra
import org.http4s.Method._
import org.http4s._
import org.http4s.circe._
import org.http4s.client._
import org.http4s.client.dsl.Http4sClientDsl

final class LiveHttpClient[F[_]: JsonDecoder: BracketThrow] private (
    cfg: OneFrameConfig,
    client: Client[F]
) extends HttpClientAlgebra[F]
    with Http4sClientDsl[F] {

  lazy val queryParams: String = currencyPairs.map(_.show).mkString("&pair=")

  def getRates: F[List[RemoteRate]] =
    Uri.fromString(cfg.uri.value.value + s"/rates?pair=$queryParams").liftTo[F].flatMap { uri =>
      GET(uri, Header("token", cfg.token.value)).flatMap { req =>
        client.run(req).use { r =>
          if (r.status == Status.Ok || r.status == Status.Conflict) {
            r.asJsonDecode[List[RemoteRate]]
          } else
            RateLookupFailedError(
              s"Remote data fetch error :${Option(r.status.reason).getOrElse("unknown")}"
            ).raiseError[F, List[RemoteRate]]
        }
      }
    }
}

object LiveHttpClient {
  def create[F[_]: Sync](
      cfg: OneFrameConfig,
      client: Client[F]
  ): F[HttpClients[F]] =
    Sync[F].delay(
      new HttpClients[F] {
        def httpClient: LiveHttpClient[F] = new LiveHttpClient[F](cfg, client)
      }
    )
}
