package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Options for retrieving x-ray body scan summaries")
data class ScanSummaryRequest(
  @Schema(
    description = "Whether relevant alerts should be included (alerts field is null otherwise)",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
  )
  val includeAlerts: Boolean = false,
)
