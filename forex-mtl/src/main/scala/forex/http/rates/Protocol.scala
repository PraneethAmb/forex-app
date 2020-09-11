package forex.http
package rates

import forex.domain.Currency.show
import forex.domain.Price.Price
import forex.domain.Rate.Pair
import forex.domain._
import io.circe._
import io.circe.generic.semiauto._
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._

object Protocol {

  final case class GetApiResponse(
      from: Currency,
      to: Currency,
      price: Price,
      timestamp: Timestamp
  )

  final case class RemoteRate(
      from: String,
      to: String,
      bid: BigDecimal,
      ask: BigDecimal,
      price: BigDecimal,
      time_stamp: String
  )

  implicit def coercibleDecoder[A: Coercible[B, *], B: Decoder]: Decoder[A] = Decoder[B].map(_.coerce[A])
  implicit def coercibleEncoder[A: Coercible[B, *], B: Encoder]: Encoder[A] =
    Encoder[B].contramap(_.repr.asInstanceOf[B])

  implicit val currencyEncoder: Encoder[Currency]              = Encoder.instance[Currency] { show.show _ andThen Json.fromString }
  implicit val pairEncoder: Encoder[Pair]                      = deriveEncoder[Pair]
  implicit val rateEncoder: Encoder[Rate]                      = deriveEncoder[Rate]
  implicit val responseEncoder: Encoder[GetApiResponse]        = deriveEncoder[GetApiResponse]
  implicit val oneFrameApiResponseDecoder: Decoder[RemoteRate] = deriveDecoder[RemoteRate]
  implicit val oneFrameApiResponseEncoder: Encoder[RemoteRate] = deriveEncoder[RemoteRate]

}
