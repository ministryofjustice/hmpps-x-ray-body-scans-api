package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.scan.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanSummaryResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.service.IncludeAlerts

@DisplayName("Bulk x-ray body scans resource")
class BulkScanResourceIntTest(
  @Value($$"${scan.annual-limit}") scanAnnualLimit: Int,
  @Value($$"${scan.nearing-limit-threshold}") nearingLimitThreshold: Int,
) : BaseScanResourceIntTest(scanAnnualLimit, nearingLimitThreshold) {

  @Nested
  @DisplayName("Bulk summary endpoint")
  inner class BulkSummariseScans {
    @Nested
    @DisplayName("Happy paths")
    inner class HappyPath {
      @Test
      fun `returns scan summaries for multiple prisoners`() {
        val expected = listOf(
          summaryResponse(prisonerNumber = "A1234BC", nomisCount = 4, dpsCount = 2, positiveCount = 1, negativeCount = 1, inconclusiveCount = 1),
          summaryResponse(prisonerNumber = "B5678DE", nomisCount = 0, dpsCount = 1, negativeCount = 1),
        )
        whenever(scanService.summariseScans(any<List<String>>(), any()))
          .thenReturn(expected)

        val result = webTestClient.post()
          .uri("/bulk/summary")
          .headers(setAuthorisation(roles = listOf(ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO)))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue("""{"prisonerNumbers":["A1234BC","B5678DE"]}""")
          .exchange()
          .expectStatus().isOk
          .expectBodyList(ScanSummaryResponse::class.java)
          .returnResult().responseBody

        assertThat(result).isEqualTo(expected)
        verify(scanService).summariseScans(eq(listOf("A1234BC", "B5678DE")), eq(IncludeAlerts.No))
      }

      @Test
      fun `returns relevant alerts when requested`() {
        val response = summaryResponse(
          prisonerNumber = "A1234BC",
          nomisCount = 0,
          dpsCount = 0,
          relevantAlerts = listOf(alertResponse(), alertResponse("XXRAY", "Do Not X-Ray Body Scan")),
        )
        whenever(scanService.summariseScans(any<List<String>>(), any()))
          .thenReturn(listOf(response))

        val result = webTestClient.post()
          .uri("/bulk/summary")
          .headers(setAuthorisation(roles = listOf(ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO)))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            // language=json
            """
            {"prisonerNumbers": ["A1234BC"], "includeAlerts": true}
            """,
          )
          .exchange()
          .expectStatus().isOk
          .expectBodyList(ScanSummaryResponse::class.java)
          .returnResult().responseBody

        assertThat(result).isEqualTo(listOf(response))
        verify(scanService).summariseScans(eq(listOf("A1234BC")), eq(IncludeAlerts.WithUsername("AUTH_ADM")))
      }

      @ParameterizedTest(name = "permits role {0}")
      @ValueSource(
        strings = [
          ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO,
          ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW,
        ],
      )
      fun `permits role`(role: String) {
        whenever(scanService.summariseScans(listOf("A1234BC")))
          .thenReturn(listOf(summaryResponse(prisonerNumber = "A1234BC", nomisCount = 0, dpsCount = 0)))

        webTestClient.post()
          .uri("/bulk/summary")
          .headers(setAuthorisation(roles = listOf(role)))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue("""{"prisonerNumbers":["A1234BC"]}""")
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    @DisplayName("Sad paths")
    inner class SadPath {
      @TestFactory
      @DisplayName("endpoint is protected")
      fun `endpoint is protected`() = endpointIsProtected(
        webTestClient.post()
          .uri("/bulk/summary")
          .bodyValue("""{"prisonerNumbers":["A1234BC"]}"""),
        afterEach = {
          verifyNoInteractions(scanService)
        },
      )

      @Test
      fun `returns 400 when prisonerNumbers is empty`() {
        webTestClient.post()
          .uri("/bulk/summary")
          .headers(setAuthorisation(roles = listOf(ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO)))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue("""{"prisonerNumbers":[]}""")
          .exchange()
          .expectErrorResponse(
            userMessageContains = "Validation failure",
            developerMessageContains = "prisonerNumbers must not be empty",
          )

        verifyNoInteractions(scanService)
      }

      @Test
      fun `returns 400 when body is missing`() {
        webTestClient.post()
          .uri("/bulk/summary")
          .headers(setAuthorisation(roles = listOf(ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO)))
          .contentType(MediaType.APPLICATION_JSON)
          .exchange()
          .expectErrorResponse(
            userMessageContains = "Malformed request body",
            developerMessageContains = "Required request body is missing",
          )

        verifyNoInteractions(scanService)
      }
    }
  }
}
