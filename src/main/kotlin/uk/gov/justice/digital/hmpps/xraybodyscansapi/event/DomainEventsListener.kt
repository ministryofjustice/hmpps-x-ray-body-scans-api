package uk.gov.justice.digital.hmpps.xraybodyscansapi.event

import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.xraybodyscansapi.event.handler.PrisonerMergedHandler

@Service
@ConditionalOnProperty(name = ["hmpps.sqs.enabled"], havingValue = "true")
class DomainEventsListener(
  private val jsonMapper: JsonMapper,
  private val prisonerMergedHandler: PrisonerMergedHandler,
) {
  init {
    log.info("Created SQS Domain Events Listener")
  }

  @SqsListener("hmppsdomaineventsqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun receive(notification: Notification) {
    val event = jsonMapper.readValue<HmppsDomainEvent>(notification.message)
    when (notification.eventType) {
      PRISONER_MERGED -> prisonerMergedHandler.handle(event)
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(DomainEventsListener::class.java)
    const val PRISONER_MERGED = "prison-offender-events.prisoner.merged"
  }
}
