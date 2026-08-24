package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.referencedata

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertNotNull
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.IntegrationTestBase

@DisplayName("Reference data resource")
class ReferenceDataResourceIntTest : IntegrationTestBase() {
  @DisplayName("endpoint is protected")
  @TestFactory
  fun `endpoint is protected`() = endpointIsProtected(
    webTestClient.get().uri("/reference-data"),
    readRole = ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO,
  )

  @Test
  fun `returns reference data map`() {
    webTestClient.get()
      .uri("/reference-data")
      .headers(setAuthorisation(roles = listOf(ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody()
      .jsonPath("$").value<Map<String, Map<String, Any>>> { referenceData ->
        assertThat(referenceData).hasSize(3)
        val domain = referenceData["JUSTIFICATION"]
        assertNotNull(domain)
        assertThat(domain["code"]).isEqualTo("JUSTIFICATION")
        val codes = domain["codes"] as List<*>
        assertThat(codes).hasSize(2)
        assertThat(codes).anyMatch { code ->
          (code as Map<*, *>)["code"] == "INTELLIGENCE"
        }
      }
  }
}
