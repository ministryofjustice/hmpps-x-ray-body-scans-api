package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PastOrPresent
import java.time.LocalDate

@Schema(description = "Request model for creating an x-ray body scan.")
data class CreateScanRequest(
  @field:NotNull
  @field:PastOrPresent(message = "scanDate must be today or in the past")
  @Schema(
    description = "Date the scan was performed (today or in the past)",
    example = "YYYY-MM-DD",
    type = "string",
    format = "date",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val scanDate: LocalDate,

  @field:NotBlank
  @Schema(
    description = "The prison/establishment code where the scan took place",
    example = "MDI",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val prisonId: String,

  @field:NotBlank
  @Schema(
    description = "Why the scan was carried out",
    example = "REASONABLE_SUSPICION",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val justification: String,
  @field:NotBlank
  @Schema(
    description = "What the outcome of the scan was",
    example = "NEGATIVE",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val outcome: String,
  @Schema(
    description = "What type of item was detected, if any",
    example = "INORGANIC",
    requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    nullable = true,
  )
  val typeOfFind: String? = null,

  @Schema(
    description = "Who created the scan record",
    example = "abc12a",
    type = "string",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val createdBy: String,
)
