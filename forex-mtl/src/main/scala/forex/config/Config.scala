package forex.config

import cats.effect._
import ciris._
import eu.timepit.refined.auto._
import forex.config.ApplicationConfig._

import scala.concurrent.duration._

/* In production environments tokens and passwords should not be set directly. Tokens are set here for simplicity */
object Config {
  def apply[F[_]: Async: ContextShift]: F[Settings] =
    default(
      redisUri = RedisURI("redis://localhost"),
      oneFrameUri = OneFrameURI("http://localhost:8000"),
      oneFrameToken = OneFrameToken("10dc303535874aeccc86a8251e6992f5"),
      regularUserToken = ApplicationToken("10dc303535874aeccc86a8251e6992f5")
    ).load[F]

  private def default(
      redisUri: RedisURI,
      oneFrameUri: OneFrameURI,
      oneFrameToken: OneFrameToken,
      regularUserToken: ApplicationToken
  ): ConfigValue[Settings] =
    ConfigValue.default(
      Settings(
        OneFrameConfig(oneFrameUri, oneFrameToken),
        HttpServerConfig(
          host = "0.0.0.0",
          port = 8080,
          timeout = 20.seconds
        ),
        HttpClientConfig(
          connectTimeout = 2.seconds,
          requestTimeout = 2.seconds
        ),
        RedisConfig(redisUri),
        RegularUserToken(regularUserToken)
      )
    )
}
