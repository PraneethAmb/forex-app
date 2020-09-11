package forex.programs.rates

import forex.domain.{ Rate, Timestamp }

object Protocol {
  sealed trait InvalidateResult
  final case object InvalidateSuccess extends InvalidateResult
  final case object AlreadyInvalidated extends InvalidateResult
  final case class LocalStorageInvalidateResult(result: InvalidateResult)
  final case class LocalStorageStatus(status: String, lastUpdateTimeSec: String)

  final case class GetRatesRequest(
      pair: Rate.Pair,
      timestamp: Timestamp
  )
}
