package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.scan.resource

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.json.JsonCompareMode
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO
import uk.gov.justice.digital.hmpps.xraybodyscansapi.config.ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.FixedClock
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.FixedClockConfiguration
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request.CreateScanRequest
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request.ListScansRequest
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanSummaryResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.service.ScanService
import java.time.LocalDate
import java.util.UUID

@DisplayName("X-ray body scans resource")
@Import(FixedClockConfiguration::class)
class ScanResourceIntTest(
  @Value($$"${scan.annual-limit}") private val scanAnnualLimit: Int,
  @Value($$"${scan.nearing-limit-threshold}") private val nearingLimitThreshold: Int,
) : IntegrationTestBase() {
  companion object : FixedClock()

  @MockitoBean
  private lateinit var scanService: ScanService

  private val prisonerNumber = "A1234BC"
  private val scanDate: LocalDate = today.minusDays(1)
  private val id: UUID = UUID.randomUUID()

  @Nested
  @DisplayName("List scans endpoint")
  inner class ListScans {
    @Nested
    @DisplayName("Happy paths")
    inner class HappyPath {
      @Test
      fun `returns a page of scans`() {
        val scanIds = listOf(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
        val scanDate = today.minusDays(5)
        whenever(scanService.listScans(prisonerNumber, ListScansRequest(), PageRequest.of(0, 20, Sort.by("scanDate").descending()))).thenReturn(
          PageImpl(
            scanIds.mapIndexed { index, id ->
              scanResponse(
                id = id,
                prisonerNumber = prisonerNumber,
                scanDate = scanDate.plusDays(index.toLong()),
              )
            },
          ),
        )

        webTestClient.get()
          .uri("/prisoner/$prisonerNumber/scan")
          .headers(setAuthorisation(roles = listOf(ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO)))
          .exchange()
          .expectStatus().isOk
          .expectHeader().contentType(MediaType.APPLICATION_JSON)
          .expectBody()
          .json(
            // language=json
            """
            {
              "content": [
                {"id": "${scanIds[0]}", "prisonerNumber": "A1234BC"},
                {"id": "${scanIds[1]}", "prisonerNumber": "A1234BC"},
                {"id": "${scanIds[2]}", "prisonerNumber": "A1234BC"}
              ]
            }
            """,
            JsonCompareMode.LENIENT,
          )
      }

      @Test
      fun `returns a page of scans filtered by date`() {
        whenever(
          scanService.listScans(
            prisonerNumber,
            ListScansRequest(
              fromScanDate = LocalDate.of(2026, 1, 1),
              toScanDate = LocalDate.of(2026, 6, 26),
            ),
            PageRequest.of(0, 20, Sort.by("scanDate").descending()),
          ),
        ).thenReturn(
          PageImpl(
            listOf(
              scanResponse(
                id = id,
                prisonerNumber = prisonerNumber,
                scanDate = LocalDate.of(2026, 5, 6),
              ),
            ),
          ),
        )

        webTestClient.get()
          .uri("/prisoner/$prisonerNumber/scan?fromScanDate=2026-01-01&toScanDate=2026-06-26")
          .headers(setAuthorisation(roles = listOf(ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO)))
          .exchange()
          .expectStatus().isOk
          .expectHeader().contentType(MediaType.APPLICATION_JSON)
          .expectBody()
          .json(
            // language=json
            """
            {
              "content": [
                {"id": "$id", "prisonerNumber": "A1234BC"}
              ]
            }
            """,
            JsonCompareMode.LENIENT,
          )
      }

      @Test
      fun `returns pages of scans as specified`() {
        val pageable = PageRequest.of(2, 100, Sort.by("id").ascending())
        whenever(
          scanService.listScans(
            prisonerNumber,
            ListScansRequest(
              fromScanDate = LocalDate.of(2025, 1, 1),
              toScanDate = LocalDate.of(2025, 12, 31),
            ),
            pageable,
          ),
        ).thenReturn(PageImpl(emptyList(), pageable, 110))

        webTestClient.get()
          .uri("/prisoner/$prisonerNumber/scan?fromScanDate=2025-01-01&toScanDate=2025-12-31&size=100&page=2&sort=id,ASC")
          .headers(setAuthorisation(roles = listOf(ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO)))
          .exchange()
          .expectStatus().isOk
          .expectHeader().contentType(MediaType.APPLICATION_JSON)
          .expectBody()
          .json(
            // language=json
            """
            {
              "content": [],
              "size": 100,
              "number": 2,
              "numberOfElements": 0,
              "totalElements": 110,
              "totalPages": 2
            }
            """,
            JsonCompareMode.LENIENT,
          )
      }
    }

    @Nested
    @DisplayName("Sad paths")
    inner class SadPath {
      @DisplayName("endpoint is protected")
      @TestFactory
      fun `endpoint is protected`() = endpointIsProtected(
        webTestClient.get()
          .uri("/prisoner/$prisonerNumber/scan"),
        afterEach = {
          verifyNoInteractions(scanService)
        },
      )

      @ParameterizedTest(name = "returns 400 for bad requests: {1}")
      @CsvSource(
        value = [
          "fromScanDate=this-year | listScansRequest.fromScanDate",
          "toScanDate=null        | listScansRequest.toScanDate",
        ],
        delimiter = '|',
      )
      fun `returns 400 for bad requests`(queryParameters: String, expectedMessage: String) {
        webTestClient.get()
          .uri("/prisoner/$prisonerNumber/scan?$queryParameters")
          .headers(setAuthorisation(roles = listOf(ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO)))
          .exchange()
          .expectErrorResponse(
            userMessageContains = "Validation failure",
            developerMessageContains = expectedMessage,
          )

        verifyNoInteractions(scanService)
      }
    }
  }

  @Nested
  @DisplayName("Create a scan endpoint")
  inner class CreateScan {

    @Nested
    @DisplayName("Happy paths")
    inner class HappyPath {

      @Test
      fun `returns 201 and created scan when request is valid`() {
        val request = createScanRequest(scanDate = scanDate)
        whenever(scanService.createScan(eq(prisonerNumber), any()))
          .thenReturn(
            scanResponse(
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
          .jsonPath("$.id").isEqualTo(id)
          .jsonPath("$.prisonerNumber").isEqualTo(prisonerNumber)
          .jsonPath("$.scanDate").isEqualTo(scanDate.toString())

        verify(scanService).createScan(eq(prisonerNumber), eq(request))
      }

      @Test
      fun `returns 201 when scanDate is today`() {
        val request = createScanRequest(scanDate = today)
        whenever(scanService.createScan(eq(prisonerNumber), any()))
          .thenReturn(
            scanResponse(
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
    @DisplayName("Sad paths")
    inner class SadPath {

      @Test
      fun `returns 400 when the scanDate is in the future`() {
        val futureDate = LocalDate.now().plusDays(1)

        webTestClient.post()
          .uri("/prisoner/$prisonerNumber/scan")
          .headers(setAuthorisation(roles = listOf(ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW)))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(createScanRequest(scanDate = futureDate))
          .exchange()
          .expectErrorResponse(
            userMessageContains = "Validation failure",
            developerMessageContains = "scanDate must be today or in the past",
          )

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
          .expectErrorResponse(
            userMessageContains = "Malformed request body",
            developerMessageContains = "Failed to deserialize `java.time.LocalDate`",
          )

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
          .expectErrorResponse(
            userMessageContains = "Malformed request body",
            developerMessageContains = "JSON property scanDate",
          )

        verifyNoInteractions(scanService)
      }

      @Test
      fun `returns 400 when the prisonerNumber is malformed or missing`() {
        webTestClient.post()
          .uri("/prisoner/RUBBISH/scan")
          .headers(setAuthorisation(roles = listOf(ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW)))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(createScanRequest(scanDate = scanDate))
          .exchange()
          .expectErrorResponse(
            userMessageContains = "Validation failure",
            developerMessageContains = "prisonerNumber must be in the right form, e.g. A1234BC.",
          )

        verifyNoInteractions(scanService)
      }

      @Test
      fun `returns 400 when the body is missing`() {
        webTestClient.post()
          .uri("/prisoner/$prisonerNumber/scan")
          .headers(setAuthorisation(roles = listOf(ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW)))
          .contentType(MediaType.APPLICATION_JSON)
          .exchange()
          .expectErrorResponse(
            userMessageContains = "Malformed request body",
            developerMessageContains = "Required request body is missing",
          )

        verifyNoInteractions(scanService)
      }

      @DisplayName("endpoint is protected")
      @TestFactory
      fun `endpoint is protected`() = endpointIsProtected(
        webTestClient.post()
          .uri("/prisoner/$prisonerNumber/scan")
          .bodyValue(createScanRequest(scanDate = scanDate)),
        requiresWriteRole = true,
        afterEach = {
          verifyNoInteractions(scanService)
        },
      )
    }

    private fun createScanRequest(
      scanDate: LocalDate,
      prisonId: String = "MDI",
      justification: String = "INTELLIGENCE",
      outcome: String = "NEGATIVE",
      typeOfFind: String? = null,
      createdBy: String = "abc12ab",
    ) = CreateScanRequest(
      scanDate = scanDate,
      prisonId = prisonId,
      justification = justification,
      outcome = outcome,
      typeOfFind = typeOfFind,
      createdBy = createdBy,
    )
  }

  @Nested
  @DisplayName("Get scan summary endpoint")
  inner class SummariseScans {
    @Nested
    @DisplayName("Happy paths")
    inner class HappyPath {
      @Test
      fun `returns scan summary for this calendar year`() {
        whenever(scanService.summariseScans(prisonerNumber))
          .thenReturn(
            summaryResponse(
              prisonerNumber = prisonerNumber,
              nomisCount = 4,
              dpsCount = 2,
              positiveCount = 1,
              negativeCount = 1,
              inconclusiveCount = 1,
            ),
          )

        webTestClient.get()
          .uri("/prisoner/$prisonerNumber/scan/summary")
          .headers(setAuthorisation(roles = listOf(ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO)))
          .exchange()
          .expectStatus().isOk
          .expectBody().json(
            // language=json
            """
            {
              "prisonerNumber": "$prisonerNumber",
              "nomisCount": 4,
              "dpsCount": 2,
              "totalCount": 6,
              "positiveCount": 1,
              "negativeCount": 1,
              "inconclusiveCount": 1,
              "annualLimit": 116,
              "remainingScans": 110,
              "nearingScanLimit": false,
              "relevantAlerts": null,
              "atScanLimit": false,
              "fromScanDate": "2026-01-01",
              "toScanDate": "2026-07-27"
            }
            """,
            JsonCompareMode.STRICT,
          )
      }

      @ParameterizedTest(name = "permits role {0}")
      @ValueSource(
        strings = [
          ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO,
          ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RW,
        ],
      )
      fun `permits role`(role: String) {
        whenever(scanService.summariseScans(prisonerNumber))
          .thenReturn(
            summaryResponse(
              prisonerNumber = prisonerNumber,
              nomisCount = 4,
              dpsCount = 2,
              negativeCount = 2,
            ),
          )

        webTestClient.get()
          .uri("/prisoner/$prisonerNumber/scan/summary")
          .headers(setAuthorisation(roles = listOf(role)))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    @DisplayName("Sad paths")
    inner class SadPath {
      @DisplayName("endpoint is protected")
      @TestFactory
      fun `endpoint is protected`() = endpointIsProtected(
        webTestClient.get()
          .uri("/prisoner/$prisonerNumber/scan/summary"),
        afterEach = {
          verifyNoInteractions(scanService)
        },
      )

      @Test
      fun `returns 400 when the prisonerNumber is malformed or missing`() {
        webTestClient.get()
          .uri("/prisoner/RUBBISH/scan/summary")
          .headers(setAuthorisation(roles = listOf(ROLE_X_RAY_BODY_SCANS_API__SCAN_DATA__RO)))
          .exchange()
          .expectErrorResponse(
            userMessageContains = "Validation failure",
            developerMessageContains = "prisonerNumber must be in the right form, e.g. A1234BC.",
          )

        verifyNoInteractions(scanService)
      }
    }
  }

  private fun scanResponse(
    id: UUID = UUID.randomUUID(),
    prisonerNumber: String,
    prisonId: String = "MDI",
    scanDate: LocalDate = today.minusDays(1),
    justification: String = "INTELLIGENCE",
    outcome: String = "NEGATIVE",
    typeOfFind: String? = null,
    createdBy: String = "abc12ab",
  ) = ScanResponse(
    id = id,
    prisonerNumber = prisonerNumber,
    prisonId = prisonId,
    scanDate = scanDate,
    justification = justification,
    justificationDescription = justification,
    outcome = outcome,
    outcomeDescription = outcome,
    typeOfFind = typeOfFind,
    typeOfFindDescription = typeOfFind,
    createdAt = now,
    createdBy = createdBy,
    lastModifiedAt = now,
    lastModifiedBy = createdBy,
  )

  private fun summaryResponse(
    prisonerNumber: String,
    nomisCount: Int,
    dpsCount: Int,
    totalCount: Int = nomisCount + dpsCount,
    positiveCount: Int = 0,
    negativeCount: Int = 0,
    inconclusiveCount: Int = 0,
    remainingScans: Int = scanAnnualLimit - totalCount,
    nearingScanLimit: Boolean = totalCount >= nearingLimitThreshold,
    atScanLimit: Boolean = remainingScans <= 0,
  ) = ScanSummaryResponse(
    prisonerNumber = prisonerNumber,
    nomisCount = nomisCount,
    dpsCount = dpsCount,
    totalCount = totalCount,
    positiveCount = positiveCount,
    negativeCount = negativeCount,
    inconclusiveCount = inconclusiveCount,
    remainingScans = remainingScans,
    annualLimit = scanAnnualLimit,
    nearingScanLimit = nearingScanLimit,
    atScanLimit = atScanLimit,
    fromScanDate = yearStart,
    toScanDate = today,
  )
}
