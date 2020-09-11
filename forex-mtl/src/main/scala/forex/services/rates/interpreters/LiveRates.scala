package forex.services.rates.interpreters

import java.time.OffsetDateTime

import cats.implicits._
import dev.profunktor.redis4cats.RedisCommands
import eu.timepit.refined.auto._
import eu.timepit.refined.types.all.PosInt
import eu.timepit.refined.types.string.NonEmptyString
import forex.domain.Rate.Pair
import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.http.rates.HttpClients
import forex.http.rates.Protocol.RemoteRate
import forex.programs.rates.Errors.Error.RateLookupFailedError
import forex.programs.rates.Protocol._
import forex.programs.rates.effects.MonadThrow
import forex.services.rates.RatesAlgebra
import io.circe.parser
import io.circe.syntax.EncoderOps

class LiveRates[F[_]: MonadThrow] private (redis: RedisCommands[F, String, String], httpClient: HttpClients[F])
    extends RatesAlgebra[F] {

  val status: NonEmptyString            = "Status"
  val statusInitial: NonEmptyString     = "Initial"
  val statusInvalid: NonEmptyString     = "Invalid"
  val statusUpdated: NonEmptyString     = "Updated"
  val lastUpdateTimeSec: NonEmptyString = "LastUpdateTime"
  val timeIntervalSec: PosInt           = 300 // 5 minutes

  override def checkLocalStorageUpdateNeeded(storageStatus: LocalStorageStatus, time: Timestamp): F[Boolean] =
    (storageStatus.status.eqv(statusInvalid) || time.value.toEpochSecond - storageStatus.lastUpdateTimeSec.toLong >= timeIntervalSec)
      .pure[F]

  override def getRatesLocalStorage(status: LocalStorageStatus, pair: Rate.Pair): F[Rate] =
    redis.get(pair.show).flatMap {
      case Some(result) =>
        parser.decode[RemoteRate](result) match {
          case Right(rate) =>
            Rate(
              Pair(Currency.fromString(rate.from), Currency.fromString(rate.to)),
              Price(rate.price),
              Timestamp(OffsetDateTime.parse(rate.time_stamp))
            ).pure[F]
          case Left(error) => RateLookupFailedError(s"Json Decode error: $error").raiseError[F, Rate]
        }
      case None => RateLookupFailedError("Local Storage Fetch error").raiseError[F, Rate]
    }

  override def updateLocalStorage(rates: Map[String, RemoteRate]): F[LocalStorageStatus] = {
    val updateTime = rates.values
      .foldLeft(OffsetDateTime.MAX)(
        (acc, i) =>
          if (acc.toEpochSecond <= OffsetDateTime.parse(i.time_stamp).toEpochSecond) acc
          else OffsetDateTime.parse(i.time_stamp)
      )
    rates.toList
      .traverse(kv => redis.getSet(kv._1.show, kv._2.asJson.show)) >>
      redis.getSet(lastUpdateTimeSec, updateTime.toEpochSecond.show) >>
      redis.getSet(status, statusUpdated) >>
      getLocalStorageStatus
  }

  override def getLocalStorageStatus: F[LocalStorageStatus] =
    redis.mGet(Set(status, lastUpdateTimeSec)).flatMap {
      case result if result.isDefinedAt(status) && result.isDefinedAt(lastUpdateTimeSec) =>
        LocalStorageStatus(result(status), result(lastUpdateTimeSec)).pure[F]
      case _ => LocalStorageStatus(statusInitial, 0.show).pure[F]
    }

  override def getRatesRemote(pair: Rate.Pair): F[Map[String, RemoteRate]] =
    queryRemote()

  private def queryRemote(): F[Map[String, RemoteRate]] =
    httpClient.httpClient.getRates.flatMap { result =>
      result
        .map(r => (Pair(Currency.fromString(r.from), Currency.fromString(r.to)).show, r))
        .toMap
        .pure[F]
    }

  override def tryInvalidateLocalStorage(): F[LocalStorageInvalidateResult] =
    redis.getSet(status, statusInvalid).flatMap {
      case Some(r) if r.eqv(statusInvalid) => LocalStorageInvalidateResult(AlreadyInvalidated).pure[F]
      case _                               => LocalStorageInvalidateResult(InvalidateSuccess).pure[F]
    }
}

object LiveRates {
  def create[F[_]: MonadThrow](redis: RedisCommands[F, String, String], httpClient: HttpClients[F]): RatesAlgebra[F] =
    new LiveRates[F](redis, httpClient)
}
