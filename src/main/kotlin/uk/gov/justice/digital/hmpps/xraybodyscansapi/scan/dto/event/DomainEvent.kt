package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.event

import java.time.LocalDateTime

/** Represents an event published to the HMPPS domain events topic */
data class DomainEvent(
  val eventType: String,
  val eventDescription: String,
  val occurredAt: LocalDateTime,
  val additionalInformation: Map<String, String>,
)
