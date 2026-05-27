package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Response model for successful x-ray body scan creations")
data class CreateScanResponse(
  @Schema(
    description = "Unique identifier for the created scan",
    example = "123456789",
  )
  val id: Long,

  @Schema(
    description = "Prisoner number of the scanned prisoner",
    example = "A1234BC",
  )
  val prisonerNumber: String,

  @Schema(
    description = "Date the scan was performed (today or in the past)",
    example = "YYYY-MM-DD",
    type = "string",
    format = "date",
  )
  val scanDate: LocalDate,
)
