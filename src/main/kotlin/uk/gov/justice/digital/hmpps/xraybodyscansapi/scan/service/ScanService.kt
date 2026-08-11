package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.service

import jakarta.validation.ValidationException
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.alertsapi.AlertsApiClient
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.prisonapi.response.PersonalCareNeed
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.prisonapi.response.PersonalCareNeedComparator
import uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.dto.response.ReferenceDataDomains
import uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.repository.ReferenceDataCodeEntity
import uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.repository.ReferenceDataCodeRepository
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request.CreateScanRequest
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request.ListScansRequest
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.AlertResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.LegacyScanResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanSummaryResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.UnifiedScanResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.ScanEntity
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.ScanRepository
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.filterByPrisonerNumber
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.groupOutcomes
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.sortableFields
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.util.UnifiedScanResponsePaginator
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
    // limit sortable fields
    val allSortOrdersPermitted = pageable.sort.all {
      sortableFields.contains(it.property)
    }
    if (!allSortOrdersPermitted) {
      throw ValidationException("Sort order not supported (only $sortableFields are allowed)")
    }

    // if date range is greater than 1 year, limit page size
    val limitPageSize = query?.fromScanDate == null || (query.toScanDate ?: LocalDate.now(clock)).toEpochDay() - query.fromScanDate.toEpochDay() > 366
    if (limitPageSize && pageable.pageSize > 200) {
      throw ValidationException("Page size limit of 200 exceeded")
    }

    // ensure non-empty sort directive
    val sort = pageable.sort.takeIf { it.isSorted }
      ?: Sort.by("scanDate").descending()
    // …and add `id` at the end to break ties for scans of the same type
    val sortWithIdTiebreak = sort
      .and(Sort.by(sort.last().direction, "id"))
    // …and add reversed `source` at the end to break ties for mixed scans
    val sortWithSourceTiebreak = sort
      .and(Sort.by(sort.last().direction, "source").reverse())

    // target page
    val pageableWithIdTiebreak = PageRequest.of(pageable.pageNumber, pageable.pageSize, sortWithIdTiebreak)
    val pageableWithSourceTiebreak = PageRequest.of(pageable.pageNumber, pageable.pageSize, sortWithSourceTiebreak)

    var specification = filterByPrisonerNumber(prisonerNumber)
    query?.let {
      specification = specification.and(query.toSpecification())
    }
    val dpsScanSequence = getDpsScanSequence(specification, pageableWithIdTiebreak)
    val nomisScanSequence = getNomisScanSequence(prisonerNumber, pageableWithIdTiebreak)
    return dpsScanSequence.paginateWith(nomisScanSequence, pageableWithSourceTiebreak)
  }

  private fun getDpsScanSequence(
    specification: Specification<ScanEntity>,
    pageable: Pageable,
  ): UnifiedScanResponsePaginator<ScanResponse> {
    // get first page to know total number
    val firstPage = scanRepository.findAll(specification, pageable.withPage(0))
    val totalElements = firstPage.totalElements.toInt()

    // add first page to subsequent pages up to target page maximum (no more could be needed)
    val sequence = (
      firstPage.asSequence() + (1..pageable.pageNumber).asSequence()
        .flatMap { page ->
          scanRepository.findAll(specification, pageable.withPage(page))
        }
      )
      .map { it.toDto() }

    return UnifiedScanResponsePaginator(totalElements, sequence)
  }

  private fun getNomisScanSequence(
    prisonerNumber: String,
    pageable: Pageable,
  ): UnifiedScanResponsePaginator<LegacyScanResponse> {
    // get all care needs and sort
    val nomisScans = prisonApiClient.getScanCareNeeds(listOf(prisonerNumber)).firstOrNull()
      ?.personalCareNeeds?.toMutableList()
      ?: mutableListOf()
    nomisScans.sortWith(PersonalCareNeedComparator(pageable.sort))

    val sequence = nomisScans.asSequence()
      .map { it.toDto(prisonerNumber) }

    return UnifiedScanResponsePaginator(nomisScans.size, sequence)
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
  fun summariseScans(
    prisonerNumber: String,
    includeAlerts: IncludeAlerts = IncludeAlerts.No,
  ): ScanSummaryResponse = summariseScans(listOf(prisonerNumber), includeAlerts).first()

  @Transactional(readOnly = true)
  fun summariseScans(
    prisonerNumbers: List<String>,
    includeAlerts: IncludeAlerts = IncludeAlerts.No,
  ): List<ScanSummaryResponse> {
    val (fromScanDate, toScanDate) = calendarYear()
    val nomisCounts = getNomisScanCounts(prisonerNumbers, fromScanDate, toScanDate)
    val dpsCounts = scanRepository.scanSummaryRowsForPrisoners(prisonerNumbers, fromScanDate, toScanDate).groupOutcomes()

    val relevantAlerts = when (includeAlerts) {
      is IncludeAlerts.WithUsername -> getRelevantAlerts(prisonerNumbers, includeAlerts.username)
      is IncludeAlerts.No -> null
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

  private fun PersonalCareNeed.toDto(prisonerNumber: String): LegacyScanResponse = LegacyScanResponse(
    originalId = personalCareNeedId,
    prisonerNumber = prisonerNumber,
    scanDate = startDate,
    scanDetails = commentText,
  )

  private fun getRelevantAlerts(
    prisonerNumbers: List<String>,
    username: String,
  ): Map<String, List<AlertResponse>> = alertsApiClient.getAlerts(
    prisonerNumbers = prisonerNumbers,
    filterAlertCodes = relevantAlertCodes,
    username = username,
  )
    .toList()
    // .filter { relevantAlertCodes.contains(it.alertCode.code) } // delegated to alerts-api
    .groupBy({ it.prisonNumber }, { AlertResponse(it) })
}
