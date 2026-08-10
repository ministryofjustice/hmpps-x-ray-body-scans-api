package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.response

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.Source
import java.time.LocalDate

@Schema(
  description = "X-ray body scan recorded in an HMPPS system",
  accessMode = Schema.AccessMode.READ_ONLY,
)
sealed interface UnifiedScanResponse {
  val source: Source

  @get:JsonIgnore
  val isLegacy: Boolean
    get() = source == Source.NOMIS

  val id: String
  val prisonerNumber: String
  val scanDate: LocalDate?
}
