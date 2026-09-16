package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.event

import java.time.LocalDateTime

/** Represents messages sent to the HMPPS Audit Service queue */
data class AuditEvent(
  val what: String,
  val `when`: LocalDateTime,
  val prisonerNumber: String,
  val details: Map<String, String>,
)
