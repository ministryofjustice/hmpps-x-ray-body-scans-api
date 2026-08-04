package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.scan.resource

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.FixedClock
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.FixedClockConfiguration
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.AlertResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanSummaryResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.service.ScanService

@Import(FixedClockConfiguration::class)
abstract class BaseScanResourceIntTest(
  @Value($$"${scan.annual-limit}") protected val scanAnnualLimit: Int,
  @Value($$"${scan.nearing-limit-threshold}") protected val nearingLimitThreshold: Int,
) : IntegrationTestBase() {
  companion object : FixedClock()

  @MockitoBean
  protected lateinit var scanService: ScanService

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
