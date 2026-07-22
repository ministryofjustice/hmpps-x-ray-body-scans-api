package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Summary of x-ray body scans for a prisoner over a given time period")
data class ScanSummaryResponse(

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

  @Schema(
    description = "Number of scans with a positive result",
    example = "1",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val positiveCount: Int,

  @Schema(
    description = "Number of scans with a negative result",
    example = "6",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val negativeCount: Int,

  @Schema(
    description = "Number of scans with an inconclusive result",
    example = "1",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val inconclusiveCount: Int,

  @Schema(
    description = "Number of scans remaining before the annual limit of 116 is reached. Negative values indicate the limit has been surpassed",
    example = "109",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val remainingScans: Int,

  @Schema(
    description = "The earliest date of the period over which these scans were counted (inclusive)",
    example = "2026-01-01",
    type = "string",
    format = "date",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val fromScanDate: LocalDate,

  @Schema(
    description = "The latest date of the period over which these scans were counted (inclusive)",
    example = "2026-06-26",
    type = "string",
    format = "date",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val toScanDate: LocalDate,
)
