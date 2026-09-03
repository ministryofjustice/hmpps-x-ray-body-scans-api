package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.casenotes.response.CaseNoteResponse
import java.time.LocalDateTime

@Schema(
  description = "Case note associated with an x-ray body scan.",
  accessMode = Schema.AccessMode.READ_ONLY,
)
data class ScanCaseNoteResponse(
  @Schema(description = "The unique case note id", format = "uuid", example = "341c845e-fadc-4ec8-9330-81c83968c1a8")
  val id: String,

  @Schema(description = "The case note sub-type description, used as the title", example = "X-Ray Body Scan")
  val title: String,

  @Schema(description = "Username of this case note's author", example = "John Smith")
  val createdBy: String,

  @Schema(description = "Date and time the case note was created", example = "2026-08-01T00:00:00")
  val createdAt: LocalDateTime,

  @Schema(description = "Date and time the event occurred", example = "2026-08-01T00:00:00")
  val occurredAt: LocalDateTime,

  @Schema(description = "The body text of the case note", example = "X-ray body scan carried out with negative result.")
  val text: String,
) {
  constructor(caseNote: CaseNoteResponse) : this(
    id = caseNote.caseNoteId,
    title = caseNote.subTypeDescription,
    createdBy = caseNote.authorName,
    createdAt = caseNote.creationDateTime,
    occurredAt = caseNote.occurrenceDateTime,
    text = caseNote.text,
  )
}
