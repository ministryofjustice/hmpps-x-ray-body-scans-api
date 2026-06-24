package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.scan.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request.CreateScanRequest
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.service.ScanService
import java.time.LocalDate

class ScanResourceIntTest : IntegrationTestBase() {

  @MockitoBean
  private lateinit var scanService: ScanService

  private val prisonerNumber = "A1234BC"
  private val scanDate: LocalDate = LocalDate.now().minusDays(1)
  private val id: Long = 1234L

  @Nested
  inner class CreateScan {

    @Nested
    inner class HappyPath {

      @Test
      fun `returns 201 and created scan when request is valid`() {
        val expectedId = 1234L
        val request = CreateScanRequest(scanDate = scanDate)
        whenever(scanService.createScan(eq(prisonerNumber), any()))
          .thenReturn(
            ScanResponse(
              id = id,
              prisonerNumber = prisonerNumber,
              scanDate = scanDate,
            ),
          )

        webTestClient.post()
          .uri("/prisoner/$prisonerNumber/scan")
          .headers(setAuthorisation(roles = listOf(ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW)))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus().isCreated
          .expectHeader().contentType(MediaType.APPLICATION_JSON)
          .expectBody()
          .jsonPath("$.id").isEqualTo(expectedId.toString())
          .jsonPath("$.prisonerNumber").isEqualTo(prisonerNumber)
          .jsonPath("$.scanDate").isEqualTo(scanDate.toString())

        verify(scanService).createScan(eq(prisonerNumber), eq(request))
      }

      @Test
      fun `returns 201 when scanDate is today`() {
        val today = LocalDate.now()
        val request = CreateScanRequest(scanDate = today)
        whenever(scanService.createScan(eq(prisonerNumber), any()))
          .thenReturn(
            ScanResponse(
              id = id,
              prisonerNumber = prisonerNumber,
              scanDate = today,
            ),
          )

        webTestClient.post()
          .uri("/prisoner/$prisonerNumber/scan")
          .headers(setAuthorisation(roles = listOf(ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW)))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus().isCreated

        verify(scanService).createScan(eq(prisonerNumber), eq(request))
      }
    }

    @Nested
    inner class SadPath {

      @Test
      fun `returns 400 when the scanDate is in the future`() {
        val futureDate = LocalDate.now().plusDays(1)

        webTestClient.post()
          .uri("/prisoner/$prisonerNumber/scan")
          .headers(setAuthorisation(roles = listOf(ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW)))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(CreateScanRequest(scanDate = futureDate))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("$.userMessage").value<String> {
            assertThat(it).isEqualTo("Validation failure")
          }
          .jsonPath("$.developerMessage").value<String> {
            assertThat(it).contains("scanDate must be today or in the past")
          }

        verifyNoInteractions(scanService)
      }

      @Test
      fun `returns 400 when the scanDate is malformed`() {
        webTestClient.post()
          .uri("/prisoner/$prisonerNumber/scan")
          .headers(setAuthorisation(roles = listOf(ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW)))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue("""{"scanDate":"not-a-date"}""")
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("Malformed request body")
          }
          .jsonPath("$.developerMessage").value<String> {
            assertThat(it).contains("Failed to deserialize `java.time.LocalDate")
          }

        verifyNoInteractions(scanService)
      }

      @Test
      fun `returns 400 when the scanDate is missing`() {
        webTestClient.post()
          .uri("/prisoner/$prisonerNumber/scan")
          .headers(setAuthorisation(roles = listOf(ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW)))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue("{}")
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("Malformed request body")
          }
          .jsonPath("$.developerMessage").value<String> {
            assertThat(it).contains("JSON property scanDate")
            assertThat(it).contains("missing")
          }

        verifyNoInteractions(scanService)
      }

      @Test
      fun `returns 400 when the prisonerNumber is malformed or missing`() {
        webTestClient.post()
          .uri("/prisoner/RUBBISH/scan")
          .headers(setAuthorisation(roles = listOf(ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW)))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(CreateScanRequest(scanDate = scanDate))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("Validation failure")
          }
          .jsonPath("$.developerMessage").value<String> {
            assertThat(it).isEqualTo("prisonerNumber must be in the right form, e.g. A1234BC.")
          }

        verifyNoInteractions(scanService)
      }

      @Test
      fun `returns 400 when the body is missing`() {
        webTestClient.post()
          .uri("/prisoner/$prisonerNumber/scan")
          .headers(setAuthorisation(roles = listOf(ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW)))
          .contentType(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("Malformed request body")
          }
          .jsonPath("$.developerMessage").value<String> {
            assertThat(it).contains("Required request body is missing")
          }

        verifyNoInteractions(scanService)
      }

      @Test
      fun `returns 401 when the token is missing`() {
        webTestClient.post()
          .uri("/prisoner/$prisonerNumber/scan")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(CreateScanRequest(scanDate = scanDate))
          .exchange()
          .expectStatus().isUnauthorized

        verifyNoInteractions(scanService)
      }

      @Test
      fun `returns 403 when the token doesn't have the right role`() {
        webTestClient.post()
          .uri("/prisoner/$prisonerNumber/scan")
          .headers(setAuthorisation(roles = listOf("ROLE_WHATEVER")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(CreateScanRequest(scanDate = scanDate))
          .exchange()
          .expectStatus().isForbidden

        verifyNoInteractions(scanService)
      }

      @Test
      fun `returns 403 when the token has no role`() {
        webTestClient.post()
          .uri("/prisoner/$prisonerNumber/scan")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(CreateScanRequest(scanDate = scanDate))
          .exchange()
          .expectStatus().isForbidden

        verifyNoInteractions(scanService)
      }
    }
  }
}
