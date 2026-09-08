package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.xraybodyscansapi.client.casenotes.response.CaseNoteAmendment
import java.time.LocalDateTime

@Schema(
  description = "An amendment added to a case note associated with an x-ray body scan",
  accessMode = Schema.AccessMode.READ_ONLY,
)
data class ScanCaseNoteAmendmentResponse(
  @Schema(description = "The body text added in an amendment to the case note", example = "No need for adjudication.")
  val text: String,

  @Schema(description = "Name of this case note amendment’s author", example = "John Smith")
  val createdBy: String,

  @Schema(description = "Date and time the case note amendment was created", example = "2026-08-01T13:00:00")
  val createdAt: LocalDateTime,
) {
  constructor(amendment: CaseNoteAmendment) : this(
    text = amendment.additionalNoteText,
    createdBy = amendment.authorName,
    createdAt = amendment.creationDateTime,
  )
}
