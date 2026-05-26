package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request.CreateScanRequest
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.CreateScanResponse
import java.util.UUID

@Service
class ScanService {

  fun createScan(prisonerNumber: String, request: CreateScanRequest): CreateScanResponse {
    // TODO: persist via repository
    return CreateScanResponse(
      id = UUID.randomUUID(),
      prisonerNumber = prisonerNumber,
      scanDate = request.scanDate,
    )
  }
}
