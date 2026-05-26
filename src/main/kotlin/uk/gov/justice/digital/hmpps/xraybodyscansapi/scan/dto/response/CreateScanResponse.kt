package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.util.UUID

@Schema(description = "Response model for successful x-ray body scan creations")
data class CreateScanResponse(
  @Schema(
    description = "Unique identifier for the created scan",
    example = "8f3c1e7a-2b4d-4e5f-9a1b-7c8d9e0f1a2b",
  )
  val id: UUID,

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
