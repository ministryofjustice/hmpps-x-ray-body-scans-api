package uk.gov.justice.digital.hmpps.xraybodyscansapi.client.prisonapi

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.util.UriBuilder
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.prisonapi.response.PersonalCareNeedsResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.DownstreamServiceException

@Component
class PrisonApiClient(
  @Qualifier("prisonApiWebClient") private val webClient: WebClient,
) {

  /**
   * Returns a list of bscan PersonalCareNeedsResponses, one for each prisoner number given.
   * Requires ROLE_VIEW_PRISONER_DATA or ROLE_GLOBAL_SEARCH.
   */
  fun getScanCareNeeds(offenderNos: List<String>): List<PersonalCareNeedsResponse> = try {
    webClient
      .post()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/bookings/offenderNo/personal-care-needs")
          .queryParam("type", "BSCAN")
          .build()
      }
      .bodyValue(offenderNos)
      .retrieve()
      .bodyToMono<List<PersonalCareNeedsResponse>>()
      .block()
      .orEmpty()
  } catch (e: Exception) {
    throw DownstreamServiceException("Prison API get scans request failed", e)
  }
}
