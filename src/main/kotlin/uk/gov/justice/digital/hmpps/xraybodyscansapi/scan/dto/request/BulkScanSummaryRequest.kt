package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern

@Schema(
  description = "Request to retrieve scan summaries for multiple prisoners",
  accessMode = Schema.AccessMode.WRITE_ONLY,
)
data class BulkScanSummaryRequest(
  @Schema(
    description = "List of prisoner numbers to summarise",
    example = "[\"A1234BC\", \"B5678DE\"]",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  @NotEmpty(message = "prisonerNumbers must not be empty")
  val prisonerNumbers: List<
    @Pattern(
      regexp = "^[A-Z]\\d{4}[A-Z]{2}$",
      message = "Each prisonerNumber must be in the right form, e.g. A1234BC.",
    )
    String,
    >,

  @Schema(
    description = "Whether relevant alerts should be included (alerts field is null otherwise)",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
  )
  val includeAlerts: Boolean = false,
)
