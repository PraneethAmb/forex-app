package forex

import cats.effect._
import cats.implicits._
import forex.config._
import forex.services.rates.interpreters.LiveHttpClient
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.chrisdavenport.log4cats.{ Logger, SelfAwareStructuredLogger }
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext.global

object Main extends IOApp {
  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]
  override def run(args: List[String]): IO[ExitCode] =
    Config[IO].flatMap { cfg =>
      Logger[IO].info(s"Loaded config settings $cfg") >>
        ApplicationResources.create[IO](cfg).use { res =>
          for {
            httpClient <- LiveHttpClient.create[IO](cfg.oneFrameConfig, res.httpClient)
            module = new Module[IO](cfg, res.redis, httpClient)
            _ <- BlazeServerBuilder[IO](global)
                  .bindHttp(cfg.httpServerConfig.port.value, cfg.httpServerConfig.host.value)
                  .withHttpApp(module.httpApp)
                  .serve
                  .compile
                  .drain
          } yield ExitCode.Success
        }
    }
}
