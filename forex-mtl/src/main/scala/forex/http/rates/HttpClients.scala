package forex.http.rates

import forex.services.rates.interpreters.LiveHttpClient

trait HttpClients[F[_]] {
  def httpClient: LiveHttpClient[F]
}
