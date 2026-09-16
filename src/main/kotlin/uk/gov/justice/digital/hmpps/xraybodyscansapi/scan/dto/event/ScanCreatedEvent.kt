package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.event

import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanResponse

class ScanCreatedEvent(scanResponse: ScanResponse) : Event {
  companion object {
    const val NAME: String = "SCAN_CREATED"
  }

  override val occurredAt = scanResponse.createdAt

  override val auditEvent = AuditEvent(
    what = NAME,
    `when` = occurredAt,
    prisonerNumber = scanResponse.prisonerNumber,
    details = mapOf(
      "scanId" to scanResponse.id,
      "prisonId" to scanResponse.prisonId,
    ),
  )

  override val domainEvent = DomainEvent(
    "xraybodyscans.scan.created",
    "An x-ray body scan was recorded",
    occurredAt,
    additionalInformation = mapOf(
      "id" to scanResponse.id,
      "prisonerNumber" to scanResponse.prisonerNumber,
      "prisonId" to scanResponse.prisonId,
    ),
  )

  override val telemetryEvent = TelemetryEvent(
    NAME,
    mapOf(
      "id" to scanResponse.id,
    ),
  )
}
