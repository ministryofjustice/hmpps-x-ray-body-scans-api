package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PastOrPresent
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response.ScanResult
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

  @field:NotNull
  @Schema(
    description = "The result of the scan",
    example = "NEGATIVE",
    requiredMode = Schema.RequiredMode.REQUIRED,
  )
  val result: ScanResult,
)
