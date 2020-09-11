package forex.services.rates

import forex.http.rates.Protocol.RemoteRate

trait HttpClientAlgebra[F[_]] {
  def getRates: F[List[RemoteRate]]
}
