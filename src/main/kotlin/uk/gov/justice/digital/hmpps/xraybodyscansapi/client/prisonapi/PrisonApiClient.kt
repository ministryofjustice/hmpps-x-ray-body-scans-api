package uk.gov.justice.digital.hmpps.xraybodyscansapi.client.prisonapi
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.prisonapi.response.CountNomisScansResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.DownstreamServiceException
import java.time.LocalDate

@Component
class PrisonApiClient(
  @Qualifier("prisonApiWebClient") private val webClient: WebClient,
) {

  fun countNomisScans(
    offenderNos: List<String>,
    fromStartDate: LocalDate,
    toStartDate: LocalDate,
  ): List<CountNomisScansResponse> {
    try {
      val response = webClient
        .post()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/api/bookings/offenderNo/personal-care-needs/count")
            .queryParam("type", "BSCAN")
            .queryParam("fromStartDate", fromStartDate)
            .queryParam("toStartDate", toStartDate)
            .build()
        }
        .bodyValue(offenderNos)
        .retrieve()
        .bodyToMono(object : ParameterizedTypeReference<List<CountNomisScansResponse>>() {})
        .block()
        .orEmpty()
      return response
    } catch (e: Exception) {
      throw DownstreamServiceException("Prison API count scans request failed", e)
    }
  }
}
