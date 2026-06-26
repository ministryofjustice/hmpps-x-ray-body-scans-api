package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request.CreateScanRequest
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanCountResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.ScanEntity
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.ScanRepository
import java.time.LocalDate

@Service
class ScanService(
  private val scanRepository: ScanRepository,
  private val prisonApiClient: PrisonApiClient,
) {

  @Transactional
  fun createScan(prisonerNumber: String, request: CreateScanRequest): ScanResponse {
    val saved = scanRepository.save(
      ScanEntity(
        prisonerNumber = prisonerNumber,
        scanDate = request.scanDate,
      ),
    )

    return ScanResponse(
      id = saved.id,
      prisonerNumber = saved.prisonerNumber,
      scanDate = saved.scanDate,
    )
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
