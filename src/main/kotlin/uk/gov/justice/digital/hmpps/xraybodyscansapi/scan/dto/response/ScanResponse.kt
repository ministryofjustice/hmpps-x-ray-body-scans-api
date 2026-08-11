package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.Source
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Schema(
  description = "Response model for an x-ray body scan, may be nested in another response model",
  accessMode = Schema.AccessMode.READ_ONLY,
)
data class ScanResponse(
  @JsonIgnore
  val originalId: UUID,

  @Schema(
    description = "Prisoner number of the scanned prisoner",
    example = "A1234BC",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  override val prisonerNumber: String,
  @Schema(
    description = "The prison/establishment code where the scan took place",
    example = "MDI",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val prisonId: String,

  @Schema(
    description = "Date the scan was performed",
    example = "YYYY-MM-DD",
    type = "string",
    format = "date",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  override val scanDate: LocalDate,

  @Schema(
    description = "Why the scan was carried out (as a code)",
    example = "REASONABLE_SUSPICION",
    type = "string",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val justification: String,
  @Schema(
    description = "Why the scan was carried out (as a human-readable description)",
    example = "Reasonable suspicion",
    type = "string",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val justificationDescription: String,

  @Schema(
    description = "What the outcome of the scan was (as a code)",
    example = "NEGATIVE",
    type = "string",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val outcome: String,
  @Schema(
    description = "What the outcome of the scan was (as a human-readable description)",
    example = "No item detected",
    type = "string",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val outcomeDescription: String,

  @Schema(
    description = "What type of item was detected, if any (as a code)",
    example = "INORGANIC",
    type = "string",
    nullable = true,
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val typeOfFind: String? = null,
  @Schema(
    description = "What type of item was detected, if any (as a human-readable description)",
    example = "Inorganic",
    type = "string",
    nullable = true,
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val typeOfFindDescription: String? = null,

  @Schema(
    description = "Reference to associated case note, if any",
    type = "string",
    format = "uuid",
    nullable = true,
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val caseNoteId: UUID? = null,

  @Schema(
    description = "Former prisoner number this record belonged to, if any",
    nullable = true,
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val mergedFromPrisonerNumber: String? = null,
  @Schema(
    description = "When this record was merged from a different prisoner number, if ever",
    type = "string",
    format = "date-time",
    nullable = true,
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val mergedAt: LocalDateTime? = null,

  @Schema(
    description = "When the scan record was created",
    type = "string",
    format = "date-time",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val createdAt: LocalDateTime,
  @Schema(
    description = "Who created the scan record",
    example = "abc12a",
    type = "string",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val createdBy: String,
  @Schema(
    description = "When the scan record was updated",
    type = "string",
    format = "date-time",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val lastModifiedAt: LocalDateTime,
  @Schema(
    description = "Who updated the scan record",
    example = "abc12a",
    type = "string",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val lastModifiedBy: String,
) : UnifiedScanResponse {
  @Schema(
    description = "Unique DPS identifier for the scan as a UUIDv7",
    type = "string",
    format = "uuid",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  override val id: String = originalId.toString()

  @Schema(
    description = "This scan was recorded in DPS",
    type = "string",
    defaultValue = "DPS",
    allowableValues = ["DPS"],
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  override val source: Source = Source.DPS
}
