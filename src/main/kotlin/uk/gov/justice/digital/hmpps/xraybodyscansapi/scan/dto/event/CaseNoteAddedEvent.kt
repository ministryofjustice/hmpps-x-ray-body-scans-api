package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.event

import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanCaseNoteResponse
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanResponse

class CaseNoteAddedEvent(scanResponse: ScanResponse, caseNoteResponse: ScanCaseNoteResponse) : Event {
  companion object {
    const val NAME: String = "CASE_NOTE_ADDED"
  }

  override val occurredAt = caseNoteResponse.createdAt

  override val auditEvent = AuditEvent(
    what = NAME,
    `when` = occurredAt,
    prisonerNumber = scanResponse.prisonerNumber,
    details = mapOf(
      "scanId" to scanResponse.id,
      "prisonId" to scanResponse.prisonId,
      "caseNoteId" to caseNoteResponse.id,
    ),
  )

  override val domainEvent = DomainEvent(
    "xraybodyscans.casenote.added",
    "An x-ray body scan had a case note added",
    occurredAt,
    additionalInformation = mapOf(
      "id" to scanResponse.id,
      "prisonerNumber" to scanResponse.prisonerNumber,
      "prisonId" to scanResponse.prisonId,
      "caseNoteId" to caseNoteResponse.id,
    ),
  )

  override val telemetryEvent = TelemetryEvent(
    NAME,
    mapOf(
      "id" to scanResponse.id,
      "caseNoteId" to caseNoteResponse.id,
    ),
  )
}
