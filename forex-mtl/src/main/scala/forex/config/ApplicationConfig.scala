package forex.config

import eu.timepit.refined.types.net.UserPortNumber
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype

import scala.concurrent.duration.FiniteDuration

object ApplicationConfig {
  case class Settings(
      oneFrameConfig: OneFrameConfig,
      httpServerConfig: HttpServerConfig,
      httpClientConfig: HttpClientConfig,
      redisConfig: RedisConfig,
      regularUserToken: RegularUserToken
  )

  case class HttpServerConfig(
      host: NonEmptyString,
      port: UserPortNumber,
      timeout: FiniteDuration
  )

  case class HttpClientConfig(
      connectTimeout: FiniteDuration,
      requestTimeout: FiniteDuration
  )

  @newtype case class ApplicationToken(value: NonEmptyString)
  @newtype case class RegularUserToken(token: ApplicationToken)

  @newtype case class RedisURI(value: NonEmptyString)
  @newtype case class RedisConfig(uri: RedisURI)

  @newtype case class OneFrameURI(value: NonEmptyString)
  @newtype case class OneFrameToken(value: NonEmptyString)
  case class OneFrameConfig(
      uri: OneFrameURI,
      token: OneFrameToken
  )
}
