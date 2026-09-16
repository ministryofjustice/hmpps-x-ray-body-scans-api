package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.event

/** Represents an event tracked by Application Insights telemetry */
data class TelemetryEvent(
  val name: String,
  val properties: Map<String, String>,
)
