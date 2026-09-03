package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.sar

import java.time.LocalDate

data class SarScanResponse(
  val person: String,
  val date: LocalDate,
  val justification: String,
  val outcome: String,
  val find: String?,
  val establishment: String,
  val additionalDetails: String?,
)
