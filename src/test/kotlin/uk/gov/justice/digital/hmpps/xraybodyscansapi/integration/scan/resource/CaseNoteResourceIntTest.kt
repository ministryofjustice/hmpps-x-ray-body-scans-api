package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.scan.resource

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
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.json.JsonCompareMode
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.ADMIN_ROLE
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.DownstreamServiceException
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.NotFoundException
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.READ_CASE_NOTE_ROLE
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.WRITE_CASE_NOTE_ROLE
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request.CreateScanCaseNoteRequest

@DisplayName("X-ray body scan case note resource")
class CaseNoteResourceIntTest(
  @Value($$"${scan.annual-limit}") scanAnnualLimit: Int,
  @Value($$"${scan.nearing-limit-threshold}") nearingLimitThreshold: Int,
) : BaseScanResourceIntTest(scanAnnualLimit, nearingLimitThreshold) {

  private val uri = "/scan/$scanId/case-note"

  @Nested
  @DisplayName("GET case note")
  inner class GetCaseNote {
    @DisplayName("Endpoint is protected")
    @TestFactory
    fun `endpoint is protected`() = endpointIsProtected(
      webTestClient.get().uri(uri),
      authorisedRoles = setOf(READ_CASE_NOTE_ROLE, WRITE_CASE_NOTE_ROLE, ADMIN_ROLE),
      setupSuccess = {
        whenever(scanService.getScanCaseNote(any()))
          .thenReturn(caseNoteResponse())
      },
      verifyFailure = {
        verifyNoInteractions(scanService)
      },
    )

    @Nested
    @DisplayName("Happy paths")
    inner class HappyPath {
      @Test
      fun `returns 200 and case note details`() {
        whenever(scanService.getScanCaseNote(eq(scanId)))
          .thenReturn(caseNoteResponse())

        webTestClient.get()
          .uri(uri)
          .headers(setAuthorisation(roles = listOf(READ_CASE_NOTE_ROLE)))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json(
            // language=json
            """
            {
              "id": "$caseNoteId",
              "typeDescription": "General",
              "subTypeDescription": "X-ray body scan",
              "createdBy": "Bob Profileman",
              "createdAt": "2026-07-27T09:10:11.123",
              "occurredAt": "2026-07-26T00:00:00",
              "text": "some text",
              "amendments": []
            }
            """,
            JsonCompareMode.STRICT,
          )

        verify(scanService).getScanCaseNote(eq(scanId))
      }

      @Test
      fun `returns 200 and case note details with amendments`() {
        whenever(scanService.getScanCaseNote(eq(scanId)))
          .thenReturn(
            caseNoteResponse(
              createdAt = now.minusHours(3),
              ammendmentDates = listOf(now.minusHours(2), now),
            ),
          )

        webTestClient.get()
          .uri(uri)
          .headers(setAuthorisation(roles = listOf(READ_CASE_NOTE_ROLE)))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json(
            // language=json
            """
            {
              "id": "$caseNoteId",
              "typeDescription": "General",
              "subTypeDescription": "X-ray body scan",
              "createdBy": "Bob Profileman",
              "createdAt": "2026-07-27T06:10:11.123",
              "occurredAt": "2026-07-26T00:00:00",
              "text": "some text",
              "amendments": [
                {
                  "text": "amendment 1",
                  "createdBy": "Another User 1",
                  "createdAt": "2026-07-27T07:10:11.123"
                },
                {
                  "text": "amendment 2",
                  "createdBy": "Another User 2",
                  "createdAt": "2026-07-27T09:10:11.123"
                }
              ]
            }
            """,
            JsonCompareMode.STRICT,
          )

        verify(scanService).getScanCaseNote(eq(scanId))
      }
    }

    @Nested
    @DisplayName("Sad paths")
    inner class SadPath {

      @Test
      fun `returns 404 when scan is not found`() {
        doThrow(NotFoundException("Scan with id $scanId not found"))
          .whenever(scanService).getScanCaseNote(eq(scanId))

        webTestClient.get()
          .uri(uri)
          .headers(setAuthorisation(roles = listOf(READ_CASE_NOTE_ROLE)))
          .exchange()
          .expectErrorResponse(
            status = HttpStatus.NOT_FOUND,
            userMessageContains = "Not found",
            developerMessageContains = "Scan with id $scanId not found",
          )
      }

      @Test
      fun `returns 404 when scan has no associated case note`() {
        doThrow(NotFoundException("Scan with id $scanId has no associated case note"))
          .whenever(scanService).getScanCaseNote(eq(scanId))

        webTestClient.get()
          .uri(uri)
          .headers(setAuthorisation(roles = listOf(READ_CASE_NOTE_ROLE)))
          .exchange()
          .expectErrorResponse(
            status = HttpStatus.NOT_FOUND,
            userMessageContains = "Not found",
            developerMessageContains = "has no associated case note",
          )
      }

      @Test
      fun `returns 500 when case note fetch fails downstream`() {
        doThrow(DownstreamServiceException("Case Notes API get case note request failed", RuntimeException("connection refused")))
          .whenever(scanService).getScanCaseNote(eq(scanId))

        webTestClient.get()
          .uri(uri)
          .headers(setAuthorisation(roles = listOf(READ_CASE_NOTE_ROLE)))
          .exchange()
          .expectStatus().is5xxServerError
      }
    }
  }

  @Nested
  @DisplayName("POST case note")
  inner class CreateCaseNote {
    @DisplayName("Endpoint is protected")
    @TestFactory
    fun `endpoint is protected`() = endpointIsProtected(
      webTestClient.post()
        .uri(uri)
        .bodyValue(CreateScanCaseNoteRequest(text = "some text", prisonId = "MDI")),
      authorisedRoles = setOf(WRITE_CASE_NOTE_ROLE, ADMIN_ROLE),
      setupSuccess = {
        whenever(scanService.createCaseNote(any(), any()))
          .thenReturn(caseNoteResponse())
      },
      verifyFailure = {
        verifyNoInteractions(scanService)
      },
    )

    @Nested
    @DisplayName("Happy paths")
    inner class HappyPath {

      @Test
      fun `returns 201 when case note is created successfully`() {
        whenever(scanService.createCaseNote(eq(scanId), any()))
          .thenReturn(caseNoteResponse())

        webTestClient.post()
          .uri(uri)
          .headers(setAuthorisation(roles = listOf(WRITE_CASE_NOTE_ROLE)))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(CreateScanCaseNoteRequest(text = "some text", prisonId = "MDI"))
          .exchange()
          .expectStatus().isCreated
          .expectBody()
          .json(
            // language=json
            """
            {
              "id": "$caseNoteId",
              "typeDescription": "General",
              "subTypeDescription": "X-ray body scan",
              "createdBy": "Bob Profileman",
              "createdAt": "2026-07-27T09:10:11.123",
              "occurredAt": "2026-07-26T00:00:00",
              "text": "some text",
              "amendments": []
            }
            """,
            JsonCompareMode.STRICT,
          )

        verify(scanService).createCaseNote(
          eq(scanId),
          eq(CreateScanCaseNoteRequest(text = "some text", prisonId = "MDI")),
        )
      }
    }

    @Nested
    @DisplayName("Sad paths")
    inner class SadPath {

      @Test
      fun `returns 404 when scan is not found`() {
        doThrow(NotFoundException("Scan with id $scanId not found"))
          .whenever(scanService).createCaseNote(eq(scanId), any())

        webTestClient.post()
          .uri(uri)
          .headers(setAuthorisation(roles = listOf(WRITE_CASE_NOTE_ROLE)))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(CreateScanCaseNoteRequest(text = "some text", prisonId = "MDI"))
          .exchange()
          .expectErrorResponse(
            status = HttpStatus.NOT_FOUND,
            userMessageContains = "Not found",
            developerMessageContains = "Scan with id $scanId not found",
          )
      }

      @Test
      fun `returns 500 when case note creation fails downstream`() {
        doThrow(DownstreamServiceException("Case Notes API create case note request failed", RuntimeException("connection refused")))
          .whenever(scanService).createCaseNote(eq(scanId), any())

        webTestClient.post()
          .uri(uri)
          .headers(setAuthorisation(roles = listOf(WRITE_CASE_NOTE_ROLE)))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(CreateScanCaseNoteRequest(text = "some text", prisonId = "MDI"))
          .exchange()
          .expectStatus().is5xxServerError
      }

      @Test
      fun `returns 400 when text is blank`() {
        webTestClient.post()
          .uri(uri)
          .headers(setAuthorisation(roles = listOf(WRITE_CASE_NOTE_ROLE)))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(CreateScanCaseNoteRequest(text = "", prisonId = "MDI"))
          .exchange()
          .expectErrorResponse(
            userMessageContains = "Validation failure",
            developerMessageContains = "must not be blank",
          )

        verifyNoInteractions(scanService)
      }

      @Test
      fun `returns 400 when prison ID is blank`() {
        webTestClient.post()
          .uri(uri)
          .headers(setAuthorisation(roles = listOf(WRITE_CASE_NOTE_ROLE)))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(CreateScanCaseNoteRequest(text = "some text", prisonId = ""))
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
          .headers(setAuthorisation(roles = listOf(WRITE_CASE_NOTE_ROLE)))
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
