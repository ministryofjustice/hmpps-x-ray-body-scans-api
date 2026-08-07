package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.Source
import java.time.LocalDate

@Schema(
  description = "X-ray body scan recorded in an HMPPS system",
  accessMode = Schema.AccessMode.READ_ONLY,
)
sealed interface UnifiedScanResponse {
  @get:Schema(
    description = "The system where this scan was recorded",
    type = "string",
    allowableValues = ["DPS", "NOMIS"],
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val source: Source

  @get:Schema(
    description = "Unique identifier",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val id: String

  @get:Schema(
    description = "Prisoner number of the scanned prisoner",
    example = "A1234BC",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val prisonerNumber: String

  @get:Schema(
    description = "Date the scan was performed",
    example = "YYYY-MM-DD",
    type = "string",
    format = "date",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val scanDate: LocalDate
}
