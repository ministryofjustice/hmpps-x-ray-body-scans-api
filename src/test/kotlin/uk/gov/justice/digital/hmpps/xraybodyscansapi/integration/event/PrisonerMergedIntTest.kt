package uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.event

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.withPollDelay
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.xraybodyscansapi.event.DomainEventsListener.Companion.PRISONER_MERGED
import uk.gov.justice.digital.hmpps.xraybodyscansapi.event.HmppsAdditionalInformation
import uk.gov.justice.digital.hmpps.xraybodyscansapi.event.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.xraybodyscansapi.event.PersonReference.Companion.withPrisonNumber
import uk.gov.justice.digital.hmpps.xraybodyscansapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.xraybodyscansapi.referencedata.repository.ReferenceDataCodeRepository
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.repository.ScanRepository
import java.time.Duration.ofSeconds
import java.time.ZonedDateTime

class PrisonerMergedIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var codeRepository: ReferenceDataCodeRepository

  @Autowired
  lateinit var scanRepository: ScanRepository

  @Test
  @Sql("classpath:/events/test-data/reset.sql")
  @Sql("classpath:/events/test-data/scans.sql")
  fun `merge functions if prisoner has no scans`() {
    sendDomainEvent(
      personMergedEvent(
        PRISONER_NUMBER_NOT_FOUND,
        PRISONER_NUMBER_NOT_FOUND,
      ),
    )

    await withPollDelay ofSeconds(1) untilCallTo { hmppsDomainEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }
    assertThat(scanRepository.findAllByPrisonerNumber(PRISONER_NUMBER_NOT_FOUND)).isEmpty()
  }

  @Test
  @Sql("classpath:/events/test-data/reset.sql")
  @Sql("classpath:/events/test-data/scans.sql")
  fun `merge correctly functions when scan exists for child`() {
    sendDomainEvent(
      personMergedEvent(
        PARENT_PRISONER_NUMBER,
        CHILD_PRISONER_NUMBER,
      ),
    )

    await withPollDelay ofSeconds(1) untilCallTo { hmppsDomainEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }

    assertThat(scanRepository.findAllByPrisonerNumber(CHILD_PRISONER_NUMBER)).isEmpty()
    assertThat(scanRepository.findAllByPrisonerNumber(PARENT_PRISONER_NUMBER)).hasSize(3)
  }

  private fun personMergedEvent(
    prisonNumber: String,
    removedPrisonNumber: String,
    occurredAt: ZonedDateTime = ZonedDateTime.now(),
    eventType: String = PRISONER_MERGED,
    detailUrl: String? = null,
    description: String = "A prisoner was merged",
  ) = HmppsDomainEvent(
    eventType,
    1,
    detailUrl,
    occurredAt,
    description,
    HmppsAdditionalInformation(mutableMapOf("nomsNumber" to prisonNumber, "removedNomsNumber" to removedPrisonNumber)),
    withPrisonNumber(prisonNumber),
  )

  companion object {
    private const val PRISONER_NUMBER_NOT_FOUND = "Z9999ZZ"
    private const val PARENT_PRISONER_NUMBER = "B1234BB"
    private const val CHILD_PRISONER_NUMBER = "A1234BC"
  }
}
