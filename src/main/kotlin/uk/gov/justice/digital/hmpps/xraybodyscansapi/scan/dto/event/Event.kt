package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.event

import java.time.LocalDateTime

/** An abstract event in this service that needs to be tracked */
sealed interface Event {
  val occurredAt: LocalDateTime
  val auditEvent: AuditEvent
  val domainEvent: DomainEvent
  val telemetryEvent: TelemetryEvent
}
