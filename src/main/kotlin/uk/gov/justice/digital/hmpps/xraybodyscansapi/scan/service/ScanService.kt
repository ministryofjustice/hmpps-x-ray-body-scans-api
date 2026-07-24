package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.service

import jakarta.validation.ValidationException
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.dto.response.ReferenceDataDomains
import uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.repository.ReferenceDataCodeEntity
import uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.repository.ReferenceDataCodeRepository
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request.CreateScanRequest
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request.ListScansRequest
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanSummaryResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.ScanEntity
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.ScanRepository
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.filterByPrisonerNumber
import java.time.LocalDate

@Service
class ScanService(
  private val codeRepository: ReferenceDataCodeRepository,
  private val scanRepository: ScanRepository,
  private val prisonApiClient: PrisonApiClient,
  @Value("\${scan.annual-limit}") private val scanAnnualLimit: Int,
) {
  @Transactional(readOnly = true)
  fun listScans(
    prisonerNumber: String,
    query: ListScansRequest? = null,
    pageable: Pageable = PageRequest.of(0, 20, Sort.by("scanDate").descending()),
  ): Page<ScanResponse> {
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
    val saved = scanRepository.save(
      ScanEntity(
        prisonerNumber = prisonerNumber,
        prisonId = request.prisonId,
        scanDate = request.scanDate,
        justification = findReferenceDataOrThrowValidationError(ReferenceDataDomains.JUSTIFICATION, request.justification),
        outcome = findReferenceDataOrThrowValidationError(ReferenceDataDomains.OUTCOME, request.outcome),
        typeOfFind = request.typeOfFind?.let { findReferenceDataOrThrowValidationError(ReferenceDataDomains.TYPE_OF_FIND, it) },
        caseNoteId = request.caseNoteId,
        createdBy = request.createdBy,
        lastModifiedBy = request.createdBy,
      ),
    )

    return saved.toDto()
  }

  @Transactional(readOnly = true)
  fun summariseScans(
    prisonerNumber: String,
    fromScanDate: LocalDate,
    toScanDate: LocalDate,
  ): ScanSummaryResponse = summariseScans(listOf(prisonerNumber), fromScanDate, toScanDate).first()

  @Transactional(readOnly = true)
  fun summariseScans(
    prisonerNumbers: List<String>,
    fromScanDate: LocalDate,
    toScanDate: LocalDate,
  ): List<ScanSummaryResponse> {
    val nomisCounts = getNomisScanCounts(prisonerNumbers, fromScanDate, toScanDate)
    val dpsScans = scanRepository
      .findByPrisonerNumberInAndScanDateBetween(prisonerNumbers, fromScanDate, toScanDate)
      .groupBy { it.prisonerNumber }

    return prisonerNumbers.map { prisonerNumber ->
      val nomisCount = nomisCounts[prisonerNumber] ?: 0
      val scans = dpsScans[prisonerNumber] ?: emptyList()
      val dpsCount = scans.size
      val outcomes = scans.groupingBy { it.outcomeCode }.eachCount()
      ScanSummaryResponse(
        prisonerNumber = prisonerNumber,
        nomisCount = nomisCount,
        dpsCount = dpsCount,
        totalCount = nomisCount + dpsCount,
        positiveCount = outcomes.getOrDefault("POSITIVE", 0),
        negativeCount = outcomes.getOrDefault("NEGATIVE", 0),
        inconclusiveCount = outcomes.getOrDefault("INCONCLUSIVE", 0),
        remainingScans = scanAnnualLimit - (nomisCount + dpsCount),
        fromScanDate = fromScanDate,
        toScanDate = toScanDate,
      )
    }
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
    id = id,
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
    createdAt = createdAt,
    createdBy = createdBy,
    lastModifiedAt = lastModifiedAt,
    lastModifiedBy = lastModifiedBy,
  )
}
