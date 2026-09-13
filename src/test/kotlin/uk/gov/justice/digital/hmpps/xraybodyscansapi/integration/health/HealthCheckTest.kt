package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.health

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.wiremock.AlertsApiExtension.Companion.alertsApi
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.wiremock.CaseNotesApiExtension.Companion.caseNotesApi
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.wiremock.PrisonApiExtension.Companion.prisonApi

class HealthCheckTest : IntegrationTestBase() {
  private fun stubHealthPing(status: Int = 200) {
    hmppsAuth.stubHealthPing(status)
    alertsApi.stubHealthPing(status)
    caseNotesApi.stubHealthPing(status)
    prisonApi.stubHealthPing(status)
  }

  @Test
  fun `Health page reports ok`() {
    stubHealthPing()

    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `Health page reports down`() {
    stubHealthPing()
    hmppsAuth.stubHealthPing(503)

    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus()
      .is5xxServerError
      .expectBody()
      .jsonPath("status").isEqualTo("DOWN")
      .jsonPath("components.hmppsAuth.status").isEqualTo("DOWN")
  }

  @Test
  fun `Health ping page is accessible`() {
    webTestClient.get()
      .uri("/health/ping")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `readiness reports ok`() {
    webTestClient.get()
      .uri("/health/readiness")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `liveness reports ok`() {
    webTestClient.get()
      .uri("/health/liveness")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }
}
