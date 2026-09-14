package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(
  description = "Scan deletion request",
  accessMode = Schema.AccessMode.WRITE_ONLY,
)
data class DeleteScanRequest(
  @NotBlank
  @Schema(
    description = "The reason a scan is being deleted",
    example = "Recorded in error.",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val reason: String,
)
