package uk.gov.justice.digital.hmpps.xraybodyscansapi.event.handler

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.xraybodyscansapi.event.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.xraybodyscansapi.event.nomsNumber
import uk.gov.justice.digital.hmpps.xraybodyscansapi.event.removedNomsNumber
import uk.gov.justice.digital.hmpps.xraybodyscansapi.scan.service.ScanService

@Transactional
@Service
class PrisonerMergedHandler(private val scanService: ScanService) {
  fun handle(personMerged: HmppsDomainEvent) {
    scanService.mergeScans(
      from = personMerged.additionalInformation.removedNomsNumber,
      to = personMerged.additionalInformation.nomsNumber,
    )
  }
}
