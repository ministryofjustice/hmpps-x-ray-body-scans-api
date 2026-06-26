package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.PastOrPresent
import org.springframework.data.jpa.domain.Specification
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.RequestParam
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.filterFromScanDate
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.filterToScanDate
import java.time.LocalDate

@Schema(description = "Filters for listing scans")
data class ListScansRequest(
  @RequestParam(required = false)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  @Schema(
    description = "Filter scans on or after this date (YYYY-MM-DD)",
    example = "2026-01-01",
    type = "string",
    format = "date",
  )
  @PastOrPresent(message = "Date filter must be today or in the past")
  val fromScanDate: LocalDate? = null,
  @RequestParam(required = false)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  @Schema(
    description = "Filter scans on or before this date (YYYY-MM-DD)",
    example = "2026-06-26",
    type = "string",
    format = "date",
  )
  @PastOrPresent(message = "Date filter must be today or in the past")
  val toScanDate: LocalDate? = null,
) {
  fun toSpecification() = Specification.allOf(
    buildList {
      fromScanDate?.let {
        add(filterFromScanDate(it))
      }
      toScanDate?.let {
        add(filterToScanDate(it))
      }
    },
  )
}
