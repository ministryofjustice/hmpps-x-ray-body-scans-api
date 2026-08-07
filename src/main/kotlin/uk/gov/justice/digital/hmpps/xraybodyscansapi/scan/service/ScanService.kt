package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.service

import jakarta.validation.ValidationException
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.alertsapi.AlertsApiClient
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.dto.response.ReferenceDataDomains
import uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.repository.ReferenceDataCodeEntity
import uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.repository.ReferenceDataCodeRepository
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request.CreateScanRequest
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request.ListScansRequest
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.AlertResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanSummaryResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.UnifiedScanResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.ScanEntity
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.ScanRepository
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.filterByPrisonerNumber
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.groupOutcomes
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters.firstDayOfYear

@Service
class ScanService(
  private val clock: Clock,
  private val codeRepository: ReferenceDataCodeRepository,
  private val scanRepository: ScanRepository,
  private val prisonApiClient: PrisonApiClient,
  private val alertsApiClient: AlertsApiClient,
  @Value($$"${scan.annual-limit}") private val scanAnnualLimit: Int,
  @Value($$"${scan.nearing-limit-threshold}") private val nearingLimitThreshold: Int,
  @Value($$"${scan.relevant-alert-codes:}") private val relevantAlertCodes: Set<String>,
) {
  @Transactional(readOnly = true)
  fun listScans(
    prisonerNumber: String,
    query: ListScansRequest? = null,
    pageable: Pageable = PageRequest.of(0, 20, Sort.by("scanDate").descending()),
  ): Page<out UnifiedScanResponse> {
    var specification = filterByPrisonerNumber(prisonerNumber)
    query?.let {
      specification = specification.and(query.toSpecification())
    }
    // TODO: impose limits on page request, eg max size or available sort columns?
    return scanRepository.findAll(specification, pageable).map {
      it.toDto()
    }
  }

  @Transactional
  fun createScan(prisonerNumber: String, request: CreateScanRequest): ScanResponse {
    val justification = findReferenceDataOrThrowValidationError(ReferenceDataDomains.JUSTIFICATION, request.justification)
    val outcome = findReferenceDataOrThrowValidationError(ReferenceDataDomains.OUTCOME, request.outcome)
    val typeOfFind = request.typeOfFind?.let {
      findReferenceDataOrThrowValidationError(ReferenceDataDomains.TYPE_OF_FIND, it)
    }
    if (outcome.code == "POSITIVE" && typeOfFind == null) {
      throw ValidationException("typeOfFind is required for positive outcomes")
    }
    val saved = scanRepository.save(
      ScanEntity(
        prisonerNumber = prisonerNumber,
        prisonId = request.prisonId,
        scanDate = request.scanDate,
        justification = justification,
        outcome = outcome,
        typeOfFind = typeOfFind,
        createdBy = request.createdBy,
      ),
    )

    return saved.toDto()
  }

  @Transactional(readOnly = true)
  fun summariseScans(prisonerNumber: String, includeAlerts: Boolean = false): ScanSummaryResponse = summariseScans(listOf(prisonerNumber), includeAlerts).first()

  @Transactional(readOnly = true)
  fun summariseScans(prisonerNumbers: List<String>, includeAlerts: Boolean = false): List<ScanSummaryResponse> {
    val (fromScanDate, toScanDate) = calendarYear()
    val nomisCounts = getNomisScanCounts(prisonerNumbers, fromScanDate, toScanDate)
    val dpsCounts = scanRepository.scanSummaryRowsForPrisoners(prisonerNumbers, fromScanDate, toScanDate).groupOutcomes()

    val relevantAlerts = if (includeAlerts) {
      getRelevantAlerts(prisonerNumbers)
    } else {
      null
    }

    return prisonerNumbers.map { prisonerNumber ->
      val nomisCount = nomisCounts[prisonerNumber] ?: 0
      val dpsOutcomes = dpsCounts[prisonerNumber] ?: emptyMap()
      val dpsCount = dpsOutcomes.values.sum()
      val totalCount = nomisCount + dpsCount
      val remainingScans = scanAnnualLimit - totalCount
      val nearingScanLimit = totalCount >= nearingLimitThreshold
      val atScanLimit = remainingScans <= 0
      ScanSummaryResponse(
        prisonerNumber = prisonerNumber,
        nomisCount = nomisCount,
        dpsCount = dpsCount,
        totalCount = totalCount,
        positiveCount = dpsOutcomes.getOrDefault("POSITIVE", 0),
        negativeCount = dpsOutcomes.getOrDefault("NEGATIVE", 0),
        inconclusiveCount = dpsOutcomes.getOrDefault("INCONCLUSIVE", 0),
        annualLimit = scanAnnualLimit,
        remainingScans = remainingScans,
        nearingScanLimit = nearingScanLimit,
        atScanLimit = atScanLimit,
        relevantAlerts = if (relevantAlerts != null) {
          relevantAlerts[prisonerNumber] ?: emptyList()
        } else {
          null
        },
        fromScanDate = fromScanDate,
        toScanDate = toScanDate,
      )
    }
  }

  private fun calendarYear(): Pair<LocalDate, LocalDate> {
    val today = LocalDate.now(clock)
    val startOfYear = today.with(firstDayOfYear())
    return startOfYear to today
  }

  private fun getNomisScanCounts(
    prisonerNumbers: List<String>,
    fromScanDate: LocalDate,
    toScanDate: LocalDate,
  ): Map<String, Int> = prisonApiClient
    .getScanCareNeeds(prisonerNumbers)
    .associate { res ->
      res.offenderNo to res.personalCareNeeds.count { bscan ->
        bscan.startDate != null && !bscan.startDate.isBefore(fromScanDate) && !bscan.startDate.isAfter(toScanDate)
      }
    }

  private fun findReferenceDataOrThrowValidationError(
    domain: ReferenceDataDomains,
    code: String,
  ): ReferenceDataCodeEntity = codeRepository.findByDomainAndCode(domain, code)
    ?: throw ValidationException("Reference data with domain ${domain.name} and code $code not found")

  private fun ScanEntity.toDto(): ScanResponse = ScanResponse(
    originalId = id,
    prisonerNumber = prisonerNumber,
    prisonId = prisonId,
    scanDate = scanDate,
    justification = justification.code,
    justificationDescription = justification.description,
    outcome = outcome.code,
    outcomeDescription = outcome.description,
    typeOfFind = typeOfFind?.code,
    typeOfFindDescription = typeOfFind?.description,
    caseNoteId = caseNoteId,
    mergedFromPrisonerNumber = mergedFromPrisonerNumber,
    mergedAt = mergedAt,
    createdAt = createdAt,
    createdBy = createdBy,
    lastModifiedAt = lastModifiedAt,
    lastModifiedBy = lastModifiedBy,
  )

  private fun getRelevantAlerts(prisonerNumbers: List<String>): Map<String, List<AlertResponse>> = alertsApiClient.getAlerts(
    prisonerNumbers = prisonerNumbers,
    filterAlertCodes = relevantAlertCodes,
  )
    .toList()
    // .filter { relevantAlertCodes.contains(it.alertCode.code) } // delegated to alerts-api
    .groupBy({ it.prisonNumber }, { AlertResponse(it) })
}
