package forex.programs.rates

import forex.domain.Rate

trait ProgramAlgebra[F[_]] {
  def get(request: Protocol.GetRatesRequest): F[Rate]
}
