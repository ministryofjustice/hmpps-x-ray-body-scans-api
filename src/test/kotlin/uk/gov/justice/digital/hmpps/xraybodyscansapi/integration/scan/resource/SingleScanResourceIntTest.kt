package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.scan.resource

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.mockito.kotlin.any
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.test.json.JsonCompareMode
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.ADMIN_ROLE
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.READ_ROLE
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.WRITE_ROLE

@DisplayName("Single x-ray body scan resource")
class SingleScanResourceIntTest(
  @Value($$"${scan.annual-limit}") scanAnnualLimit: Int,
  @Value($$"${scan.nearing-limit-threshold}") nearingLimitThreshold: Int,
) : BaseScanResourceIntTest(scanAnnualLimit, nearingLimitThreshold) {
  @Nested
  @DisplayName("Retrieving a single scan by id")
  inner class Get {
    @TestFactory
    @DisplayName("Endpoint is protected")
    fun `endpoint is protected`() = endpointIsProtected(
      webTestClient.get()
        .uri("/scan/$scanId"),
      authorisedRoles = setOf(READ_ROLE, WRITE_ROLE, ADMIN_ROLE),
      setupSuccess = {
        whenever(scanService.getScans(any()))
          .thenReturn(listOf(dpsScanResponse(scanId, "A1234BC")))
      },
      verifyFailure = {
        verifyNoInteractions(scanService)
      },
    )

    @Nested
    @DisplayName("Happy paths")
    inner class HappyPath {
      @Test
      fun `returns a scan when one is found`() {
        whenever(scanService.getScans(listOf(scanId)))
          .thenReturn(listOf(dpsScanResponse(scanId, "A1234BC")))

        webTestClient.get()
          .uri("/scan/$scanId")
          .headers(setAuthorisation(roles = listOf(READ_ROLE)))
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
    }

    @Nested
    @DisplayName("Sad paths")
    inner class SadPath {
      @Test
      fun `returns 400 when id is not a UUID`() {
        webTestClient.get()
          .uri("/scan/1234")
          .headers(setAuthorisation(roles = listOf(READ_ROLE)))
          .exchange()
          .expectErrorResponse(
            userMessageContains = "Parameter scanId must be of type java.util.UUID",
            developerMessageContains = "Failed to convert value",
          )
        verifyNoInteractions(scanService)
      }

      @Test
      fun `returns 404 when no scan is found`() {
        whenever(scanService.getScans(listOf(scanId)))
          .thenReturn(emptyList())

        webTestClient.get()
          .uri("/scan/$scanId")
          .headers(setAuthorisation(roles = listOf(READ_ROLE)))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }

  @Nested
  @DisplayName("Deleting a single scan by id")
  inner class Delete {
    @TestFactory
    @DisplayName("Endpoint is protected")
    fun `endpoint is protected`() = endpointIsProtected(
      webTestClient.delete()
        .uri("/scan/$scanId?reason=Recorded+in+error"),
      authorisedRoles = setOf(ADMIN_ROLE),
      setupSuccess = {
        whenever(scanService.deleteScans(any(), any()))
          .thenReturn(listOf(dpsScanResponse(scanId, "A1234BC", deleted = now to "Recorded in error")))
      },
      verifyFailure = {
        verifyNoInteractions(scanService)
      },
    )

    @Nested
    @DisplayName("Happy paths")
    inner class HappyPath {
      @Test
      fun `deletes a scan when one is found`() {
        whenever(scanService.deleteScans(listOf(scanId), "Recorded in error"))
          .thenReturn(listOf(dpsScanResponse(scanId, "A1234BC", deleted = now to "Recorded in error")))

        webTestClient.delete()
          .uri("/scan/$scanId?reason=Recorded+in+error")
          .headers(setAuthorisation(roles = listOf(ADMIN_ROLE)))
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
              "lastModifiedBy": "abc12ab",
              "deletedAt": "2026-07-27T09:10:11.123",
              "deletedReason": "Recorded in error"
            }
            """,
            JsonCompareMode.STRICT,
          )
      }
    }

    @Nested
    @DisplayName("Sad paths")
    inner class SadPath {
      @Test
      fun `returns 400 when id is not a UUID`() {
        webTestClient.delete()
          .uri("/scan/1234?reason=Recorded+in+error")
          .headers(setAuthorisation(roles = listOf(ADMIN_ROLE)))
          .exchange()
          .expectErrorResponse(
            userMessageContains = "Parameter scanId must be of type java.util.UUID",
            developerMessageContains = "Failed to convert value",
          )
        verifyNoInteractions(scanService)
      }

      @Test
      fun `returns 400 when reason is missing`() {
        webTestClient.delete()
          .uri("/scan/$scanId")
          .headers(setAuthorisation(roles = listOf(ADMIN_ROLE)))
          .exchange()
          .expectErrorResponse(
            userMessageContains = "Parameter specified as non-null is null",
            developerMessageContains = "Parameter specified as non-null is null",
          )
        verifyNoInteractions(scanService)
      }

      @Test
      fun `returns 400 when reason is blank`() {
        webTestClient.delete()
          .uri("/scan/$scanId?reason=")
          .headers(setAuthorisation(roles = listOf(ADMIN_ROLE)))
          .exchange()
          .expectErrorResponse(
            userMessageContains = "deleteScanRequest.reason: must not be blank",
            developerMessageContains = "deleteScanRequest.reason: must not be blank",
          )
        verifyNoInteractions(scanService)
      }

      @Test
      fun `returns 404 when no scan is found`() {
        whenever(scanService.deleteScans(listOf(scanId), "Recorded in error"))
          .thenReturn(emptyList())

        webTestClient.delete()
          .uri("/scan/$scanId?reason=Recorded+in+error")
          .headers(setAuthorisation(roles = listOf(ADMIN_ROLE)))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }
}
