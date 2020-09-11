package forex.domain

import io.estatico.newtype.macros.newtype

object Price {
  def apply(price: Int): Price = Price(BigDecimal(price))

  def apply(price: BigDecimal): Price = Price(price)

  @newtype case class Price(value: BigDecimal)
}
