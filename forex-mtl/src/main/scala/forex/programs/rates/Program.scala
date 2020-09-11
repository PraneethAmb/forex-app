package forex.programs.rates

import cats.effect.{ Bracket, Timer }
import cats.implicits._
import cats.{ Monad, MonadError }
import eu.timepit.refined.auto._
import eu.timepit.refined.types.string.NonEmptyString
import forex.domain._
import forex.http.rates.Protocol.RemoteRate
import forex.programs.rates.Errors.Error.RateLookupFailedError
import forex.programs.rates.Protocol._
import forex.programs.rates.effects.MonadThrow
import forex.services.RatesService
import io.chrisdavenport.log4cats.Logger
import retry.RetryDetails._
import retry.RetryPolicies.{ constantDelay, exponentialBackoff, limitRetries }
import retry._

import scala.concurrent.duration.DurationInt

object effects {
  type MonadThrow[F[_]]   = MonadError[F, Throwable]
  type BracketThrow[F[_]] = Bracket[F, Throwable]

  object MonadThrow {
    def apply[F[_]](implicit ev: MonadError[F, Throwable]): MonadThrow[F] = ev
  }

  object BracketThrow {
    def apply[F[_]](implicit ev: Bracket[F, Throwable]): BracketThrow[F] = ev
  }
}

class Program[F[_]: Monad: Logger: MonadThrow: Timer] private (
    ratesService: RatesService[F]
) extends ProgramAlgebra[F] {

  /* default retry policy used when accessing remote server and local storage*/
  private val retryPolicy: RetryPolicy[F] =
    limitRetries[F](3) |+| exponentialBackoff[F](2.seconds)

  override def get(request: Protocol.GetRatesRequest): F[Rate] =
    for {
      status <- getLocalStorageStatus
      needUpdate <- ratesService.checkLocalStorageUpdateNeeded(status, request.timestamp)
      rates <- if (!needUpdate) getRatesLocalStorage(status, request.pair) else getUpdatedRates(request)
    } yield rates

  /*get updated rates. if the status is AlreadyInvalidated, which means a different node has already
   * invalidated the local storage and would update the local storage with new rates.
   * there for if status is AlreadyInvalidated, this wait till local storage is updated and get the new rates
   * */
  private def getUpdatedRates(request: Protocol.GetRatesRequest): F[Rate] = {
    val statusInvalidated = for {
      invalidated <- tryInvalidateLocalStorage()
    } yield invalidated
    statusInvalidated.flatMap { inValidatedResult =>
      inValidatedResult.result match {
        case InvalidateSuccess =>
          for {
            remoteRates <- getRatesRemote(request.pair)
            updatedStatus <- updateLocalStorage(remoteRates)
            updatedRates <- getRatesLocalStorage(updatedStatus, request.pair)
          } yield updatedRates
        case AlreadyInvalidated =>
          for {
            status <- checkLocalStorageStatus()
            updatedRates <- getRatesLocalStorage(status, request.pair)
          } yield updatedRates
      }
    }
  }

  private def logError(action: String)(e: Throwable, details: RetryDetails): F[Unit] =
    details match {
      case r: WillDelayAndRetry =>
        Logger[F].error(s"$action Error: ${e.getMessage} Tries ${r.retriesSoFar} times.")
      case g: GivingUp =>
        Logger[F].error(s"$action Error: ${e.getMessage} Giving up after ${g.totalRetries} retries.")
    }

  /* wrapper method to wrap retry logic with tryInvalidateLocalStorage function */
  private def tryInvalidateLocalStorage(): F[LocalStorageInvalidateResult] = {
    val action = retryingOnAllErrors[LocalStorageInvalidateResult](
      policy = retryPolicy,
      onError = logError("Local storage state in invalid waiting till its updated.")
    )(ratesService.tryInvalidateLocalStorage())
    action.adaptError {
      case e =>
        RateLookupFailedError(
          s"Invalidating Local Storage data failed: ${Option(e.getMessage).getOrElse("Unknown Error")}"
        )
    }
  }

  /* wrapper method to wrap retry logic with getLocalStorageStatus function */
  private def getLocalStorageStatus: F[LocalStorageStatus] = {
    val action = retryingOnAllErrors[LocalStorageStatus](
      policy = retryPolicy,
      onError =
        logError("Error retrieving Local Storage status. Manually reset storage server to avoid data inconsistency.")
    )(ratesService.getLocalStorageStatus)
    action.adaptError {
      case e =>
        RateLookupFailedError(
          s"Getting Local Storage status failed: ${Option(e.getMessage).getOrElse("Unknown Error")}"
        )
    }
  }

  /* wrapper method to wrap retry logic with getLocalStorageStatus function
   *  used when invalidating local storage fails. Which means that another node is already trying to update
   *  the local storage. This function retries 3 times. Its assumed with in that time the local storage should be updated
   *  if it is not updated, the client will be asked to retry again later.
   *  */
  private def checkLocalStorageStatus(): F[LocalStorageStatus] = {
    val statusUpdated: NonEmptyString = "Updated"

    def handleFailure(action: String)(s: LocalStorageStatus, details: RetryDetails): F[Unit] =
      details match {
        case r: WillDelayAndRetry =>
          Logger[F].info(s"$action Tries ${r.retriesSoFar} times.")
        case g: GivingUp =>
          Logger[F].info(s"$action Giving up after ${g.totalRetries} retries.")
          RateLookupFailedError(
            s"Could not validate Local Storage Status. Current Status ${s.status}. Please Try again later"
          ).raiseError[F, Unit]
      }

    val action = retryingM[LocalStorageStatus](
      policy = retryPolicy,
      wasSuccessful = s => s.status.eqv(statusUpdated),
      onFailure = handleFailure("Storage state in invalid waiting till its updated.")
    )(ratesService.getLocalStorageStatus)

    action.adaptError {
      case _ =>
        RateLookupFailedError("Could not validate Local Storage Status. Please Try again later")
    }
  }

  /* wrapper method to wrap retry logic with updateLocalStorage function */
  private def updateLocalStorage(rates: Map[String, RemoteRate]): F[LocalStorageStatus] = {
    val action = retryingOnAllErrors[LocalStorageStatus](
      policy = retryPolicy,
      onError = logError("Error updating local storage. Manually reset storage server to avoid data inconsistency.")
    )(ratesService.updateLocalStorage(rates))

    action.adaptError {
      case e =>
        RateLookupFailedError(s"Updating Local Storage failed: ${Option(e.getMessage).getOrElse("Unknown Error")}")
    }
  }

  /* wrapper method to wrap retry logic with getRatesLocalStorage function */
  private def getRatesLocalStorage(status: LocalStorageStatus, pair: Rate.Pair): F[Rate] = {
    val action = retryingOnAllErrors[Rate](
      policy = retryPolicy,
      onError =
        logError("Error Fetching Rates from Local Storage. Manually reset storage server to avoid data inconsistency.")
    )(ratesService.getRatesLocalStorage(status, pair))

    action.adaptError {
      case e =>
        RateLookupFailedError(
          s"Fetching Rates from Local Storage Failed. Please Try again later: ${Option(e.getMessage).getOrElse("Unknown Error")}"
        )
    }
  }
  /* wrapper method to wrap retry logic with getRatesRemote function
   *  If there is a error from the remote server. this wrapper will initially retry three times
   *  after which it will retry once every 5 minutes indefinitely
   * */
  private def getRatesRemote(pair: Rate.Pair): F[Map[String, RemoteRate]] = {
    val action = retryingOnAllErrors[Map[String, RemoteRate]](
      policy = retryPolicy.followedBy(constantDelay[F](5.minute)),
      onError = logError("Error fetching rates from Remote Server.")
    )(ratesService.getRatesRemote(pair))

    action.adaptError {
      case e =>
        RateLookupFailedError(
          s"Error Fetching Rates from Remote Server. Please Try again later: ${Option(e.getMessage).getOrElse("Unknown Error")}"
        )
    }
  }
}

object Program {
  def create[F[_]: Monad: Logger: MonadThrow: Timer](
      ratesService: RatesService[F]
  ): ProgramAlgebra[F] = new Program[F](ratesService)
}
