package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.Source
import java.time.LocalDate

@Schema(
  description = "Legacy x-ray body scan recorded as a personal care need in NOMIS",
  accessMode = Schema.AccessMode.READ_ONLY,
)
data class LegacyScanResponse(
  @JsonIgnore
  val originalId: Long,

  @Schema(
    description = "Prisoner number of the scanned prisoner",
    example = "A1234BC",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  override val prisonerNumber: String,

  @Schema(
    description = "Date the scan was performed",
    example = "YYYY-MM-DD",
    type = "string",
    format = "date",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  override val scanDate: LocalDate,

  @Schema(
    description = "Comment entered by staff",
    type = "string",
    nullable = true,
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val scanDetails: String? = null,
) : UnifiedScanResponse {
  @Schema(
    description = "Personal care need ID from NOMIS",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  override val id: String = originalId.toString()

  @Schema(
    description = "This scan was recorded in NOMIS",
    type = "string",
    allowableValues = ["NOMIS"],
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  override val source: Source = Source.NOMIS
}
