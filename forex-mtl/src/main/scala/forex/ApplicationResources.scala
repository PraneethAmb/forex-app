package forex

import cats.effect.{ ConcurrentEffect, ContextShift, Resource }
import cats.implicits.catsSyntaxTuple2Semigroupal
import dev.profunktor.redis4cats.log4cats._
import dev.profunktor.redis4cats.{ Redis, RedisCommands }
import eu.timepit.refined.auto._
import forex.config.ApplicationConfig.{ HttpClientConfig, RedisConfig, Settings }
import io.chrisdavenport.log4cats.Logger
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder

import scala.concurrent.ExecutionContext

final case class ApplicationResources[F[_]](httpClient: Client[F], redis: RedisCommands[F, String, String])

object ApplicationResources {

  def create[F[_]: ConcurrentEffect: ContextShift: Logger](cfg: Settings): Resource[F, ApplicationResources[F]] = {

    def createRedisResource(c: RedisConfig): Resource[F, RedisCommands[F, String, String]] =
      Redis[F].utf8(c.uri.value)

    def createHttpClient(c: HttpClientConfig): Resource[F, Client[F]] =
      BlazeClientBuilder[F](ExecutionContext.global)
        .withConnectTimeout(c.connectTimeout)
        .withRequestTimeout(c.requestTimeout)
        .resource

    (
      createHttpClient(cfg.httpClientConfig),
      createRedisResource(cfg.redisConfig)
    ).mapN(ApplicationResources.apply[F])

  }

}
