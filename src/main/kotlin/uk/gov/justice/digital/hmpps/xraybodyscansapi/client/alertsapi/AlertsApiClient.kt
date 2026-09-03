package uk.gov.justice.digital.hmpps.xraybodyscansapi.client.alertsapi

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.alertsapi.response.AlertResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.DownstreamServiceException

@Component
class AlertsApiClient(
  @Qualifier("alertsApiWebClient") private val webClient: WebClient,
) {
  /** Bulk-get alerts for given prisoners. Requires ROLE_PRISONER_ALERTS__RO or ROLE_PRISONER_ALERTS__RW */
  fun getAlerts(
    prisonerNumbers: List<String>,
    filterAlertCodes: Set<String>? = null,
    username: String? = null,
  ): AlertResponse = try {
    webClient.post().uri {
      it.path("/search/alerts/prison-numbers")
      filterAlertCodes?.let { codes ->
        it.queryParam("filterAlertCodes", *codes.toTypedArray())
      }
      it.build()
    }
      .apply {
        if (username != null) {
          header("Username", username)
        }
      }
      .bodyValue(prisonerNumbers)
      .retrieve()
      .bodyToMono<AlertResponse>()
      .block()
      ?: AlertResponse(emptyList())
  } catch (e: Exception) {
    throw DownstreamServiceException("Alerts API bulk-get alerts request failed", e)
  }
}
