package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Case note associated with an x-ray body scan.")
data class ScanCaseNoteResponse(
  @Schema(description = "The case note sub-type description, used as the title", example = "X-Ray Body Scan")
  val title: String,

  @Schema(description = "Username of this case note's author", example = "John Smith")
  val createdBy: String,

  @Schema(description = "Date and time the event occurred", example = "2026-08-01T00:00:00")
  val occurredAt: LocalDateTime,

  @Schema(description = "The body text of the case note", example = "X-ray body scan carried out with negative result.")
  val text: String,
)
