package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request.CreateScanRequest
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request.ListScansRequest
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanCountResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.ScanEntity
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.ScanRepository
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.filterByPrisonerNumber
import java.time.LocalDate

@Service
class ScanService(
  private val scanRepository: ScanRepository,
  private val prisonApiClient: PrisonApiClient,
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
    // TODO: impose limits on page request, eg max size?
    return scanRepository.findAll(specification, pageable).map {
      it.toDto()
    }
  }

  @Transactional
  fun createScan(prisonerNumber: String, request: CreateScanRequest): ScanResponse {
    val saved = scanRepository.save(
      ScanEntity(
        prisonerNumber = prisonerNumber,
        scanDate = request.scanDate,
      ),
    )

    return saved.toDto()
  }

  @Transactional(readOnly = true)
  fun countScans(
    prisonerNumber: String,
    fromScanDate: LocalDate,
    toScanDate: LocalDate,
  ): ScanCountResponse = countScans(listOf(prisonerNumber), fromScanDate, toScanDate).first()

  @Transactional(readOnly = true)
  fun countScans(
    prisonerNumbers: List<String>,
    fromScanDate: LocalDate,
    toScanDate: LocalDate,
  ): List<ScanCountResponse> {
    val nomisCounts = getNomisScanCounts(prisonerNumbers, fromScanDate, toScanDate)
    val dpsCounts = scanRepository
      .findByPrisonerNumberInAndScanDateBetween(prisonerNumbers, fromScanDate, toScanDate)
      .groupingBy { it.prisonerNumber }
      .eachCount()

    return prisonerNumbers.map { prisonerNumber ->
      val nomisCount = nomisCounts[prisonerNumber] ?: 0
      val dpsCount = dpsCounts[prisonerNumber] ?: 0
      ScanCountResponse(
        prisonerNumber = prisonerNumber,
        nomisCount = nomisCount,
        dpsCount = dpsCount,
        totalCount = nomisCount + dpsCount,
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
}

private fun ScanEntity.toDto(): ScanResponse = ScanResponse(
  id = id,
  prisonerNumber = prisonerNumber,
  scanDate = scanDate,
)
