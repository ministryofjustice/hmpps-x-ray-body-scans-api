package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.scan.resource

import jakarta.validation.ValidationException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.DownstreamServiceException
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request.CreateScanCaseNoteRequest

@DisplayName("Scan case note resource")
class ScanCaseNoteResourceIntTest(
  @Value("\${scan.annual-limit}") scanAnnualLimit: Int,
  @Value("\${scan.nearing-limit-threshold}") nearingLimitThreshold: Int,
) : BaseScanResourceIntTest(scanAnnualLimit, nearingLimitThreshold) {

  private val prisonerNumber = "A1234BC"
  private val uri = "/prisoner/$prisonerNumber/scan/$scanId/case-note"

  @Nested
  @DisplayName("Happy paths")
  inner class HappyPath {

    @Test
    fun `returns 201 when case note is created successfully`() {
      whenever(scanService.createCaseNote(eq(prisonerNumber), eq(scanId), any())).then { }

      webTestClient.post()
        .uri(uri)
        .headers(setAuthorisation(roles = listOf(ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW)))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(CreateScanCaseNoteRequest(text = "some text"))
        .exchange()
        .expectStatus().isCreated

      verify(scanService).createCaseNote(
        eq(prisonerNumber),
        eq(scanId),
        eq(CreateScanCaseNoteRequest(text = "some text")),
      )
    }
  }

  @Nested
  @DisplayName("Sad paths")
  inner class SadPath {

    @Test
    fun `returns 404 when scan is not found`() {
      doThrow(ValidationException("Scan with id $scanId not found"))
        .whenever(scanService).createCaseNote(eq(prisonerNumber), eq(scanId), any())

      webTestClient.post()
        .uri(uri)
        .headers(setAuthorisation(roles = listOf(ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW)))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(CreateScanCaseNoteRequest(text = "some text"))
        .exchange()
        .expectErrorResponse(
          userMessageContains = "Validation failure",
          developerMessageContains = "Scan with id $scanId not found",
        )
    }

    @Test
    fun `returns 500 when case note creation fails downstream`() {
      doThrow(DownstreamServiceException("Case Notes API create case note request failed", RuntimeException("connection refused")))
        .whenever(scanService).createCaseNote(eq(prisonerNumber), eq(scanId), any())

      webTestClient.post()
        .uri(uri)
        .headers(setAuthorisation(roles = listOf(ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW)))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(CreateScanCaseNoteRequest(text = "some text"))
        .exchange()
        .expectStatus().is5xxServerError
    }

    @Test
    fun `returns 400 when text is blank`() {
      webTestClient.post()
        .uri(uri)
        .headers(setAuthorisation(roles = listOf(ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW)))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(CreateScanCaseNoteRequest(text = ""))
        .exchange()
        .expectErrorResponse(
          userMessageContains = "Validation failure",
          developerMessageContains = "must not be blank",
        )

      verifyNoInteractions(scanService)
    }

    @Test
    fun `returns 400 when body is missing`() {
      webTestClient.post()
        .uri(uri)
        .headers(setAuthorisation(roles = listOf(ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW)))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange()
        .expectErrorResponse(
          userMessageContains = "Malformed request body",
          developerMessageContains = "Required request body is missing",
        )

      verifyNoInteractions(scanService)
    }

    @Test
    fun `returns 400 when prisonerNumber is malformed`() {
      webTestClient.post()
        .uri("/prisoner/RUBBISH/scan/$scanId/case-note")
        .headers(setAuthorisation(roles = listOf(ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW)))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(CreateScanCaseNoteRequest(text = "some text"))
        .exchange()
        .expectErrorResponse(
          userMessageContains = "Validation failure",
          developerMessageContains = "prisonerNumber must be in the right form, e.g. A1234BC.",
        )

      verifyNoInteractions(scanService)
    }

    @DisplayName("endpoint is protected")
    @TestFactory
    fun `endpoint is protected`() = endpointIsProtected(
      webTestClient.post()
        .uri(uri)
        .bodyValue(CreateScanCaseNoteRequest(text = "some text")),
      requiresWriteRole = true,
      afterEach = {
        verifyNoInteractions(scanService)
      },
    )
  }
}
