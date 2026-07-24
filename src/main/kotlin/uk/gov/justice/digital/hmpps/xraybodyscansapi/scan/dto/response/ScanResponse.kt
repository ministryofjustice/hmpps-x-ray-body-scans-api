package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "Response model for an x-ray body scan, may be nested in another response model")
data class ScanResponse(
  @Schema(
    description = "Unique identifier for the scan as a UUIDv7",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val id: UUID,

  @Schema(
    description = "Prisoner number of the scanned prisoner",
    example = "A1234BC",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val prisonerNumber: String,
  @Schema(
    description = "The prison/establishment code where the scan took place",
    example = "MDI",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val prisonId: String,

  @Schema(
    description = "Date the scan was performed (today or in the past)",
    example = "YYYY-MM-DD",
    type = "string",
    format = "date",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val scanDate: LocalDate,

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
)
