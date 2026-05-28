package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request.CreateScanRequest
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.ScanEntity
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.ScanRepository

@Service
class ScanService(
  private val scanRepository: ScanRepository,
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
      id = saved.id!!,
      prisonerNumber = saved.prisonerNumber,
      scanDate = saved.scanDate,
    )
  }
}
