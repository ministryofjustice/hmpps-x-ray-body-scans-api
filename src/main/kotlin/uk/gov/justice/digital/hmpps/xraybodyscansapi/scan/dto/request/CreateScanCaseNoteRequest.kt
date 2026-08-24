package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(
  description = "Request model for creating a case note for an x-ray body scan.",
  accessMode = Schema.AccessMode.WRITE_ONLY,
)
data class CreateScanCaseNoteRequest(
  @NotBlank
  @Schema(
    description = "The body text of the case note",
    example = "Negative. No object found.",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val text: String,
)
