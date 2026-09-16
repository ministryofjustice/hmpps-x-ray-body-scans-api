package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.event

import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanResponse

class ScanDeletedEvent(scanResponse: ScanResponse) : Event {
  companion object {
    const val NAME: String = "SCAN_DELETED"
  }

  override val occurredAt = scanResponse.deletedAt ?: throw IllegalArgumentException("Scan has not been deleted")

  override val auditEvent = AuditEvent(
    what = NAME,
    `when` = occurredAt,
    prisonerNumber = scanResponse.prisonerNumber,
    details = mapOf(
      "scanId" to scanResponse.id,
    ),
  )

  override val domainEvent = DomainEvent(
    "xraybodyscans.scan.deleted",
    "An x-ray body scan was deleted",
    occurredAt,
    additionalInformation = mapOf(
      "id" to scanResponse.id,
    ),
  )

  override val telemetryEvent = TelemetryEvent(
    NAME,
    mapOf(
      "id" to scanResponse.id,
    ),
  )
}
