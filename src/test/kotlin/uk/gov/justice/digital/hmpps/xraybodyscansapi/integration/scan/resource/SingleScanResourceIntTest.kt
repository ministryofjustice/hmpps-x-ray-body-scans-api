package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.scan.resource

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.test.json.JsonCompareMode
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW

@DisplayName("Single x-ray body scan resource")
class SingleScanResourceIntTest(
  @Value($$"${scan.annual-limit}") scanAnnualLimit: Int,
  @Value($$"${scan.nearing-limit-threshold}") nearingLimitThreshold: Int,
) : BaseScanResourceIntTest(scanAnnualLimit, nearingLimitThreshold) {
  @Nested
  @DisplayName("Retrieving a single scan by id")
  inner class Get {
    @Nested
    @DisplayName("Happy paths")
    inner class HappyPath {
      @Test
      fun `returns a scan when one is found`() {
        whenever(scanService.getScans(listOf(scanId)))
          .thenReturn(listOf(dpsScanResponse(scanId, "A1234BC")))

        webTestClient.get()
          .uri("/scans/$scanId")
          .headers(setAuthorisation(roles = listOf(ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO)))
          .exchange()
          .expectStatus().isOk
          .expectHeader().contentType(MediaType.APPLICATION_JSON)
          .expectBody()
          .json(
            // language=json
            """
            {
              "id": "$scanId",
              "source": "DPS",
              "prisonerNumber": "A1234BC",
              "prisonId": "MDI",
              "scanDate": "2026-07-26",
              "justification": "INTELLIGENCE",
              "justificationDescription": "INTELLIGENCE",
              "outcome": "NEGATIVE",
              "outcomeDescription": "NEGATIVE",
              "typeOfFind": null,
              "typeOfFindDescription": null,
              "caseNoteId": null,
              "mergedFromPrisonerNumber": null,
              "mergedAt": null,
              "createdAt": "2026-07-27T09:10:11.123",
              "createdBy": "abc12ab",
              "lastModifiedAt": "2026-07-27T09:10:11.123",
              "lastModifiedBy": "abc12ab"
            }
            """,
            JsonCompareMode.STRICT,
          )
      }

      @Test
      fun `returns 404 when no scan is found`() {
        whenever(scanService.getScans(listOf(scanId)))
          .thenReturn(emptyList())

        webTestClient.get()
          .uri("/scans/$scanId")
          .headers(setAuthorisation(roles = listOf(ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO)))
          .exchange()
          .expectStatus().isNotFound
      }

      @ParameterizedTest(name = "permits role {0}")
      @ValueSource(
        strings = [
          ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO,
          ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW,
        ],
      )
      fun `permits role`(role: String) {
        whenever(scanService.getScans(listOf(scanId)))
          .thenReturn(listOf(dpsScanResponse(scanId, "A1234BC")))

        webTestClient.get()
          .uri("/scans/$scanId")
          .headers(setAuthorisation(roles = listOf(role)))
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
        webTestClient.get()
          .uri("/scans/$scanId"),
        readRole = ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO,
        afterEach = {
          verifyNoInteractions(scanService)
        },
      )

      @Test
      fun `returns 400 when id is not a UUID`() {
        webTestClient.get()
          .uri("/scans/1234")
          .headers(setAuthorisation(roles = listOf(ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO)))
          .exchange()
          .expectErrorResponse(
            userMessageContains = "Parameter scanId must be of type java.util.UUID",
            developerMessageContains = "Failed to convert value",
          )
        verifyNoInteractions(scanService)
      }
    }
  }
}
