package uk.gov.justice.digital.hmpps.xraybodyscansapi.client.alertsapi

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.alertsapi.response.AlertResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.DownstreamServiceException

@Component
class AlertsApiClient(
  @Qualifier("alertsApiWebClient") private val webClient: WebClient,
) {
  /** Bulk-get alerts for given prisoners. Requires ROLE_PRISONER_ALERTS__RO or ROLE_PRISONER_ALERTS__RW */
  fun getAlerts(prisonerNumbers: List<String>): AlertResponse = try {
    webClient.post().uri("/search/alerts/prison-numbers")
      .bodyValue(prisonerNumbers).retrieve()
      .bodyToMono(object : ParameterizedTypeReference<AlertResponse>() {})
      .block()
      ?: AlertResponse(emptyList())
  } catch (e: Exception) {
    throw DownstreamServiceException("Alerts API bulk-get alerts request failed", e)
  }
}
