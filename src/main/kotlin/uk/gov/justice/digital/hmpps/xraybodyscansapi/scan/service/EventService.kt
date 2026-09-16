package uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.dto.event.Event
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.audit.HmppsAuditEvent
import uk.gov.justice.hmpps.sqs.eventTypeMessageAttributes
import uk.gov.justice.hmpps.sqs.publish
import java.time.ZoneId

@Service
class EventService(
  private val authenticationHolder: HmppsAuthenticationHolder,
  private val hmppsQueueService: HmppsQueueService,
  private val jsonMapper: JsonMapper,
  @Value($$"${spring.application.name}")
  private val serviceName: String,
  private val telemetryClient: TelemetryClient,
  private val zoneId: ZoneId,
) {
  private val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("domainevents")
      ?: throw RuntimeException("Topic with name domainevents not found")
  }

  private val auditQueue by lazy {
    hmppsQueueService.findByQueueId("audit")
      ?: throw RuntimeException("Queue with name audit not found")
  }
  private val auditSqsClient by lazy { auditQueue.sqsClient }
  private val auditQueueUrl by lazy { auditQueue.queueUrl }

  fun track(event: Event) {
    val domainEvent = event.domainEvent
    domainEventsTopic.publish(
      eventType = domainEvent.eventType,
      event = domainEvent.toJson(),
    )

    val auditEvent = event.auditEvent
    val hmppsAuditEvent = HmppsAuditEvent(
      what = auditEvent.what,
      `when` = auditEvent.`when`.atZone(zoneId).toInstant(),
      subjectId = auditEvent.prisonerNumber,
      subjectType = "PRISONER_ID",
      // correlationId = TODO(),
      who = authenticationHolder.username ?: authenticationHolder.principal,
      service = serviceName,
      details = auditEvent.details.toJson(),
    )
    /* TODO:
    `uk.gov.justice.hmpps.sqs.audit.HmppsAuditService.publishEvent` is a suspending function
    `uk.gov.justice.hmpps.sqs.sendMessage` does not work for audit events?
     */
    auditSqsClient.sendMessage(
      SendMessageRequest.builder()
        .queueUrl(auditQueueUrl)
        .messageBody(hmppsAuditEvent.toJson())
        .eventTypeMessageAttributes("hmpps-audit-event")
        .build(),
    )

    val telemetryEvent = event.telemetryEvent
    telemetryClient.trackEvent(telemetryEvent.name, telemetryEvent.properties, null)
  }

  private fun Any.toJson() = jsonMapper.writeValueAsString(this)
}
