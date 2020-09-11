package forex.services.rates

import forex.domain.{ Rate, Timestamp }
import forex.http.rates.Protocol.RemoteRate
import forex.programs.rates.Protocol._

trait RatesAlgebra[F[_]] {
  def getLocalStorageStatus: F[LocalStorageStatus]
  def checkLocalStorageUpdateNeeded(status: LocalStorageStatus, time: Timestamp): F[Boolean]
  def tryInvalidateLocalStorage(): F[LocalStorageInvalidateResult]
  def getRatesLocalStorage(status: LocalStorageStatus, pair: Rate.Pair): F[Rate]
  def getRatesRemote(pair: Rate.Pair): F[Map[String, RemoteRate]]
  def updateLocalStorage(rates: Map[String, RemoteRate]): F[LocalStorageStatus]
}
