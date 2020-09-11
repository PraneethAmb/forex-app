package forex

import cats.Monad
import cats.data.Kleisli
import cats.effect.{ Concurrent, Timer }
import cats.implicits._
import dev.profunktor.redis4cats.RedisCommands
import eu.timepit.refined.auto._
import eu.timepit.refined.types.string.NonEmptyString
import forex.config.ApplicationConfig.Settings
import forex.domain.RegularUser
import forex.http.rates.{ AuthenticatedRoute, HttpClients, RatesHttpRoutes }
import forex.programs.RatesProgram
import forex.programs.rates.Errors.Error.{ InvalidAuthenticationHeader, InvalidRegularUser }
import forex.services._
import io.chrisdavenport.log4cats.Logger
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.middleware.{ AutoSlash, Timeout }
import org.http4s.server.{ AuthMiddleware, Router }
import org.http4s.util.CaseInsensitiveString

class Module[F[_]: Concurrent: Timer: Logger](config: Settings,
                                              redis: RedisCommands[F, String, String],
                                              httpClient: HttpClients[F]) {

  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]

  private val ratesService: RatesService[F] = RatesServices.create[F](redis, httpClient)
  private val ratesProgram: RatesProgram[F] = RatesProgram.create[F](ratesService)
  private val ratesHttpRoutes               = new RatesHttpRoutes[F](ratesProgram)
  private val authHeader: NonEmptyString    = "X-Auth-Regular-User"
  private val invalidHeader: String         = InvalidAuthenticationHeader().msg
  private val invalidUser: String           = InvalidRegularUser().msg

  private val authUser: Kleisli[F, Request[F], Either[String, RegularUser]] = Kleisli({ request =>
    val authToken = for {
      header <- request.headers
                 .get(CaseInsensitiveString(authHeader))
                 .toRight(invalidHeader)
      token <- (if (header.value.eqv(config.regularUserToken.token.value)) Some(header.value) else None)
                .toRight(invalidUser)
    } yield token
    authToken.traverse(retrieveRegularUser.run)
  })
  private val authMiddleware = AuthMiddleware(authUser, ratesHttpRoutes.onFailure)
  private val routesMiddleware: PartialMiddleware = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    }
  }
  private val appMiddleware: TotalMiddleware = { http: HttpApp[F] =>
    Timeout(config.httpServerConfig.timeout)(http)
  }
  private val http: AuthenticatedRoute[F] = ratesHttpRoutes.routes

  private def retrieveRegularUser: Kleisli[F, String, RegularUser] = Kleisli(id => Monad[F].pure(RegularUser(id)))

  val httpApp: HttpApp[F] = appMiddleware(
    routesMiddleware(Router(http.prefix -> authMiddleware {
      http.route
    })).orNotFound
  )
}
