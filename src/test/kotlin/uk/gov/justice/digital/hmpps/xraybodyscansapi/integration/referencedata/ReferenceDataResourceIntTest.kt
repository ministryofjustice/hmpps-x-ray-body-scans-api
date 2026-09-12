package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.referencedata

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertNotNull
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.ADMIN_ROLE
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.READ_ROLE
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.WRITE_ROLE
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.IntegrationTestBase

@DisplayName("Reference data resource")
class ReferenceDataResourceIntTest : IntegrationTestBase() {
  @DisplayName("Endpoint is protected")
  @TestFactory
  fun `endpoint is protected`() = endpointIsProtected(
    webTestClient.get().uri("/reference-data"),
    authorisedRoles = setOf(READ_ROLE, WRITE_ROLE, ADMIN_ROLE),
  )

  @Test
  fun `returns reference data map`() {
    val referenceData = webTestClient.get()
      .uri("/reference-data")
      .headers(setAuthorisation(roles = listOf(READ_ROLE)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody<Map<String, Map<String, Any>>>()
      .returnResult().responseBody

    assertThat(referenceData).hasSize(3)
    assertNotNull(referenceData)
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
