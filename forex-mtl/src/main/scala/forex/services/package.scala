package forex

package object services {
  type RatesService[F[_]] = rates.RatesAlgebra[F]
  final val RatesServices = rates.interpreters.LiveRates
}
