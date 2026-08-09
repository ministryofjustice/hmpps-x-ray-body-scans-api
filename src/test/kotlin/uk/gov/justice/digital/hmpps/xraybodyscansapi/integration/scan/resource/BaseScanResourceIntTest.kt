package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.scan.resource

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.FixedClock
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.FixedClockConfiguration
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.AlertResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.LegacyScanResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanSummaryResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.service.ScanService
import java.time.LocalDate
import java.util.UUID

@Import(FixedClockConfiguration::class)
abstract class BaseScanResourceIntTest(
  @Value($$"${scan.annual-limit}") protected val scanAnnualLimit: Int,
  @Value($$"${scan.nearing-limit-threshold}") protected val nearingLimitThreshold: Int,
) : IntegrationTestBase() {
  companion object : FixedClock()

  @MockitoBean
  protected lateinit var scanService: ScanService

  protected val scanId: UUID = UUID.fromString("019fc832-57b9-704f-a907-8059720e37e8")
  protected val scanDate: LocalDate = today.minusDays(1)

  protected fun dpsScanResponse(
    originalId: UUID = scanId,
    prisonerNumber: String,
    prisonId: String = "MDI",
    scanDate: LocalDate = this.scanDate,
    justification: String = "INTELLIGENCE",
    outcome: String = "NEGATIVE",
    typeOfFind: String? = null,
    createdBy: String = "abc12ab",
  ) = ScanResponse(
    originalId = originalId,
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

  protected val legacyId: Long = 13134

  protected fun nomisScanResponse(
    originalId: Long = legacyId,
    prisonerNumber: String,
    scanDate: LocalDate = this.scanDate,
    scanDetails: String? = "object detected",
  ) = LegacyScanResponse(
    originalId = originalId,
    prisonerNumber = prisonerNumber,
    scanDate = scanDate,
    scanDetails = scanDetails,
  )

  protected fun summaryResponse(
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
    relevantAlerts: List<AlertResponse>? = null,
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
    relevantAlerts = relevantAlerts,
    fromScanDate = yearStart,
    toScanDate = today,
  )

  protected fun alertResponse(code: String = "XIS", codeDescription: String = "Internal Secretor") = AlertResponse(
    id = "019fcc21-8aaf-75a8-9c27-ec1e006fe35e",
    type = "X",
    typeDescription = "Security",
    code = code,
    codeDescription = codeDescription,
  )
}
