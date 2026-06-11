package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Response for scan counts of a prisoner over the requested time period")
data class ScanCountResponse(

  @Schema(
    description = "Unique prisoner identifier",
    example = "A1234BC",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val prisonerNumber: String,

  @Schema(
    description = "Number of scans recorded in NOMIS",
    example = "5",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val nomisCount: Int,

  @Schema(
    description = "Number of scans recorded in DPS",
    example = "2",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val dpsCount: Int,

  @Schema(
    description = "Total number of scans across NOMIS and DPS",
    example = "8",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val totalCount: Int,
)
