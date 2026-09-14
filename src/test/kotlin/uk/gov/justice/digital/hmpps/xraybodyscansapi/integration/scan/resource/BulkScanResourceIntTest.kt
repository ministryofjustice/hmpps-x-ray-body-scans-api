package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.scan.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.test.json.JsonCompareMode
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.ADMIN_ROLE
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.READ_ROLE
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.WRITE_ROLE
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
    @TestFactory
    @DisplayName("Endpoint is protected")
    fun `endpoint is protected`() = endpointIsProtected(
      webTestClient.post()
        .uri("/bulk/summary")
        .bodyValue(
          // language=json
          """{"prisonerNumbers": ["A1234BC"]}""",
        ),
      authorisedRoles = setOf(READ_ROLE, WRITE_ROLE, ADMIN_ROLE),
      setupSuccess = {
        whenever(scanService.summariseScans(any<List<String>>(), any(), any()))
          .thenReturn(emptyList())
      },
      verifyFailure = {
        verifyNoInteractions(scanService)
      },
    )

    @Nested
    @DisplayName("Happy paths")
    inner class HappyPath {
      @Test
      fun `returns scan summaries for multiple prisoners`() {
        val expected = listOf(
          summaryResponse(prisonerNumber = "A1234BC", nomisCount = 4, dpsCount = 2, positiveCount = 1, negativeCount = 1, inconclusiveCount = 1),
          summaryResponse(prisonerNumber = "B5678DE", nomisCount = 0, dpsCount = 1, negativeCount = 1),
        )
        whenever(scanService.summariseScans(any<List<String>>(), any(), any()))
          .thenReturn(expected)

        val result = webTestClient.post()
          .uri("/bulk/summary")
          .headers(setAuthorisation(roles = listOf(READ_ROLE)))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue("""{"prisonerNumbers":["A1234BC","B5678DE"]}""")
          .exchange()
          .expectStatus().isOk
          .expectBodyList<ScanSummaryResponse>()
          .returnResult().responseBody

        assertThat(result).isEqualTo(expected)
        verify(scanService).summariseScans(eq(listOf("A1234BC", "B5678DE")), eq(false), eq(IncludeAlerts.No))
      }

      @Test
      fun `returns latest scans when requested`() {
        whenever(scanService.summariseScans(any<List<String>>(), any(), any()))
          .thenReturn(
            listOf(
              summaryResponse(
                prisonerNumber = "A1111AA",
                nomisCount = 0,
                dpsCount = 1,
                latestScan = dpsScanResponse(prisonerNumber = "A1111AA"),
              ),
              summaryResponse(
                prisonerNumber = "B2222BB",
                nomisCount = 1,
                dpsCount = 0,
                latestScan = nomisScanResponse(prisonerNumber = "B2222BB"),
              ),
            ),
          )

        webTestClient.post()
          .uri("/bulk/summary")
          .headers(setAuthorisation(roles = listOf(READ_ROLE)))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            // language=json
            """
            {"prisonerNumbers": ["A1111AA", "B2222BB"], "includeLatestScans": true}
            """,
          )
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json(
            // language=json
            """
            [
              {
                "prisonerNumber": "A1111AA",
                "nomisCount": 0,
                "dpsCount": 1,
                "latestScan": {
                  "source": "DPS",
                  "id": "$scanId",
                  "prisonerNumber": "A1111AA"
                }
              },
              {
                "prisonerNumber": "B2222BB",
                "nomisCount": 1,
                "dpsCount": 0,
                "latestScan": {
                  "source": "NOMIS",
                  "id": "$legacyId",
                  "prisonerNumber": "B2222BB"
                }
              }
            ]
            """,
            JsonCompareMode.LENIENT,
          )

        verify(scanService).summariseScans(eq(listOf("A1111AA", "B2222BB")), eq(true), eq(IncludeAlerts.No))
      }

      @Test
      fun `returns relevant alerts when requested`() {
        val response = summaryResponse(
          prisonerNumber = "A1234BC",
          nomisCount = 0,
          dpsCount = 0,
          relevantAlerts = listOf(alertResponse(), alertResponse("XXRAY", "Do Not X-Ray Body Scan")),
        )
        whenever(scanService.summariseScans(any<List<String>>(), any(), any()))
          .thenReturn(listOf(response))

        val result = webTestClient.post()
          .uri("/bulk/summary")
          .headers(setAuthorisation(roles = listOf(READ_ROLE)))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            // language=json
            """
            {"prisonerNumbers": ["A1234BC"], "includeAlerts": true}
            """,
          )
          .exchange()
          .expectStatus().isOk
          .expectBodyList<ScanSummaryResponse>()
          .returnResult().responseBody

        assertThat(result).isEqualTo(listOf(response))
        verify(scanService).summariseScans(eq(listOf("A1234BC")), eq(false), eq(IncludeAlerts.WithUsername("AUTH_ADM")))
      }
    }

    @Nested
    @DisplayName("Sad paths")
    inner class SadPath {
      @Test
      fun `returns 400 when prisonerNumbers is empty`() {
        webTestClient.post()
          .uri("/bulk/summary")
          .headers(setAuthorisation(roles = listOf(READ_ROLE)))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue("""{"prisonerNumbers": []}""")
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
          .headers(setAuthorisation(roles = listOf(READ_ROLE)))
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
